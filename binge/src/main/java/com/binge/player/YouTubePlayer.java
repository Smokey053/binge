package com.binge.player;

import com.binge.util.Fmt;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.util.regex.Pattern;

/**
 * Plays YouTube trailers inside the application. A {@link WebView} hosts the official YouTube
 * IFrame player (served from {@link PlayerServer}); this class hides all of that behind plain
 * JavaFX properties and methods so the UI can draw its own custom controls. Video data is always
 * streamed from YouTube, nothing is stored locally.
 *
 * <p>All methods must be called on the JavaFX application thread.
 */
public final class YouTubePlayer {

    /** Playback state as seen by the UI. */
    public enum State { IDLE, LOADING, PLAYING, PAUSED, BUFFERING, ENDED, ERROR }

    /** Pseudo error code used when YouTube could not be reached or never started playing. */
    public static final int ERROR_TIMEOUT = -100;

    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final long LOAD_TIMEOUT_NANOS = 25_000_000_000L;
    private static final long KICK_AFTER_NANOS = 4_000_000_000L;

    private final PlayerServer server = new PlayerServer();
    private final WebView view = new WebView();
    private final WebEngine engine = view.getEngine();
    private final Timeline poller = new Timeline(new KeyFrame(Duration.millis(250), e -> poll()));

    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.IDLE);
    private final ReadOnlyDoubleWrapper currentTime = new ReadOnlyDoubleWrapper();
    private final ReadOnlyDoubleWrapper duration = new ReadOnlyDoubleWrapper();
    private final ReadOnlyDoubleWrapper loaded = new ReadOnlyDoubleWrapper();
    private final ReadOnlyBooleanWrapper started = new ReadOnlyBooleanWrapper();
    private final ReadOnlyIntegerWrapper errorCode = new ReadOnlyIntegerWrapper();
    private final DoubleProperty volume = new SimpleDoubleProperty(0.8);
    private final BooleanProperty muted = new SimpleBooleanProperty(false);

    private boolean pageReady;
    private boolean pageRequested;
    private String requestedId;
    private long requestedAt;
    private boolean kicked;

    public YouTubePlayer() {
        view.setContextMenuEnabled(false);
        view.setMouseTransparent(true);
        view.setFocusTraversable(false);
        poller.setCycleCount(Animation.INDEFINITE);
        engine.getLoadWorker().stateProperty().addListener((o, was, now) -> {
            if (now == Worker.State.SUCCEEDED) {
                pageReady = true;
                pushAudio();
                if (requestedId != null) {
                    script("load('" + requestedId + "')");
                }
            } else if (now == Worker.State.FAILED && requestedId != null) {
                fail(ERROR_TIMEOUT);
            }
        });
        volume.addListener((o, a, b) -> pushAudio());
        muted.addListener((o, a, b) -> pushAudio());
    }

    /** The node that renders the video. It ignores the mouse; the UI draws its own controls on top. */
    public WebView view() {
        return view;
    }

    /** Starts loading the player page in advance so the first trailer opens quickly. */
    public void prepare() {
        if (!pageRequested) {
            pageRequested = true;
            engine.load(server.start());
        }
    }

    /** Loads a trailer by YouTube id and starts playing it as soon as it is ready. */
    public void load(String videoId) {
        if (videoId == null || !VIDEO_ID.matcher(videoId).matches()) {
            throw new IllegalArgumentException("Not a YouTube video id: " + videoId);
        }
        prepare();
        requestedId = videoId;
        requestedAt = System.nanoTime();
        kicked = false;
        started.set(false);
        errorCode.set(0);
        currentTime.set(0);
        duration.set(0);
        loaded.set(0);
        state.set(State.LOADING);
        if (pageReady) {
            script("load('" + videoId + "')");
        }
        poller.play();
    }

    /** Re-attempts the last requested trailer, e.g. after a network error. */
    public void retry() {
        if (requestedId != null) {
            load(requestedId);
        }
    }

    public void play() {
        script("play()");
    }

    public void pause() {
        script("pause()");
    }

    public void togglePlayPause() {
        switch (state.get()) {
            case PLAYING, BUFFERING -> pause();
            case ENDED -> {
                seekTo(0);
                play();
            }
            case PAUSED -> play();
            default -> { }
        }
    }

    public void seekTo(double seconds) {
        double target = Fmt.clamp(seconds, 0, duration.get() > 0 ? duration.get() : Double.MAX_VALUE);
        currentTime.set(target);
        script("seek(" + target + ")");
        if (state.get() == State.ENDED) {
            state.set(State.PAUSED);
        }
    }

    public void seekBy(double deltaSeconds) {
        seekTo(currentTime.get() + deltaSeconds);
    }

    /** Stops playback and frees the stream; the player page stays loaded for the next trailer. */
    public void stop() {
        poller.stop();
        requestedId = null;
        started.set(false);
        state.set(State.IDLE);
        script("stop()");
    }

    /** Releases the local server; call when the application exits. */
    public void dispose() {
        poller.stop();
        engine.load(null);
        server.close();
    }

    private void pushAudio() {
        if (pageReady) {
            script("setAudio(" + Math.round(volume.get() * 100) + "," + (muted.get() ? 1 : 0) + ")");
        }
    }

    private Object script(String js) {
        if (!pageReady) {
            return null;
        }
        try {
            return engine.executeScript(js);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void poll() {
        PlayerSnapshot s = PlayerSnapshot.parse(script("snap()"));
        long waited = System.nanoTime() - requestedAt;
        if (s == null) {
            if (!started.get() && waited > LOAD_TIMEOUT_NANOS) {
                fail(ERROR_TIMEOUT);
            }
            return;
        }
        if (s.error() != 0) {
            fail(s.error());
            return;
        }
        switch (s.playerState()) {
            case 1 -> {
                started.set(true);
                state.set(State.PLAYING);
            }
            case 2 -> {
                started.set(true);
                state.set(State.PAUSED);
            }
            case 3 -> state.set(started.get() ? State.BUFFERING : State.LOADING);
            case 0 -> {
                started.set(true);
                state.set(State.ENDED);
            }
            default -> {
                if (!started.get()) {
                    state.set(State.LOADING);
                    // Autoplay is sometimes held back; nudge the player once if it sits idle.
                    if (s.apiReady() && !kicked && waited > KICK_AFTER_NANOS && (s.playerState() == -1 || s.playerState() == 5)) {
                        kicked = true;
                        play();
                    }
                }
            }
        }
        if (!started.get() && waited > LOAD_TIMEOUT_NANOS) {
            fail(ERROR_TIMEOUT);
            return;
        }
        if (s.duration() > 0) {
            duration.set(s.duration());
        }
        if (state.get() != State.LOADING) {
            currentTime.set(s.time());
        }
        loaded.set(s.loaded());
    }

    private void fail(int code) {
        poller.stop();
        errorCode.set(code);
        state.set(State.ERROR);
    }

    /** True when the video owner disabled embedding, so the trailer can only be watched on youtube.com. */
    public static boolean isEmbeddingBlocked(int code) {
        return code == 101 || code == 150 || code == 152 || code == 153;
    }

    /** A short user-facing explanation for a YouTube error code. */
    public static String describeError(int code) {
        return switch (code) {
            case ERROR_TIMEOUT -> "We couldn't reach YouTube. Check your internet connection and try again.";
            case 2, 5 -> "YouTube could not play this trailer in the embedded player.";
            case 100 -> "This trailer has been removed or made private.";
            case 101, 150, 152, 153 -> "The owner of this trailer doesn't allow it to be played inside other apps.";
            default -> "Something went wrong while loading the trailer (code " + code + ").";
        };
    }

    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }

    public State getState() {
        return state.get();
    }

    public ReadOnlyDoubleProperty currentTimeProperty() {
        return currentTime.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty durationProperty() {
        return duration.getReadOnlyProperty();
    }

    /** Fraction (0-1) of the video that has been buffered. */
    public ReadOnlyDoubleProperty loadedProperty() {
        return loaded.getReadOnlyProperty();
    }

    /** True once the current trailer has begun playing (used to hide the loading animation). */
    public ReadOnlyBooleanProperty startedProperty() {
        return started.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty errorCodeProperty() {
        return errorCode.getReadOnlyProperty();
    }

    /** Volume from 0 to 1. */
    public DoubleProperty volumeProperty() {
        return volume;
    }

    public BooleanProperty mutedProperty() {
        return muted;
    }

    public ObjectProperty<State> stateObjectProperty() {
        return state;
    }
}
