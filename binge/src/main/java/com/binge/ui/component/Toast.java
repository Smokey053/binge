package com.binge.ui.component;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** A small message that slides up from the bottom of the window and fades away by itself. */
public final class Toast extends StackPane {

    private final Label label = new Label();
    private SequentialTransition running;

    public Toast() {
        getStyleClass().add("toast");
        getChildren().add(label);
        setMouseTransparent(true);
        setVisible(false);
        setOpacity(0);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        StackPane.setAlignment(this, Pos.BOTTOM_CENTER);
        StackPane.setMargin(this, new Insets(0, 0, 36, 0));
    }

    public void show(String message) {
        if (running != null) {
            running.stop();
        }
        label.setText(message);
        setVisible(true);
        setTranslateY(18);
        setOpacity(0);

        FadeTransition in = new FadeTransition(Duration.millis(200), this);
        in.setToValue(1);
        TranslateTransition rise = new TranslateTransition(Duration.millis(200), this);
        rise.setToY(0);
        PauseTransition hold = new PauseTransition(Duration.millis(1900));
        FadeTransition out = new FadeTransition(Duration.millis(320), this);
        out.setToValue(0);
        running = new SequentialTransition(new javafx.animation.ParallelTransition(in, rise), hold, out);
        running.setOnFinished(e -> setVisible(false));
        running.play();
    }
}
