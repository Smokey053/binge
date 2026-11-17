package com.binge.ui.view;

import com.binge.data.UserLibrary;
import com.binge.ui.Icons;
import com.binge.ui.control.Icon;
import com.binge.ui.control.PillButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Application preferences kept separate from the primary browsing navigation. */
public final class SettingsView extends StackPane {

    private final PillButton soundButton = new PillButton("", Icons.VOLUME_UP, PillButton.Kind.GHOST);
    private final PillButton themeButton = new PillButton("", Icons.MOON, PillButton.Kind.GHOST);

    public SettingsView(UserLibrary library) {
        getStyleClass().add("settings-view");

        Label title = new Label("Settings");
        title.getStyleClass().add("settings-title");

        HBox appearanceRow = preferenceRow("Appearance", "Choose a dark or light interface",
                Icons.SUN, themeButton);
        appearanceRow.getStyleClass().add("settings-row-separator");
        VBox card = new VBox(2,
                preferenceRow("Sound effects", "Play interface feedback sounds",
                        Icons.VOLUME_UP, soundButton),
                appearanceRow);
        card.getStyleClass().add("settings-card");

        soundButton.setOnAction(() ->
                library.soundEnabledProperty().set(!library.soundEnabledProperty().get()));
        themeButton.setOnAction(() ->
                library.lightThemeProperty().set(!library.lightThemeProperty().get()));

        library.soundEnabledProperty().addListener((o, was, enabled) -> refreshSound(enabled));
        library.lightThemeProperty().addListener((o, was, light) -> refreshTheme(light));
        refreshSound(library.soundEnabledProperty().get());
        refreshTheme(library.lightThemeProperty().get());

        VBox content = new VBox(24, title, card);
        content.setMaxWidth(760);
        content.setPadding(new Insets(112, 40, 40, 40));
        getChildren().add(content);
        StackPane.setAlignment(content, Pos.TOP_CENTER);
    }

    private HBox preferenceRow(String title, String detail, String iconPath, PillButton action) {
        Icon icon = new Icon(iconPath, 22);
        icon.getStyleClass().add("settings-row-icon");
        Label heading = new Label(title);
        heading.getStyleClass().add("settings-row-title");
        Label description = new Label(detail);
        description.getStyleClass().add("settings-row-detail");
        VBox copy = new VBox(4, heading, description);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(18, icon, copy, spacer, action);
        row.getStyleClass().add("settings-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void refreshSound(boolean enabled) {
        soundButton.setText(enabled ? "On" : "Off");
        soundButton.setIcon(enabled ? Icons.VOLUME_UP : Icons.VOLUME_OFF);
        setSelected(soundButton, enabled);
    }

    private void refreshTheme(boolean light) {
        themeButton.setText(light ? "Light" : "Dark");
        themeButton.setIcon(light ? Icons.SUN : Icons.MOON);
        setSelected(themeButton, !light);
    }

    private void setSelected(PillButton button, boolean selected) {
        if (selected) {
            if (!button.getStyleClass().contains("selected")) {
                button.getStyleClass().add("selected");
            }
        } else {
            button.getStyleClass().remove("selected");
        }
    }
}
