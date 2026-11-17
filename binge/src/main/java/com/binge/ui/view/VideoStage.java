package com.binge.ui.view;

import com.binge.audio.Sound;
import com.binge.model.Movie;
import com.binge.player.YouTubePlayer;
import com.binge.ui.Icons;
import com.binge.ui.Images;
import com.binge.ui.UiSound;
import com.binge.ui.control.CoverImageView;
import com.binge.ui.control.Icon;
import com.binge.ui.control.LoadingReel;
import com.binge.ui.control.PillButton;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 * The 16:9 video area: the YouTube WebView plus everything layered on top of it - the custom
 * loading animation, buffering indicator, play/pause flash, end card, error panel and the
 * auto-hiding control bar. Clicking the picture toggles playback, double-clicking toggles full screen.
 */
final class VideoStage extends StackPane {

    private static final double ASPECT = 16.0 / 9.0;
    private static final String SHOWN_KEY = "shown";

    private final YouTubePlayer player;
    private final Runnable onFullscreen;
    private final Runnable onOpenYouTube;

    private final CoverImageView loadingBackdrop = new CoverImageView();
    private final LoadingReel loadingReel = new LoadingReel();
    private final StackPane loadingLayer = new StackPane();
    private final LoadingReel bufferingReel = new LoadingReel();
    private final StackPane bufferingLayer = new StackPane();
    private final StackPane flash = new StackPane();
    private final Icon flashIcon = new Icon(Icons.PLAY, 34);
    private final VBox endCard = new VBox(14);
    private final VBox errorPanel = new VBox(12);
    private final Label errorMessage = new Label();
    private final PillButton retryButton = new PillButton("Try again", Icons.REFRESH, PillButton.Kind.PRIMARY);
    private final PlayerControls controls;
    private final PauseTransition idleTimer = new PauseTransition(Duration.seconds(2.6));
    private final Map<Node, FadeTransition> fades = new HashMap<>();
    private boolean controlsShown = true;

