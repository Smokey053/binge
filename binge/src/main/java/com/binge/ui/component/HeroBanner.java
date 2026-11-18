package com.binge.ui.component;

import com.binge.model.Movie;
import com.binge.ui.AppContext;
import com.binge.ui.Icons;
import com.binge.ui.Images;
import com.binge.ui.control.CoverImageView;
import com.binge.ui.control.Pressable;
import com.binge.ui.control.PillButton;
import com.binge.util.Fmt;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;

/** The large featured banner at the top of the home page. It cross-fades through the featured
 movies every few seconds (paused while the pointer is over it) and offers one-click trailer
 playback and a My List toggle.
 */
public final class HeroBanner extends StackPane {

    private static final PseudoClass CURRENT = PseudoClass.getPseudoClass("current");
    private static final PseudoClass COMPACT = PseudoClass.getPseudoClass("compact");
    private static final PseudoClass NARROW = PseudoClass.getPseudoClass("narrow");

    private final AppContext context;
    private final List<Movie> movies;
    private final CoverImageView backdropBack = new CoverImageView();
    private final CoverImageView backdropFront = new CoverImageView();
    private final Label tag = new Label();
    private final Label title = new Label();
    private final Label meta = new Label();
    private final Label synopsis = new Label();
    private final PillButton playButton = new PillButton("Play Trailer", Icons.PLAY, PillButton.Kind.PRIMARY);
    private final PillButton listButton = new PillButton("My List", Icons.ADD, PillButton.Kind.GLASS);
    private final HBox dots = new HBox(8);
    private final VBox content = new VBox(12);
    private final Timeline rotation;
    private int index = -1;
    private double viewportHeight;

    public HeroBanner(List<Movie> movies, AppContext context) {
        this.movies = movies;
        this.context = context;
        getStyleClass().add("hero");
        setMinHeight(340);

        backdropBack.setFocus(0.5, 0.3);
        backdropFront.setFocus(0.5, 0.3);
        backdropFront.setOpacity(0);

        Region shadeSide = new Region();
        shadeSide.getStyleClass().add("hero-shade-side");
        Region shadeBottom = new Region();
        shadeBottom.getStyleClass().add("hero-shade-bottom");
        shadeSide.setMouseTransparent(true);
        shadeBottom.setMouseTransparent(true);

        tag.getStyleClass().add("hero-tag");
        title.getStyleClass().add("hero-title");
        title.setWrapText(true);
        meta.getStyleClass().add("hero-meta");
        synopsis.getStyleClass().add("hero-synopsis");
        synopsis.setWrapText(true);
        synopsis.setMaxWidth(560);
        synopsis.setMaxHeight(72);
        HBox buttons = new HBox(12, playButton, listButton);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(6, 0, 0, 0));
        content.getChildren().addAll(tag, title, meta, synopsis, buttons);
        content.setAlignment(Pos.BOTTOM_LEFT);
        content.setMaxWidth(640);
        content.setPadding(new Insets(0, 0, 38, 0));

        dots.setAlignment(Pos.CENTER_RIGHT);
        for (int i = 0; i < movies.size(); i++) {
            int target = i;
            Pressable dot = new Pressable() {
                { getStyleClass().add("hero-dot"); setMinSize(26, 14); setMaxSize(26, 14); }
            };
            dot.setHoverSound(null);
            dot.setOnAction(() -> {
                show(target);
                restartRotation();
            });
            dots.getChildren().add(dot);
        }

