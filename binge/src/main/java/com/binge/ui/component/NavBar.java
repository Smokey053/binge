package com.binge.ui.component;

import com.binge.ui.Icons;
import com.binge.ui.control.Icon;
import com.binge.ui.control.IconButton;
import com.binge.ui.control.NavLink;
import com.binge.ui.control.Pressable;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** The top bar: brand, page tabs, search and settings. */
public final class NavBar extends HBox {

    /** The top-level pages reachable from the tabs. */
    public enum Page { HOME, BROWSE, MY_LIST }

    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");
    private static final double SEARCH_IDLE = 180;
    private static final double SEARCH_FOCUSED = 270;

    private final Map<Page, NavLink> links = new EnumMap<>(Page.class);
    private final TextField search = new TextField();
    private final HBox searchBox = new HBox(8);
    private final IconButton clear = new IconButton(Icons.CLOSE, 14, "Clear search");
    private final IconButton settings = new IconButton(Icons.SETTINGS, 20, "Settings");
    private final Timeline searchResize = new Timeline();
    private Consumer<Page> onNavigate = page -> { };
    private Runnable onSettings = () -> { };
    private boolean compact;

    public NavBar() {
        getStyleClass().add("nav-bar");
        setAlignment(Pos.CENTER);
        setSpacing(14);

        ImageView logo = new ImageView(new Image(Objects.requireNonNull(
                NavBar.class.getResourceAsStream("/com/binge/branding/binge-logo.png"))));
        logo.setPreserveRatio(true);
        logo.setFitHeight(34);
        logo.setSmooth(true);
        logo.getStyleClass().add("brand-logo");
        Pressable brandButton = new Pressable() { };
        brandButton.getStyleClass().add("brand");
        brandButton.getChildren().add(logo);
        brandButton.setOnAction(() -> navigate(Page.HOME));

        HBox tabs = new HBox(8);
        tabs.setAlignment(Pos.CENTER_LEFT);
        addLink(tabs, Page.HOME, "Home");
        addLink(tabs, Page.BROWSE, "Browse");
        addLink(tabs, Page.MY_LIST, "My List");

        buildSearch();

        setPadding(new Insets(0, 28, 0, 28));
        getChildren().addAll(brandButton, tabs, searchBox, settings);
        settings.setOnAction(() -> onSettings.run());
        links.get(Page.HOME).setSelected(true);
    }

    private void addLink(HBox tabs, Page page, String text) {
        NavLink link = new NavLink(text);
        link.setOnAction(() -> navigate(page));
        links.put(page, link);
        tabs.getChildren().add(link);
    }

    private void navigate(Page page) {
        select(page);
        onNavigate.accept(page);
    }

    private void buildSearch() {
        searchBox.getStyleClass().add("search-box");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        search.setPromptText("Search movies, genres…");
        search.getStyleClass().add("search-field");
        HBox.setHgrow(search, Priority.ALWAYS);
        clear.setVisible(false);
        clear.setManaged(false);
        clear.setOnAction(this::clearSearch);
        searchBox.getChildren().addAll(new Icon(Icons.SEARCH, 18), search, clear);
        searchBox.setPrefWidth(SEARCH_IDLE);
        searchBox.setMinWidth(SEARCH_IDLE);
        searchBox.setMaxWidth(SEARCH_IDLE);
        searchBox.setOnMouseClicked(e -> search.requestFocus());

        search.focusedProperty().addListener((o, was, focused) -> {
            searchBox.pseudoClassStateChanged(FOCUSED, focused);
            resizeSearch(focused || !search.getText().isEmpty());
        });
        search.textProperty().addListener((o, was, text) -> {
            clear.setVisible(!text.isEmpty());
            clear.setManaged(!text.isEmpty());
            resizeSearch(search.isFocused() || !text.isEmpty());
        });
    }

    private void resizeSearch(boolean expanded) {
        double target = compact ? (expanded ? 180 : 128) : (expanded ? SEARCH_FOCUSED : SEARCH_IDLE);
        searchResize.stop();
        searchResize.getKeyFrames().setAll(new KeyFrame(Duration.millis(220),
                new KeyValue(searchBox.prefWidthProperty(), target, Interpolator.EASE_BOTH),
                new KeyValue(searchBox.minWidthProperty(), target, Interpolator.EASE_BOTH),
                new KeyValue(searchBox.maxWidthProperty(), target, Interpolator.EASE_BOTH)));
        searchResize.play();
    }

    public void select(Page page) {
        links.forEach((p, link) -> link.setSelected(p == page));
    }

    /** Removes every tab highlight, e.g. while search results are on screen. */
    public void clearSelection() {
        links.values().forEach(link -> link.setSelected(false));
    }

    public void setOnNavigate(Consumer<Page> handler) {
        this.onNavigate = handler;
    }

    public void setOnSettings(Runnable handler) {
        this.onSettings = handler;
    }

    public StringProperty searchProperty() {
        return search.textProperty();
    }

    public void clearSearch() {
        search.clear();
    }

    public void focusSearch() {
        search.requestFocus();
    }

    public void setMyListCount(int count) {
        links.get(Page.MY_LIST).setBadge(count);
    }

    /** Compact mode hides tab captions so the bar fits narrow windows. */
    public void setCompact(boolean compact) {
        if (this.compact == compact) {
            return;
        }
        this.compact = compact;
        links.values().forEach(link -> link.setLabelVisible(!compact));
        resizeSearch(search.isFocused() || !search.getText().isEmpty());
    }

}
