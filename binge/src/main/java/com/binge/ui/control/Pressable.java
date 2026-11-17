package com.binge.ui.control;

import com.binge.audio.Sound;
import com.binge.ui.UiSound;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * Base class of every clickable element in the app. The assignment forbids
 * {@code javafx.scene.control.Button}, so all buttons are built from plain layout nodes that
 * react to the mouse and keyboard, play feedback sounds and are styled entirely through CSS
 * ({@code :hover}, {@code :pressed}, {@code :focused} and custom pseudo-classes).
 */
public abstract class Pressable extends StackPane {

    private Runnable onAction = () -> { };
    private Sound hoverSound = Sound.HOVER;
    private Sound clickSound = Sound.CLICK;

    protected Pressable() {
        getStyleClass().add("pressable");
        setFocusTraversable(true);
        setAccessibleRole(AccessibleRole.BUTTON);
        setCursor(Cursor.HAND);

        addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            if (hoverSound != null && !isDisabled()) {
                UiSound.play(hoverSound);
            }
        });
        addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && !isDisabled()) {
                setScaleX(0.95);
                setScaleY(0.95);
            }
        });
        addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            setScaleX(1);
            setScaleY(1);
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && !isDisabled()) {
                fire();
                e.consume();
            }
        });
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                fire();
                e.consume();
            }
        });
    }

    /** Runs the click handler (and plays the click sound). */
    public void fire() {
        if (clickSound != null) {
            UiSound.play(clickSound);
        }
        onAction.run();
    }

    public void setOnAction(Runnable action) {
        this.onAction = action == null ? () -> { } : action;
    }

    public void setHoverSound(Sound sound) {
        this.hoverSound = sound;
    }

    public void setClickSound(Sound sound) {
        this.clickSound = sound;
    }
}
