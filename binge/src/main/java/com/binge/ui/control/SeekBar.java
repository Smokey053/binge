package com.binge.ui.control;

import com.binge.util.Fmt;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.function.DoubleConsumer;

/**
 * A custom slider (no {@code Slider} control) used for the video timeline and the volume.
 * The value runs from 0 to 1. {@code onScrub} fires continuously while dragging and
 * {@code onCommit} once when the pointer is released.
 */
public final class SeekBar extends Region {

    private static final double THUMB = 14;

    private final Region track = new Region();
    private final Region buffer = new Region();
    private final Region fill = new Region();
    private final Region thumb = new Region();
    private final DoubleProperty value = new SimpleDoubleProperty(0);
    private final DoubleProperty buffered = new SimpleDoubleProperty(0);
    private final BooleanProperty scrubbing = new SimpleBooleanProperty(false);
    private final Timeline hoverGrow = new Timeline();
    private boolean dragging;
    private DoubleConsumer onScrub = v -> { };
    private DoubleConsumer onCommit = v -> { };

    public SeekBar() {
        getStyleClass().add("seek-bar");
        track.getStyleClass().add("seek-track");
        buffer.getStyleClass().add("seek-buffer");
        fill.getStyleClass().add("seek-fill");
        thumb.getStyleClass().add("seek-thumb");
        for (Region part : new Region[] {track, buffer, fill, thumb}) {
            part.setMouseTransparent(true);
        }
        getChildren().addAll(track, buffer, fill, thumb);
        setCursor(Cursor.HAND);
        setMinHeight(20);
        setPrefHeight(20);
        setMaxHeight(20);
        thumb.setScaleX(0);
        thumb.setScaleY(0);

        value.addListener(o -> requestLayout());
        buffered.addListener(o -> requestLayout());
        hoverProperty().addListener((o, was, hovered) -> animateThumb(hovered || scrubbing.get()));
        scrubbing.addListener((o, was, now) -> animateThumb(now || isHover()));

        addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            dragging = true;
            scrubbing.set(true);
            updateFrom(e);
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (dragging) {
                updateFrom(e);
            }
        });
        addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            // Ignore releases that belong to a press somewhere else.
            if (!dragging) {
                return;
            }
            dragging = false;
            updateFrom(e);
            scrubbing.set(false);
            onCommit.accept(value.get());
        });
    }

    private void updateFrom(MouseEvent e) {
        double usable = Math.max(1, getWidth() - THUMB);
        value.set(Fmt.clamp((e.getX() - THUMB / 2) / usable, 0, 1));
        onScrub.accept(value.get());
        e.consume();
    }

    private void animateThumb(boolean grown) {
        hoverGrow.stop();
        double target = grown ? 1 : 0;
        hoverGrow.getKeyFrames().setAll(new KeyFrame(Duration.millis(130),
                new KeyValue(thumb.scaleXProperty(), target),
                new KeyValue(thumb.scaleYProperty(), target)));
        hoverGrow.play();
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        double trackHeight = isHover() || scrubbing.get() ? 6 : 4;
        double y = (h - trackHeight) / 2;
        double usable = Math.max(0, w - THUMB);
        double x0 = THUMB / 2;
        track.resizeRelocate(x0, y, usable, trackHeight);
        buffer.resizeRelocate(x0, y, usable * Fmt.clamp(buffered.get(), 0, 1), trackHeight);
        fill.resizeRelocate(x0, y, usable * Fmt.clamp(value.get(), 0, 1), trackHeight);
        thumb.resizeRelocate(x0 + usable * Fmt.clamp(value.get(), 0, 1) - THUMB / 2, (h - THUMB) / 2, THUMB, THUMB);
    }

    @Override
    protected double computePrefWidth(double height) {
        return 120;
    }

    public DoubleProperty valueProperty() {
        return value;
    }

    public DoubleProperty bufferedProperty() {
        return buffered;
    }

    public BooleanProperty scrubbingProperty() {
        return scrubbing;
    }

    public void setOnScrub(DoubleConsumer onScrub) {
        this.onScrub = onScrub;
    }

    public void setOnCommit(DoubleConsumer onCommit) {
        this.onCommit = onCommit;
    }
}
