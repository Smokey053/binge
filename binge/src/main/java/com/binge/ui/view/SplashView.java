package com.binge.ui.view;

import com.binge.audio.Sound;
import com.binge.ui.UiSound;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Objects;
import java.util.function.Consumer;

/** A short start-up animation that introduces the supplied brand mark. */
public final class SplashView extends StackPane {

    public SplashView(Consumer<SplashView> onFinished) {
        getStyleClass().add("splash");

        ImageView logo = new ImageView(new Image(Objects.requireNonNull(
                SplashView.class.getResourceAsStream("/com/binge/branding/binge-logo.png"))));
        logo.getStyleClass().add("splash-logo");
        logo.setPreserveRatio(true);
        logo.setFitWidth(520);
        logo.setSmooth(true);
        logo.setScaleX(0.4);
        logo.setScaleY(0.4);
        logo.setOpacity(0);

        getChildren().add(logo);
        StackPane.setAlignment(logo, Pos.CENTER);

        ScaleTransition pop = new ScaleTransition(Duration.millis(520), logo);
        pop.setToX(1);
        pop.setToY(1);
        pop.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition logoFade = new FadeTransition(Duration.millis(400), logo);
        logoFade.setToValue(1);
        ParallelTransition logoIn = new ParallelTransition(pop, logoFade);

        FadeTransition leave = new FadeTransition(Duration.millis(450), this);
        leave.setToValue(0);

        SequentialTransition show = new SequentialTransition(
                logoIn, new PauseTransition(Duration.millis(520)), leave);
        show.setOnFinished(e -> onFinished.accept(this));
        setOnMouseClicked(e -> show.jumpTo(Duration.INDEFINITE));
        UiSound.play(Sound.INTRO);
        show.play();
    }
}
