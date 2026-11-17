package com.binge.ui.view;

import com.binge.audio.Sound;
import com.binge.model.Movie;
import com.binge.player.YouTubePlayer;
import com.binge.ui.AppContext;
import com.binge.ui.Icons;
import com.binge.ui.UiSound;
import com.binge.ui.component.MovieRow;
import com.binge.ui.control.IconButton;
import com.binge.ui.control.PageScroll;
import com.binge.ui.control.PillButton;
import com.binge.util.Fmt;
import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * The full-window trailer player: a top bar, the {@link VideoStage}, movie details and a
 * "More like this" shelf. It owns the {@link YouTubePlayer}, handles keyboard shortcuts and
 * full-screen mode, and lays itself out for any window size.
 */
public final class PlayerView extends StackPane {

    private static final double SEEK_STEP = 5;
    private static final double BIG_SEEK_STEP = 10;

    private final AppContext context;
    private final YouTubePlayer player = new YouTubePlayer();
    private final BooleanProperty fullscreen = new SimpleBooleanProperty(false);
    private final VideoStage stage;
    private final PageScroll scroll;
    private final HBox topBar = new HBox(10);
    private final VBox info = new VBox(12);
    private final VBox page = new VBox();
    private final VBox relatedHolder = new VBox();
    private final Label topTitle = new Label();
    private final Label title = new Label();
    private final Label meta = new Label();
    private final FlowPane genreTags = new FlowPane(8, 8);
    private final Label synopsis = new Label();
    private final PillButton listButton = new PillButton("My List", Icons.ADD, PillButton.Kind.GHOST);
    private Movie current;
    private boolean stageListenerAttached;
    private FadeTransition fade;

    public PlayerView(AppContext context) {
        this.context = context;
        getStyleClass().add("player-view");
        setVisible(false);
        setFocusTraversable(true);

        stage = new VideoStage(player, fullscreen, this::toggleFullscreen, this::openOnYouTube);
        buildTopBar();
        buildInfo();

        page.getChildren().addAll(topBar, stage, info, relatedHolder);
        scroll = new PageScroll(page);
        scroll.getStyleClass().add("player-scroll");
        getChildren().add(scroll);

        scroll.viewportBoundsProperty().addListener((o, was, bounds) -> sizeStage(bounds.getWidth(), bounds.getHeight()));
        fullscreen.addListener((o, was, full) -> applyFullscreenLayout(full));
        context.library().myList().addListener((ListChangeListener<String>) c -> refreshListButton());

        addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
    }

    /** Warms up the embedded player so the first trailer starts quickly. */
    public void prepare() {
        player.prepare();
    }

    public boolean isShowing() {
        return isVisible() && getOpacity() > 0;
    }

    /** Opens the player for a movie (or switches to another one if it is already open). */
    public void show(Movie movie) {
        current = movie;
        context.library().addRecent(movie);
        attachStageListener();
        fillInfo(movie);
        scroll.scrollToTop();
        stage.showLoading(movie);
        player.load(movie.trailerId());
        UiSound.play(Sound.OPEN);

        if (!isVisible() || getOpacity() < 1) {
            setVisible(true);
            fadeTo(1, 220, null);
        }
        requestFocus();
    }

    public void close() {
        if (!isShowing()) {
            return;
        }
        UiSound.play(Sound.CLOSE);
        player.stop();
        setFullscreen(false);
        fadeTo(0, 180, () -> setVisible(false));
    }

    /** Releases the embedded browser and its local server. */
    public void dispose() {
        player.dispose();
    }

    private void fadeTo(double opacity, double millis, Runnable then) {
        if (fade != null) {
            fade.stop();
        }
        fade = new FadeTransition(Duration.millis(millis), this);
        fade.setToValue(opacity);
        fade.setOnFinished(e -> {
            if (then != null) {
                then.run();
            }
        });
        fade.play();
    }

