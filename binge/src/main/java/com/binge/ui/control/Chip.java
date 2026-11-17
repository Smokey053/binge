package com.binge.ui.control;

import javafx.css.PseudoClass;
import javafx.scene.control.Label;

/** A selectable pill used for the genre filter. */
public final class Chip extends Pressable {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    public Chip(String text) {
        getStyleClass().add("chip");
        Label label = new Label(text);
        label.setMouseTransparent(true);
        getChildren().add(label);
    }

    public void setSelected(boolean selected) {
        pseudoClassStateChanged(SELECTED, selected);
    }
}