        StackPane overlay = new StackPane(content, dots);
        overlay.setPadding(new Insets(0, 44, 0, 44));
        // These layers stretch over the whole banner, so they must not swallow clicks meant for the buttons.
        overlay.setPickOnBounds(false);
        content.setPickOnBounds(false);
        dots.setPickOnBounds(false);
        dots.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        StackPane.setAlignment(content, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(dots, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(dots, new Insets(0, 0, 44, 0));

        getChildren().addAll(backdropBack, backdropFront, shadeSide, shadeBottom, overlay);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        widthProperty().addListener((o, was, w) -> {
            pseudoClassStateChanged(COMPACT, w.doubleValue() < 980);
            pseudoClassStateChanged(NARROW, w.doubleValue() < 720);
            synopsis.setMaxWidth(Fmt.clamp(w.doubleValue() * 0.5, 280, 560));
        });

        rotation = new Timeline(new KeyFrame(Duration.seconds(8), e -> show((index + 1) % movies.size())));
        rotation.setCycleCount(Animation.INDEFINITE);
        context.library().myList().addListener((ListChangeListener<String>) change -> {
            if (index >= 0) {
                refreshListButton(movies.get(index));
            }
        });
        hoverProperty().addListener((o, was, hovered) -> {
            if (hovered) {
                rotation.pause();
            } else {
                rotation.play();
            }
        });

        if (!movies.isEmpty()) {
            show(0);
            if (movies.size() > 1) {
                rotation.play();
            }
        }
    }

    /** Stops the automatic rotation (called when the page is hidden). */
    public void pause() {
        rotation.pause();
    }

    private void restartRotation() {
        rotation.playFromStart();
    }

    public void resume() {
        if (movies.size() > 1) {
            rotation.play();
        }
    }

    public int currentIndex() {
        return index;
    }

    private void show(int target) {
        if (movies.isEmpty() || target == index) {
            return;
        }
        boolean first = index < 0;
        index = target;
        Movie movie = movies.get(target);

        Images.backdrop(movie, image -> {
            if (index < 0 || movies.get(index) != movie || image == null) {
                return;
            }
            backdropFront.setImage(image);
            if (first) {
                backdropBack.setImage(image);
                backdropFront.setOpacity(0);
            } else {
                FadeTransition crossfade = new FadeTransition(Duration.millis(750), backdropFront);
                crossfade.setFromValue(0);
                crossfade.setToValue(1);
                crossfade.setOnFinished(e -> {
                    backdropBack.setImage(backdropFront.getImage());
                    backdropFront.setOpacity(0);
                });
                crossfade.play();
            }
        });

        FadeTransition out = new FadeTransition(Duration.millis(first ? 1 : 160), content);
        out.setToValue(0);
        out.setOnFinished(e -> {
            bindContent(movie);
            content.setTranslateY(14);
            FadeTransition in = new FadeTransition(Duration.millis(380), content);
            in.setToValue(1);
            TranslateTransition rise = new TranslateTransition(Duration.millis(380), content);
            rise.setToY(0);
            new ParallelTransition(in, rise).play();
        });
        out.play();

        for (int i = 0; i < dots.getChildren().size(); i++) {
            dots.getChildren().get(i).pseudoClassStateChanged(CURRENT, i == target);
        }
    }

    private void bindContent(Movie movie) {
        tag.setText(index == 0 ? "FEATURED TONIGHT" : "FEATURED");
        title.setText(movie.title());
        title.pseudoClassStateChanged(PseudoClass.getPseudoClass("long"), movie.title().length() > 22);
        meta.setText(movie.year() + "  •  " + movie.genreLine() + "  •  " + movie.director());
        synopsis.setText(movie.synopsis());
        playButton.setOnAction(() -> context.navigator().play(movie));
        listButton.setOnAction(() -> {
            context.library().toggleMyList(movie);
            refreshListButton(movie);
        });
        refreshListButton(movie);
    }

    private void refreshListButton(Movie movie) {
        boolean in = context.library().isInMyList(movie);
        listButton.setText(in ? "In My List" : "My List");
        listButton.setIcon(in ? Icons.CHECK : Icons.ADD);
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computePrefHeight(double width) {
        double w = width < 0 ? getWidth() : width;
        double byWidth = Fmt.clamp(w * 0.42, 360, 560);
        if (viewportHeight <= 0) {
            return byWidth;
        }
        // Leave room for the first shelf title below the banner on short windows.
        return Fmt.clamp(Math.min(byWidth, viewportHeight * 0.62), 300, 560);
    }

    /** Lets the banner shrink on short windows so the first shelf is always partly visible. */
    public void setViewportHeight(double height) {
        if (Math.abs(height - viewportHeight) > 1) {
            viewportHeight = height;
            requestLayout();
        }
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }
}
