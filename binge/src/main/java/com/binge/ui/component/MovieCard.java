package com.binge.ui.component;

import com.binge.audio.Sound;
import com.binge.model.Movie;
import com.binge.ui.AppContext;
import com.binge.ui.Icons;
import com.binge.ui.Images;
import com.binge.ui.UiSound;
import com.binge.ui.control.CoverImageView;
import com.binge.ui.control.Icon;
import com.binge.ui.control.IconButton;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * A movie thumbnail with its title underneath. Hovering lifts the card, zooms the poster,
 * adds an accent glow and reveals a play badge, a "My List" toggle and the genres. Clicking
 * the poster or the title opens the trailer.
 */
public final class MovieCard extends VBox {

    private static final double ASPECT = 1.5;

    private final Movie movie;
    private final AppContext context;
    private final StackPane poster = new StackPane();
    private final StackPane inner = new StackPane();
    private final CoverImageView image = new CoverImageView();
    private final StackPane overlay = new StackPane();
    private final StackPane playBadge = new StackPane();
    private IconButton listToggle;
    private ParallelTransition hoverAnimation;
    private final Label title;
    private final Label meta;
    private final Label genres = new Label();
    // Bindings are only weakly referenced by the list they observe, so keep a strong reference.
    private final BooleanBinding inList;

    public MovieCard(Movie movie, AppContext context, double width) {
        this.movie = movie;
        this.context = context;
        this.inList = context.library().inMyListBinding(movie);
        getStyleClass().add("movie-card");
        setSpacing(9);
        setFocusTraversable(true);
        setAccessibleRole(AccessibleRole.BUTTON);
        setAccessibleText(movie.title());
        setCursor(Cursor.HAND);

        buildPoster();
        buildListToggle();
        poster.getChildren().add(listToggle);
        StackPane.setAlignment(listToggle, Pos.TOP_RIGHT);
        StackPane.setMargin(listToggle, new Insets(8));

        title = new Label(movie.title());
        title.getStyleClass().add("card-title");
        title.setMaxWidth(Double.MAX_VALUE);
        meta = new Label(movie.year() + "  •  " + movie.genres().stream().findFirst().orElse(""));
        meta.getStyleClass().add("card-meta");
        meta.setMaxWidth(Double.MAX_VALUE);
        VBox text = new VBox(2, title, meta);
        text.setPadding(new Insets(0, 2, 0, 2));
        getChildren().addAll(poster, text);

        setCardWidth(width);
        wireInteractions();
    }

    public Movie movie() {
        return movie;
    }

    /** Resizes the card; the poster keeps a 2:3 ratio. */
    public void setCardWidth(double width) {
        setMinWidth(width);
        setPrefWidth(width);
        setMaxWidth(width);
        poster.setMinSize(width, width * ASPECT);
        poster.setPrefSize(width, width * ASPECT);
        poster.setMaxSize(width, width * ASPECT);
    }

    /** Height of the card for a given width, used by carousels and grids to size their viewport. */
    public static double heightFor(double width) {
        return width * ASPECT + 9 + 40;
    }

    private void buildPoster() {
        poster.getStyleClass().add("poster-box");
        inner.setStyle("-fx-background-color: " + placeholderGradient(movie) + ";");
        Rectangle clip = new Rectangle();
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        clip.widthProperty().bind(inner.widthProperty());
        clip.heightProperty().bind(inner.heightProperty());
        inner.setClip(clip);

        Label placeholder = new Label(movie.title());
        placeholder.getStyleClass().add("poster-placeholder");
        placeholder.setWrapText(true);
        placeholder.setMaxWidth(140);
        placeholder.setAlignment(Pos.CENTER);

        Images.poster(movie, image::setImage);

        overlay.getStyleClass().add("card-overlay");
        overlay.setOpacity(0);
        overlay.setMouseTransparent(true);

        playBadge.getStyleClass().add("play-badge");
        playBadge.getChildren().add(new Icon(Icons.PLAY, 24));
        playBadge.setMinSize(52, 52);
        playBadge.setMaxSize(52, 52);
        playBadge.setScaleX(0.6);
        playBadge.setScaleY(0.6);
        playBadge.setOpacity(0);
        playBadge.setMouseTransparent(true);

        genres.setText(movie.genreLine());
        genres.getStyleClass().add("card-genres");
        genres.setOpacity(0);
        genres.setMouseTransparent(true);
        StackPane.setAlignment(genres, Pos.BOTTOM_LEFT);
        StackPane.setMargin(genres, new Insets(0, 10, 10, 12));

        // The rounded clip only covers the inner layer so the hover glow on the outer box is never cut off.
        inner.getChildren().addAll(placeholder, image, overlay, playBadge, genres);
        poster.getChildren().add(inner);
    }