    VideoStage(YouTubePlayer player, BooleanProperty fullscreen, Runnable onFullscreen, Runnable onOpenYouTube) {
        this.player = player;
        this.onFullscreen = onFullscreen;
        this.onOpenYouTube = onOpenYouTube;
        getStyleClass().add("video-stage");

        VideoHolder holder = new VideoHolder(player.view());
        Region clickLayer = new Region();
        clickLayer.setOnMouseClicked(this::onSurfaceClicked);

        buildLoadingLayer();
        buildBufferingLayer();
        buildFlash();
        buildEndCard();
        buildErrorPanel();

        controls = new PlayerControls(player, fullscreen, onFullscreen, this::togglePlayback);
        controls.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(controls, Pos.BOTTOM_CENTER);

        getChildren().addAll(holder, clickLayer, loadingLayer, bufferingLayer, flash, endCard, errorPanel, controls);

        idleTimer.setOnFinished(e -> {
            if (player.getState() == YouTubePlayer.State.PLAYING && !controls.isHover()) {
                setControlsShown(false);
            }
        });
        addEventFilter(MouseEvent.MOUSE_MOVED, e -> wake());
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> wake());
        hoverProperty().addListener((o, was, hovered) -> {
            if (!hovered && player.getState() == YouTubePlayer.State.PLAYING) {
                idleTimer.playFromStart();
            }
        });
        player.stateProperty().addListener((o, was, now) -> onStateChanged(now));
        player.errorCodeProperty().addListener((o, was, now) -> updateErrorText());
        onStateChanged(YouTubePlayer.State.IDLE);
    }

    /** Prepares the loading screen for a movie; call right before {@link YouTubePlayer#load}. */
    void showLoading(Movie movie) {
        loadingBackdrop.setImage(null);
        loadingBackdrop.setEffect(new GaussianBlur(4));
        Images.backdrop(movie, loadingBackdrop::setImage);
        loadingReel.setTitle(movie.title());
        loadingReel.setMessage("Loading trailer");
        setControlsShown(true);
    }

    void togglePlayback() {
        YouTubePlayer.State state = player.getState();
        if (state == YouTubePlayer.State.LOADING || state == YouTubePlayer.State.IDLE || state == YouTubePlayer.State.ERROR) {
            return;
        }
        boolean pausing = state == YouTubePlayer.State.PLAYING || state == YouTubePlayer.State.BUFFERING;
        player.togglePlayPause();
        UiSound.play(pausing ? Sound.PAUSE : Sound.PLAY);
        flash(pausing ? Icons.PAUSE : Icons.PLAY);
    }

    /** Re-shows the controls (mouse movement, key presses) and restarts the idle countdown. */
    void wake() {
        setControlsShown(true);
        if (player.getState() == YouTubePlayer.State.PLAYING) {
            idleTimer.playFromStart();
        }
    }

    private void onSurfaceClicked(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        if (e.getClickCount() == 2) {
            onFullscreen.run();
        } else {
            togglePlayback();
        }
    }

    private void onStateChanged(YouTubePlayer.State state) {
        setLayerVisible(loadingLayer, state == YouTubePlayer.State.LOADING);
        setLayerVisible(bufferingLayer, state == YouTubePlayer.State.BUFFERING);
        setLayerVisible(endCard, state == YouTubePlayer.State.ENDED);
        setLayerVisible(errorPanel, state == YouTubePlayer.State.ERROR);
        if (state == YouTubePlayer.State.ERROR) {
            UiSound.play(Sound.ERROR);
            updateErrorText();
        }
        if (state == YouTubePlayer.State.PLAYING) {
            idleTimer.playFromStart();
        } else {
            setControlsShown(true);
        }
    }

    private void setControlsShown(boolean shown) {
        if (controlsShown == shown) {
            return;
        }
        controlsShown = shown;
        FadeTransition fade = new FadeTransition(Duration.millis(shown ? 120 : 380), controls);
        fade.setToValue(shown ? 1 : 0);
        fade.play();
        controls.setMouseTransparent(!shown);
        setCursor(shown ? Cursor.DEFAULT : Cursor.NONE);
    }

    /** Fades an overlay in or out; the latest request always wins. */
    private void setLayerVisible(Node layer, boolean visible) {
        Object current = layer.getProperties().get(SHOWN_KEY);
        if (current != null && (Boolean) current == visible) {
            return;
        }
        layer.getProperties().put(SHOWN_KEY, visible);
        FadeTransition previous = fades.remove(layer);
        if (previous != null) {
            previous.stop();
        }
        FadeTransition fade = new FadeTransition(Duration.millis(visible ? 160 : 420), layer);
        if (visible) {
            layer.setVisible(true);
            fade.setToValue(1);
        } else {
            fade.setToValue(0);
            fade.setOnFinished(e -> layer.setVisible(false));
        }
        fades.put(layer, fade);
        fade.play();
    }
    private void flash(String iconPath) {
        flashIcon.setPath(iconPath);
        flash.setVisible(true);
        flash.setOpacity(0.9);
        flash.setScaleX(0.7);
        flash.setScaleY(0.7);
        ScaleTransition grow = new ScaleTransition(Duration.millis(520), flash);
        grow.setToX(1.35);
        grow.setToY(1.35);
        grow.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition fade = new FadeTransition(Duration.millis(520), flash);
        fade.setToValue(0);
        ParallelTransition both = new ParallelTransition(grow, fade);
        both.setOnFinished(e -> flash.setVisible(false));
        both.play();
    }

    private void buildLoadingLayer() {
        loadingBackdrop.setFocus(0.5, 0.3);
        Region dim = new Region();
        dim.getStyleClass().add("loading-dim");
        loadingLayer.getStyleClass().add("loading-layer");
        loadingLayer.getChildren().addAll(loadingBackdrop, dim, loadingReel);
        loadingLayer.setMouseTransparent(true);
        loadingLayer.setVisible(false);
        loadingLayer.setOpacity(0);
        loadingLayer.visibleProperty().addListener((o, was, now) -> {
            if (now) {
                loadingReel.play();
            } else {
                loadingReel.stop();
            }
        });
    }

    private void buildBufferingLayer() {
        bufferingReel.setTitle("");
        bufferingReel.setMessage("Buffering");
        bufferingReel.setScaleX(0.55);
        bufferingReel.setScaleY(0.55);
        bufferingLayer.getChildren().add(bufferingReel);
        bufferingLayer.setMouseTransparent(true);
        bufferingLayer.setVisible(false);
        bufferingLayer.setOpacity(0);
        bufferingLayer.visibleProperty().addListener((o, was, now) -> {
            if (now) {
                bufferingReel.play();
            } else {
                bufferingReel.stop();
            }
        });
    }

    private void buildFlash() {
        flash.getStyleClass().add("flash-badge");
        flash.getChildren().add(flashIcon);
        flash.setMinSize(84, 84);
        flash.setMaxSize(84, 84);
        flash.setVisible(false);
        flash.setMouseTransparent(true);
    }

    private void buildEndCard() {
        Label heading = new Label("Trailer finished");
        heading.getStyleClass().add("end-title");
        Label hint = new Label("Watch it again or pick something from More like this below.");
        hint.getStyleClass().add("end-hint");
        PillButton replay = new PillButton("Watch again", Icons.REPLAY, PillButton.Kind.PRIMARY);
        replay.setOnAction(() -> {
            player.seekTo(0);
            player.play();
        });
        endCard.getStyleClass().add("overlay-card");
        endCard.setAlignment(Pos.CENTER);
        endCard.setFillWidth(false);
        endCard.getChildren().addAll(heading, hint, replay);
        endCard.setVisible(false);
        endCard.setOpacity(0);
    }

    private void buildErrorPanel() {
        Icon icon = new Icon(Icons.INFO, 40);
        icon.getStyleClass().add("error-icon");
        Label heading = new Label("This trailer can't be played here");
        heading.getStyleClass().add("end-title");
        errorMessage.getStyleClass().add("end-hint");
        errorMessage.setWrapText(true);
        errorMessage.setMaxWidth(440);
        errorMessage.setAlignment(Pos.CENTER);
        retryButton.setOnAction(player::retry);
        PillButton youtube = new PillButton("Watch on YouTube", Icons.OPEN_IN_NEW, PillButton.Kind.GLASS);
        youtube.setOnAction(onOpenYouTube);
        HBox actions = new HBox(12, retryButton, youtube);
        actions.setAlignment(Pos.CENTER);
        errorPanel.getStyleClass().add("overlay-card");
        errorPanel.setAlignment(Pos.CENTER);
        errorPanel.setFillWidth(false);
        errorPanel.getChildren().addAll(icon, heading, errorMessage, actions);
        errorPanel.setVisible(false);
        errorPanel.setOpacity(0);
    }

    private void updateErrorText() {
        int code = player.errorCodeProperty().get();
        errorMessage.setText(YouTubePlayer.describeError(code));
        boolean blocked = YouTubePlayer.isEmbeddingBlocked(code);
        retryButton.setVisible(!blocked);
        retryButton.setManaged(!blocked);
    }

    /** Keeps the WebView at 16:9 and centred; any spare room becomes black bars. */
    private static final class VideoHolder extends Region {
        private final WebView view;

        VideoHolder(WebView view) {
            this.view = view;
            getStyleClass().add("video-holder");
            getChildren().add(view);
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            double fitW = Math.min(w, h * ASPECT);
            double fitH = fitW / ASPECT;
            view.resizeRelocate((w - fitW) / 2, (h - fitH) / 2, fitW, fitH);
        }
    }
}
