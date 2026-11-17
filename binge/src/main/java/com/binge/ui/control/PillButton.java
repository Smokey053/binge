package com.binge.ui.control;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** A rounded text button with an optional leading icon, e.g. "Play Trailer". */
public final class PillButton extends Pressable {

    /** Visual variants, mapped to style classes in app.css. */
    public enum Kind {
        PRIMARY("primary"), GHOST("ghost"), GLASS("glass");

        private final String styleClass;

        Kind(String styleClass) {
            this.styleClass = styleClass;
        }
    }

    private final Label label = new Label();
    private final Icon icon;

    public PillButton(String text, String iconPath, Kind kind) {
        getStyleClass().addAll("pill-button", kind.styleClass);
        label.setText(text);
        label.setMouseTransparent(true);
        HBox content = new HBox(9);
        content.setAlignment(Pos.CENTER);
        content.setMouseTransparent(true);
        if (iconPath != null) {
            icon = new Icon(iconPath, 18);
            content.getChildren().add(icon);
        } else {
            icon = null;
        }
        content.getChildren().add(label);
        getChildren().add(content);
    }

    public void setText(String text) {
        label.setText(text);
    }

    public void setIcon(String iconPath) {
        if (icon != null) {
            icon.setPath(iconPath);
        }
    }
}
