package com.binge.ui.view;

import com.binge.audio.Sound;
import com.binge.audio.SoundFx;
import com.binge.data.UserLibrary;
import com.binge.model.Catalog;
import com.binge.model.Movie;
import com.binge.ui.AppContext;
import com.binge.ui.Icons;
import com.binge.ui.Navigator;
import com.binge.ui.UiSound;
import com.binge.ui.component.NavBar;
import com.binge.ui.component.NavBar.Page;
import com.binge.ui.component.Toast;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.HostServices;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.function.Consumer;

/** The main app frame: top navigation, switchable pages, and overlays (trailer player, toast messages,
startup splash). It implements {Link navigator}, so components ask the shell to open up trailers or 
change pages instead of talking to each other*/
public final class AppShell extends StackPane implements Navigator {

    private static final double COMPACT_WIDTH = 1020;

    private final Catalog catalog;
    private final UserLibrary library;
    private final AppContext context;
    private final NavBar navBar = new NavBar();
    private final StackPane pages = new StackPane();
    private final HomeView home;
    private final BrowseView browse;
    private final MyListView myList;
    private final SearchView searchView;
    private final SettingsView settingsView;
    private final PlayerView player;
    private final Toast toast = new Toast();
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(260));
    private FadeTransition navFade;
    private TranslateTransition navSlide;
    private boolean navHidden;
    private Node currentPage;
    private Page lastPage = Page.HOME;

    public AppShell(Catalog catalog, UserLibrary library, SoundFx sound, HostServices hostServices) {
        this.catalog = catalog;
        this.library = library;
        this.context = new AppContext(catalog, library, sound, this, hostServices);
        getStyleClass().add("app-shell");

        home = new HomeView(context);
        browse = new BrowseView(context);
        myList = new MyListView(context);
        searchView = new SearchView(context);
        settingsView = new SettingsView(library);
        player = new PlayerView(context);

        pages.getChildren().addAll(home, browse, myList, searchView, settingsView);
        pages.getChildren().forEach(p -> p.setVisible(false));

        StackPane.setAlignment(navBar, Pos.TOP_CENTER);
        navBar.setMaxWidth(Double.MAX_VALUE);
        getChildren().addAll(pages, navBar, player, toast);

        wireNavigation();
        wireSettings();
        wireResponsiveness();
        wireFloatingNavigation();
        wireShortcuts();

        showPage(Page.HOME);
    }

    /** Starts background preparation that should not delay the first frame. */
    public void warmUp() {
        player.prepare();
    }

    public void dispose() {
        player.dispose();
    }

    public SplashView createSplash(Consumer<SplashView> onFinished) {
        return new SplashView(onFinished);
    }

    // ---- Navigator ---------------------------------------------------------------------------

    @Override
    public void play(Movie movie) {
        player.show(movie);
    }

    @Override
    public void browse(String categoryId) {
        player.close();
        navBar.clearSearch();
        browse.showCategory(categoryId);
        navBar.select(Page.BROWSE);
        show(browse);
        lastPage = Page.BROWSE;
    }

    // ---- Pages -------------------------------------------------------------------------------

    private void showPage(Page page) {
        lastPage = page;
        navBar.select(page);
        switch (page) {
            case HOME -> show(home);
            case BROWSE -> {
                browse.showCategory(null);
                show(browse);
            }
            case MY_LIST -> show(myList);
        }
    }

    private void show(Node page) {
        if (page == currentPage) {
            return;
        }
        if (currentPage == home) {
            home.onHidden();
        }
        currentPage = page;
        showNav();
        for (Node p : pages.getChildren()) {
            p.setVisible(p == page);
        }
        page.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(260), page);
        fade.setToValue(1);
        fade.play();
        if (page == home) {
            home.onShown();
        }
    }

    private void wireNavigation() {
        navBar.setOnNavigate(page -> {
            navBar.clearSearch();
            showPage(page);
        });
        navBar.setOnSettings(() -> {
            navBar.clearSearch();
            navBar.clearSelection();
            show(settingsView);
        });
        navBar.searchProperty().addListener((o, was, text) -> searchDelay.playFromStart());
        searchDelay.setOnFinished(e -> {
            String query = navBar.searchProperty().get().strip();
            if (query.isEmpty()) {
                showPage(lastPage);
            } else {
                navBar.clearSelection();
                searchView.search(query);
                show(searchView);
            }
        });
    }

    // ---- Settings, toasts and badges ---------------------------------------------------------

    private void wireSettings() {
        library.soundEnabledProperty().addListener((o, was, on) -> {
            if (on) {
                UiSound.play(Sound.CLICK);
            }
        });

        library.lightThemeProperty().addListener((o, was, light) -> applyTheme(light));
        applyTheme(library.lightThemeProperty().get());

        navBar.setMyListCount(library.myList().size());
        library.myList().addListener((ListChangeListener<String>) change -> {
            navBar.setMyListCount(library.myList().size());
            while (change.next()) {
                for (String id : change.getAddedSubList()) {
                    catalog.find(id).ifPresent(m -> toast.show("Added “" + m.title() + "” to My List"));
                }
                for (String id : change.getRemoved()) {
                    if (!library.myList().contains(id)) {
                        catalog.find(id).ifPresent(m -> toast.show("Removed “" + m.title() + "” from My List"));
                    }
                }
            }
        });
    }

    private void applyTheme(boolean light) {
        if (light) {
            if (!getStyleClass().contains("light")) {
                getStyleClass().add("light");
            }
        } else {
            getStyleClass().remove("light");
        }
    }

    // ---- Responsiveness and shortcuts --------------------------------------------------------

    private void wireResponsiveness() {
        widthProperty().addListener((o, was, width) -> applyWidth(width.doubleValue()));
        applyWidth(COMPACT_WIDTH + 1);
    }

    /** Hides the navigation  bar while reading down the page and restores it when scrolling back up. */
    private void wireFloatingNavigation() {
        addEventFilter(ScrollEvent.SCROLL, e -> {
            if (player.isShowing() || Math.abs(e.getDeltaX()) >= Math.abs(e.getDeltaY()) || e.getDeltaY() == 0) {
                return;
            }
            if (e.getDeltaY() < 0) {
                hideNav();
            } else {
                showNav();
            }
        });
    }

    private void hideNav() {
        if (navHidden) {
            return;
        }
        navHidden = true;
        navBar.setMouseTransparent(true);
        animateNav(0, -14);
    }

    private void showNav() {
        if (!navHidden) {
            return;
        }
        navHidden = false;
        navBar.setMouseTransparent(false);
        animateNav(1, 0);
    }

    private void animateNav(double opacity, double y) {
        if (navFade != null) {
            navFade.stop();
        }
        if (navSlide != null) {
            navSlide.stop();
        }
        navFade = new FadeTransition(Duration.millis(180), navBar);
        navFade.setToValue(opacity);
        navSlide = new TranslateTransition(Duration.millis(180), navBar);
        navSlide.setToY(y);
        navFade.play();
        navSlide.play();
    }

    private void applyWidth(double width) {
        boolean compact = width < COMPACT_WIDTH;
        navBar.setCompact(compact);
        if (compact && !getStyleClass().contains("compact")) {
            getStyleClass().add("compact");
        } else if (!compact) {
            getStyleClass().remove("compact");
        }
    }

    private void wireShortcuts() {
        // Focus rings are only drawn while the keyboard is being used, not after mouse clicks.
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB && !getStyleClass().contains("keyboard-nav")) {
                getStyleClass().add("keyboard-nav");
            }
        });
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> getStyleClass().remove("keyboard-nav"));
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            boolean typing = getScene() != null && getScene().getFocusOwner() instanceof TextField;
            if (!player.isShowing() && !typing && (e.getCode() == KeyCode.SLASH
                    || (e.getCode() == KeyCode.K && e.isControlDown()))) {
                navBar.focusSearch();
                e.consume();
            } else if (typing && e.getCode() == KeyCode.ESCAPE) {
                navBar.clearSearch();
                requestFocus();
                e.consume();
            }
        });
        setFocusTraversable(true);
    }
}
