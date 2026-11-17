package com.binge.ui.view;

import com.binge.audio.Sound;
import com.binge.ui.Icons;
import com.binge.ui.UiSound;
import com.binge.ui.control.Icon;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.function.Consumer;
/** A short start-up animation: the logo pops in, the letters of the wordmark rise one by one, then it fades out. */
public final class SplashView extends StackPane {

    public SplashView(Consumer<SplashView> onFinished) {
        getStyleClass().add("splash");

        StackPane logo = new StackPane(new Icon(Icons.FILM, 38));
        logo.getStyleClass().add("splash-logo");
        logo.setMinSize(76, 76);
        logo.setMaxSize(76, 76);
        logo.setScaleX(0.4);
        logo.setScaleY(0.4);
        logo.setOpacity(0);

        HBox letters = new HBox(4);
        letters.setAlignment(Pos.CENTER);
        for (char c : "BINGE".toCharArray()) {
            Label letter = new Label(String.valueOf(c));
            letter.getStyleClass().add("splash-letter");
            letter.setOpacity(0);
            letter.setTranslateY(26);
            letters.getChildren().add(letter);
        }
        VBox column = new VBox(18, logo, letters);
        column.setAlignment(Pos.CENTER);
        getChildren().add(column);

        ScaleTransition pop = new ScaleTransition(Duration.millis(520), logo);
        pop.setToX(1);
        pop.setToY(1);
        pop.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition logoFade = new FadeTransition(Duration.millis(400), logo);
        logoFade.setToValue(1);
        ParallelTransition logoIn = new ParallelTransition(pop, logoFade);

        ParallelTransition wordmark = new ParallelTransition();
        for (int i = 0; i < letters.getChildren().size(); i++) {
            var letter = letters.getChildren().get(i);
            FadeTransition fade = new FadeTransition(Duration.millis(300), letter);
            fade.setToValue(1);
            TranslateTransition rise = new TranslateTransition(Duration.millis(380), letter);
            rise.setToY(0);
            rise.setInterpolator(Interpolator.EASE_OUT);
            ParallelTransition one = new ParallelTransition(fade, rise);
            one.setDelay(Duration.millis(90L * i));
            wordmark.getChildren().add(one);
        }
        FadeTransition leave = new FadeTransition(Duration.millis(450), this);
        leave.setToValue(0);

        SequentialTransition show = new SequentialTransition(
                logoIn, wordmark, new PauseTransition(Duration.millis(520)), leave);
        show.setOnFinished(e -> onFinished.accept(this));
        setOnMouseClicked(e -> show.jumpTo(Duration.INDEFINITE));
        UiSound.play(Sound.INTRO);
        show.play();
    }
}
