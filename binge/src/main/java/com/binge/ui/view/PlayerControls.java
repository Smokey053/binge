package com.binge.ui.view;

import com.binge.player.YouTubePlayer;
import com.binge.ui.Icons;
import com.binge.ui.control.Icon;
import com.binge.ui.control.IconButton;
import com.binge.ui.control.Pressable;
import com.binge.ui.control.SeekBar;
import com.binge.util.Fmt;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Teh custom control bar drawn over the video: timeline, play/pause, skip, buttons, 
volume, elapsed/totaltime, and the full-screen toggle. Every control is a plain node
{button} wired to the YouTubePlayer properties*/
final class PlayerControls extends VBox {

    private final YouTubePlayer player;
    private final SeekBar timeline = new SeekBar();
    private final SeekBar volume = new SeekBar();
    private final IconButton playPause = new IconButton(Icons.PAUSE, 26, "Pause (Space)");
    private final IconButton mute = new IconButton(Icons.VOLUME_UP, 22, "Mute (M)");
    private final IconButton fullscreen = new IconButton(Icons.FULLSCREEN, 22, "Full screen (F)");
    private final Label time = new Label("0:00 / 0:00");

    PlayerControls(YouTubePlayer player, BooleanProperty fullscreenState, Runnable onFullscreen, Runnable onToggle) {
        this.player = player;
        getStyleClass().add("player-controls");
        setSpacing(2);
        setPadding(new Insets(34, 18, 12, 18));

        bindTimeline();
        bindVolume();
        bindPlayPause();

        Pressable back10 = skipButton(-10, false);
        Pressable forward10 = skipButton(10, true);

        playPause.setOnAction(onToggle);
        playPause.setClickSound(null);
        mute.setOnAction(() -> {
            if (player.volumeProperty().get() <= 0.001) {
                player.volumeProperty().set(0.6);
            }
            player.mutedProperty().set(!player.mutedProperty().get());
        });
        fullscreen.setOnAction(onFullscreen);
        fullscreenState.addListener((o, was, now) -> {
            fullscreen.setIcon(now ? Icons.FULLSCREEN_EXIT : Icons.FULLSCREEN);
            fullscreen.setTooltipText(now ? "Exit full screen (F)" : "Full screen (F)");
        });
        time.getStyleClass().add("player-time");

        volume.setPrefWidth(84);
        volume.setMaxWidth(84);
        volume.setMinWidth(84);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(4, playPause, back10, forward10, mute, volume, spacer(10), time, spacer, fullscreen);
        row.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(timeline, row);
    }

    private static Region spacer(double width) {
        Region r = new Region();
        r.setMinWidth(width);
        return r;
    }

    private void bindTimeline() {
        timeline.setOnCommit(fraction -> {
            if (player.durationProperty().get() > 0) {
                player.seekTo(fraction * player.durationProperty().get());
            }
        });
        Runnable sync = () -> {
            double duration = player.durationProperty().get();
            if (!timeline.scrubbingProperty().get() && duration > 0) {
                timeline.valueProperty().set(player.currentTimeProperty().get() / duration);
            }
            timeline.bufferedProperty().set(player.loadedProperty().get());
        };
        player.currentTimeProperty().addListener(o -> sync.run());
        player.durationProperty().addListener(o -> sync.run());
        player.loadedProperty().addListener(o -> sync.run());

        time.textProperty().bind(Bindings.createStringBinding(() -> {
            double duration = player.durationProperty().get();
            double shown = timeline.scrubbingProperty().get()
                    ? timeline.valueProperty().get() * duration
                    : player.currentTimeProperty().get();
            return Fmt.time(shown) + " / " + Fmt.time(duration);
        }, player.currentTimeProperty(), player.durationProperty(), timeline.valueProperty(), timeline.scrubbingProperty()));
    }

    private void bindVolume() {
        volume.valueProperty().set(player.volumeProperty().get());
        volume.setOnScrub(v -> {
            player.volumeProperty().set(v);
            player.mutedProperty().set(v <= 0.001);
        });
        player.volumeProperty().addListener((o, was, now) -> {
            if (!volume.scrubbingProperty().get()) {
                volume.valueProperty().set(now.doubleValue());
            }
        });
        Runnable refresh = () -> {
            boolean off = player.mutedProperty().get() || player.volumeProperty().get() <= 0.001;
            mute.setIcon(off ? Icons.VOLUME_OFF : player.volumeProperty().get() < 0.5 ? Icons.VOLUME_DOWN : Icons.VOLUME_UP);
            mute.setTooltipText(off ? "Unmute (M)" : "Mute (M)");
            volume.valueProperty().set(off ? 0 : player.volumeProperty().get());
        };
        player.mutedProperty().addListener(o -> refresh.run());
        player.volumeProperty().addListener(o -> refresh.run());
    }

    private void bindPlayPause() {
        player.stateProperty().addListener((o, was, state) -> {
            boolean playing = state == YouTubePlayer.State.PLAYING || state == YouTubePlayer.State.BUFFERING;
            playPause.setIcon(state == YouTubePlayer.State.ENDED ? Icons.REPLAY : playing ? Icons.PAUSE : Icons.PLAY);
            playPause.setTooltipText(playing ? "Pause (Space)" : "Play (Space)");
        });
    }

    /** A circular skip control. The arrow shows the direction, with no number drawn on it. */
    private Pressable skipButton(double seconds, boolean forward) {
        Pressable button = new Pressable() {
            {
                getStyleClass().add("icon-button");
                setMinSize(44, 44);
                setMaxSize(44, 44);
            }
        };
        Icon arrow = new Icon(Icons.REPLAY, 26);
        arrow.setScaleX(forward ? -1 : 1);
        arrow.setMouseTransparent(true);
        button.getChildren().add(new StackPane(arrow));
        button.setAccessibleText(forward ? "Skip forward" : "Skip back");
        button.setOnAction(() -> player.seekBy(seconds));
        return button;
    }
}
