package com.binge.ui.control;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** A navigation tab with an optional item count. */
public final class NavLink extends Pressable {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final Label label;
    private final Label badge = new Label();

    public NavLink(String text) {
        getStyleClass().add("nav-link");
        label = new Label(text);
        label.setMouseTransparent(true);
        badge.getStyleClass().add("nav-badge");
        badge.setVisible(false);
        badge.setManaged(false);
        badge.setMouseTransparent(true);
        HBox content = new HBox(8, label, badge);
        content.setAlignment(Pos.CENTER);
        content.setMouseTransparent(true);
        getChildren().add(content);
    }

    public void setSelected(boolean selected) {
        pseudoClassStateChanged(SELECTED, selected);
    }

    /** Shows or hides the caption in compact layouts. */
    public void setLabelVisible(boolean visible) {
        label.setVisible(visible);
        label.setManaged(visible);
    }

    /** Small counter bubble, e.g. the number of titles in My List. A count of 0 hides it. */
    public void setBadge(int count) {
        badge.setText(String.valueOf(count));
        badge.setVisible(count > 0);
        badge.setManaged(count > 0);
    }
}
