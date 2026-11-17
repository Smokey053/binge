package com.binge.ui.control;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * The custom "trailer is loading" animation: a spinning film reel with a sweeping arc, sonar
 * ripples that radiate from the hub and a row of bouncing dots under the message. Everything is
 * drawn with plain shapes and driven by transitions; nothing is imported as an image or GIF.
 */
public final class LoadingReel extends VBox {

    private static final double SIZE = 150;

    private final List<Animation> animations = new ArrayList<>();
    private final Label title = new Label();
    private final Label message = new Label("Loading trailer");

    public LoadingReel() {
        getStyleClass().add("loading-reel");
        setAlignment(Pos.CENTER);
        setSpacing(18);
        setMouseTransparent(true);

        title.getStyleClass().add("loading-title");
        message.getStyleClass().add("loading-message");

        StackPane stage = new StackPane(buildRipples(), buildReel(), buildSweep());
        stage.setMinSize(SIZE * 1.5, SIZE * 1.5);
        stage.setPrefSize(SIZE * 1.5, SIZE * 1.5);
        stage.setMaxSize(SIZE * 1.5, SIZE * 1.5);

        getChildren().addAll(stage, title, message, buildDots());
        visibleProperty().addListener((o, was, visible) -> {
            if (visible) {
                play();
            } else {
                stop();
            }
        });
    }

    public void setTitle(String text) {
        title.setText(text);
    }

    public void setMessage(String text) {
        message.setText(text);
    }

    public void play() {
        animations.forEach(Animation::play);
    }

    public void stop() {
        animations.forEach(Animation::pause);
    }

    /** Concentric rings that expand and fade, like a signal being searched for. */
    private Group buildRipples() {
        Group group = new Group();
        for (int i = 0; i < 3; i++) {
            Circle ring = new Circle(SIZE * 0.34);
            ring.getStyleClass().add("loading-ripple");
            ring.setOpacity(0);
            ScaleTransition grow = new ScaleTransition(Duration.seconds(2.4), ring);
            grow.setFromX(0.8);
            grow.setFromY(0.8);
            grow.setToX(2.0);
            grow.setToY(2.0);
            grow.setInterpolator(Interpolator.EASE_OUT);
            FadeTransition fade = new FadeTransition(Duration.seconds(2.4), ring);
            fade.setFromValue(0.55);
            fade.setToValue(0);
            ParallelTransition pulse = new ParallelTransition(grow, fade);
            SequentialTransition delayed = new SequentialTransition(new PauseTransition(Duration.seconds(0.8 * i)), pulse);
            delayed.setCycleCount(Animation.INDEFINITE);
            animations.add(delayed);
            group.getChildren().add(ring);
        }
        return group;
    }

    /** The reel body: a disc with six cut-outs and a hub, rotating at a constant speed. */
    private Group buildReel() {
        Group reel = new Group();
        Circle disc = new Circle(SIZE * 0.34);
        disc.getStyleClass().add("loading-disc");
        reel.getChildren().add(disc);
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i);
            Circle hole = new Circle(SIZE * 0.075);
            hole.getStyleClass().add("loading-hole");
            hole.setCenterX(Math.cos(angle) * SIZE * 0.2);
            hole.setCenterY(Math.sin(angle) * SIZE * 0.2);
            reel.getChildren().add(hole);
        }
        Circle hub = new Circle(SIZE * 0.055);
        hub.getStyleClass().add("loading-hub");
        reel.getChildren().add(hub);

        RotateTransition spin = new RotateTransition(Duration.seconds(3.2), reel);
        spin.setByAngle(360);
        spin.setInterpolator(Interpolator.LINEAR);
        spin.setCycleCount(Animation.INDEFINITE);
        animations.add(spin);
        return reel;
    }

    /** An accent arc that sweeps around the reel in the opposite direction. */
    private Group buildSweep() {
        double radius = SIZE * 0.42;
        Arc arc = new Arc(0, 0, radius, radius, 90, 95);
        arc.setType(ArcType.OPEN);
        arc.getStyleClass().add("loading-sweep");
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setFill(null);

        // An invisible full circle keeps the group centred on the reel, so it rotates around the hub.
        Circle bounds = new Circle(radius + 3);
        bounds.setFill(Color.TRANSPARENT);
        Group sweep = new Group(bounds, arc);

        RotateTransition spin = new RotateTransition(Duration.seconds(1.3), sweep);
        spin.setByAngle(-360);
        spin.setInterpolator(Interpolator.LINEAR);
        spin.setCycleCount(Animation.INDEFINITE);
        animations.add(spin);
        return sweep;
    }
    private StackPane buildDots() {
        HBox dots = new HBox(7);
        dots.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Region dot = new Region();
            dot.getStyleClass().add("loading-dot");
            dot.setMinSize(7, 7);
            dot.setPrefSize(7, 7);
            dot.setMaxSize(7, 7);
            Timeline bounce = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(dot.translateYProperty(), 0), new KeyValue(dot.opacityProperty(), 0.35)),
                    new KeyFrame(Duration.millis(300), new KeyValue(dot.translateYProperty(), -7, Interpolator.EASE_OUT), new KeyValue(dot.opacityProperty(), 1)),
                    new KeyFrame(Duration.millis(600), new KeyValue(dot.translateYProperty(), 0, Interpolator.EASE_IN), new KeyValue(dot.opacityProperty(), 0.35)),
                    new KeyFrame(Duration.millis(1000), new KeyValue(dot.translateYProperty(), 0), new KeyValue(dot.opacityProperty(), 0.35)));
            bounce.setCycleCount(Animation.INDEFINITE);
            bounce.setDelay(Duration.millis(160L * i));
            animations.add(bounce);
            dots.getChildren().add(dot);
        }
        return new StackPane(dots);
    }
}