    private void buildListToggle() {
        IconButton toggle = new IconButton(Icons.ADD, 16, "Add to My List");
        listToggle = toggle;
        toggle.getStyleClass().add("glass");
        toggle.setOpacity(0);
        toggle.setOnAction(() -> {
            boolean added = context.library().toggleMyList(movie);
            UiSound.play(added ? Sound.ADD : Sound.REMOVE);
        });
        toggle.setHoverSound(null);
        toggle.setClickSound(null);
        inList.addListener((o, was, now) -> refreshListToggle(now));
        refreshListToggle(inList.get());
    }

    private void refreshListToggle(boolean listed) {
        listToggle.setIcon(listed ? Icons.CHECK : Icons.ADD);
        listToggle.setActive(listed);
        listToggle.setTooltipText(listed ? "Remove from My List" : "Add to My List");
        // A title in the list keeps its check mark visible even without hover.
        listToggle.setOpacity(listed || isHover() ? 1 : 0);
    }

    private void wireInteractions() {
        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                context.navigator().play(movie);
            }
        });
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                context.navigator().play(movie);
                e.consume();
            }
        });
        hoverProperty().addListener((o, was, hovered) -> animateHover(hovered));
    }

    private void animateHover(boolean hovered) {
        if (hovered) {
            UiSound.play(Sound.HOVER);
            setViewOrder(-1);
        } else {
            setViewOrder(0);
        }
        if (hoverAnimation != null) {
            hoverAnimation.stop();
        }
        Duration d = Duration.millis(hovered ? 250 : 190);

        ScaleTransition lift = new ScaleTransition(d, this);
        lift.setToX(hovered ? 1.025 : 1);
        lift.setToY(hovered ? 1.025 : 1);
        lift.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition rise = new TranslateTransition(d, this);
        rise.setToY(hovered ? -2.5 : 0);
        rise.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition zoom = new ScaleTransition(Duration.millis(hovered ? 360 : 220), image);
        zoom.setToX(hovered ? 1.04 : 1);
        zoom.setToY(hovered ? 1.04 : 1);
        zoom.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition shade = new FadeTransition(d, overlay);
        shade.setToValue(hovered ? 0.82 : 0);

        ScaleTransition badgeScale = new ScaleTransition(Duration.millis(hovered ? 240 : 120), playBadge);
        badgeScale.setToX(hovered ? 1 : 0.6);
        badgeScale.setToY(hovered ? 1 : 0.6);
        badgeScale.setInterpolator(hovered ? Interpolator.EASE_OUT : Interpolator.EASE_IN);

        FadeTransition badgeFade = new FadeTransition(d, playBadge);
        badgeFade.setToValue(hovered ? 1 : 0);

        FadeTransition genreFade = new FadeTransition(d, genres);
        genreFade.setToValue(hovered ? 1 : 0);

        FadeTransition toggleFade = new FadeTransition(d, listToggle);
        toggleFade.setToValue(hovered || context.library().isInMyList(movie) ? 1 : 0);

        hoverAnimation = new ParallelTransition(lift, rise, zoom, shade, badgeScale, badgeFade, genreFade, toggleFade);
        hoverAnimation.play();
    }

    /** A stable, pleasant gradient so posters that are still loading do not flash as blank boxes. */
    private static String placeholderGradient(Movie movie) {
        int hue = Math.floorMod(movie.title().hashCode(), 360);
        return String.format("linear-gradient(to bottom right, hsb(%d, 35%%, 30%%), hsb(%d, 45%%, 14%%))",
                hue, (hue + 40) % 360);
    }
}
