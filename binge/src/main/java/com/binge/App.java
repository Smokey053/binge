package com.binge;

import com.binge.audio.SoundFx;
import com.binge.data.CatalogRepository;
import com.binge.data.UserLibrary;
import com.binge.model.Catalog;
import com.binge.ui.UiSound;
import com.binge.ui.Icons;
import com.binge.ui.control.Icon;
import com.binge.ui.view.AppShell;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Binge - a Netflix-style movie streaming demo. Browse genre shelves, hover thumbnails for a
 * preview, and watch official trailers streamed straight from YouTube with fully custom controls.
 */
public class App extends Application {

    private AppShell shell;

    @Override
    public void start(Stage stage) {
        Catalog catalog = CatalogRepository.load();
        UserLibrary library = UserLibrary.atDefaultLocation();

        SoundFx sound = new SoundFx();
        sound.enabledProperty().bindBidirectional(library.soundEnabledProperty());
        UiSound.install(sound);

        shell = new AppShell(catalog, library, sound, getHostServices());
        StackPane root = new StackPane(shell);
        root.getChildren().add(shell.createSplash(splash -> root.getChildren().remove(splash)));

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1360, screen.getWidth() * 0.94);
        double height = Math.min(860, screen.getHeight() * 0.94);
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(Objects.requireNonNull(
                App.class.getResource("/com/binge/app.css")).toExternalForm());

        stage.setTitle("Binge");
        stage.setScene(scene);
        stage.setMinWidth(Math.min(860, screen.getWidth()));
        stage.setMinHeight(Math.min(600, screen.getHeight()));
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.getIcons().add(createAppIcon());
        stage.setOnCloseRequest(e -> Platform.exit());
        stage.show();

        // Heavier set-up runs after the first frame so the window appears immediately.
        Platform.runLater(() -> {
            sound.warmUp();
            shell.warmUp();
        });
    }

    @Override
    public void stop() {
        if (shell != null) {
            shell.dispose();
        }
    }

    /** Renders the brand mark into an image for the window icon. */
    private static Image createAppIcon() {
        StackPane tile = new StackPane(new Icon(Icons.FILM, 34));
        tile.setStyle("-fx-background-color: #ff7a1a; -fx-background-radius: 14;");
        tile.setMinSize(64, 64);
        tile.setMaxSize(64, 64);
        tile.getChildren().get(0).setStyle("-fx-background-color: #211207;");
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        Scene scene = new Scene(tile, Color.TRANSPARENT);
        scene.getRoot().applyCss();
        return tile.snapshot(params, null);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