    private void buildTopBar() {
        IconButton back = new IconButton(Icons.CHEVRON_LEFT, 24, "Back (Esc)");
        back.setOnAction(this::close);
        back.setClickSound(null);
        Label label = new Label("Now playing");
        label.getStyleClass().add("topbar-label");
        topTitle.getStyleClass().add("topbar-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label hint = new Label("Space  play/pause     ←  →  seek     M  mute     F  full screen     Esc  close");
        hint.setVisible(false);
        hint.setManaged(false);
        topBar.getStyleClass().add("player-topbar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 24, 10, 16));
        topBar.getChildren().addAll(back, label, topTitle, spacer);
    }

    private void buildInfo() {
        title.getStyleClass().add("player-title");
        title.setWrapText(true);
        meta.getStyleClass().add("player-meta");
        synopsis.getStyleClass().add("player-synopsis");
        synopsis.setWrapText(true);
        synopsis.setMaxWidth(820);
        listButton.setOnAction(() -> {
            if (current != null) {
                context.library().toggleMyList(current);
            }
        });
        PillButton youtube = new PillButton("Watch on YouTube", Icons.OPEN_IN_NEW, PillButton.Kind.GHOST);
        youtube.setOnAction(this::openOnYouTube);
        HBox actions = new HBox(12, listButton, youtube);
        actions.setPadding(new Insets(6, 0, 0, 0));
        info.getStyleClass().add("player-info");
        info.setPadding(new Insets(24, 44, 8, 44));
        info.getChildren().addAll(title, meta, genreTags, synopsis, actions);
        relatedHolder.setPadding(new Insets(10, 24, 40, 24));
    }

    private void fillInfo(Movie movie) {
        topTitle.setText(movie.title());
        title.setText(movie.title());
        meta.setText(movie.year() + "  •  Directed by " + movie.director());
        synopsis.setText(movie.synopsis());
        genreTags.getChildren().setAll(movie.genres().stream().map(g -> {
            Label tag = new Label(g);
            tag.getStyleClass().add("genre-tag");
            return tag;
        }).toList());
        refreshListButton();

        relatedHolder.getChildren().clear();
        var related = context.catalog().related(movie);
        if (!related.isEmpty()) {
            MovieRow row = new MovieRow("More like this", related, context, null);
            relatedHolder.getChildren().add(row);
        }
    }

    private void refreshListButton() {
        if (current == null) {
            return;
        }
        boolean in = context.library().isInMyList(current);
        listButton.setText(in ? "In My List" : "Add to My List");
        listButton.setIcon(in ? Icons.CHECK : Icons.ADD);
    }

    private void openOnYouTube() {
        if (current != null) {
            context.hostServices().showDocument(current.watchUrl());
        }
    }

    private void sizeStage(double width, double height) {
        double h = fullscreen.get()
                ? height
                : Fmt.clamp(width * 9 / 16, 220, Math.max(260, height * 0.74));
        stage.setMinHeight(h);
        stage.setPrefHeight(h);
        stage.setMaxHeight(h);
    }

    private void applyFullscreenLayout(boolean full) {
        for (var node : new javafx.scene.Node[] {topBar, info, relatedHolder}) {
            node.setVisible(!full);
            node.setManaged(!full);
        }
        scroll.setVbarPolicy(full ? javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
                : javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.scrollToTop();
        sizeStage(scroll.getViewportBounds().getWidth(), scroll.getViewportBounds().getHeight());
    }

    private void attachStageListener() {
        if (stageListenerAttached || getScene() == null || !(getScene().getWindow() instanceof Stage window)) {
            return;
        }
        stageListenerAttached = true;
        window.fullScreenProperty().addListener((o, was, now) -> fullscreen.set(now));
    }

    public void toggleFullscreen() {
        setFullscreen(!fullscreen.get());
    }

    private void setFullscreen(boolean full) {
        Window window = getScene() == null ? null : getScene().getWindow();
        if (window instanceof Stage s && s.isFullScreen() != full) {
            s.setFullScreen(full);
        }
        fullscreen.set(full);
    }

    private void onKey(KeyEvent e) {
        if (!isShowing()) {
            return;
        }
        KeyCode code = e.getCode();
        boolean handled = true;
        switch (code) {
            case SPACE, K -> stage.togglePlayback();
            case LEFT -> player.seekBy(-SEEK_STEP);
            case RIGHT -> player.seekBy(SEEK_STEP);
            case J -> player.seekBy(-BIG_SEEK_STEP);
            case L -> player.seekBy(BIG_SEEK_STEP);
            case UP -> adjustVolume(0.1);
            case DOWN -> adjustVolume(-0.1);
            case M -> player.mutedProperty().set(!player.mutedProperty().get());
            case F -> toggleFullscreen();
            case ESCAPE -> {
                if (fullscreen.get()) {
                    setFullscreen(false);
                } else {
                    close();
                }
            }
            default -> handled = false;
        }
        if (handled) {
            stage.wake();
            e.consume();
        }
    }

    private void adjustVolume(double delta) {
        player.volumeProperty().set(Fmt.clamp(player.volumeProperty().get() + delta, 0, 1));
        player.mutedProperty().set(player.volumeProperty().get() <= 0.001);
    }
}
