package com.binge.ui.control;

import javafx.css.PseudoClass;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/** A round icon-only button (play/pause, volume, arrows, ...). */
public final class IconButton extends Pressable {

    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");

    private final Icon icon;
    private Tooltip tooltip;

    public IconButton(String iconPath, double iconSize, String tooltip) {
        getStyleClass().add("icon-button");
        icon = new Icon(iconPath, iconSize);
        getChildren().add(icon);
        double box = iconSize + 18;
        setMinSize(box, box);
        setPrefSize(box, box);
        setMaxSize(box, box);
        if (tooltip != null) {
            setTooltipText(tooltip);
        }
    }

    public void setIcon(String iconPath) {
        icon.setPath(iconPath);
    }

    /** Highlights the button, e.g. when an item is in My List. */
    public void setActive(boolean active) {
        pseudoClassStateChanged(ACTIVE, active);
    }

    public void setTooltipText(String text) {
        if (tooltip == null) {
            tooltip = new Tooltip(text);
            tooltip.setShowDelay(Duration.millis(450));
            Tooltip.install(this, tooltip);
        } else {
            tooltip.setText(text);
        }
    }
}
