package com.binge.ui.component;

import com.binge.audio.Sound;
import com.binge.model.Movie;
import com.binge.ui.AppContext;
import com.binge.ui.Icons;
import com.binge.ui.UiSound;
import com.binge.ui.control.IconButton;
import com.binge.ui.control.Pressable;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * A labelled shelf of movies, like a row on Netflix. Exactly {@value #VISIBLE} thumbnails are
 * visible at any window size (cards grow and shrink with the window); the rest are reached by
 * paging with the arrows, the page dots, the keyboard (tab), a horizontal scroll gesture or a swipe.
 */
public final class MovieRow extends VBox {

    /** Number of thumbnails shown at once, as required by the assignment. */
    public static final int VISIBLE = 5;

    private static final double GAP = 16;
    private static final double SIDE_PAD = 20;
    private static final double VERT_PAD = 18;
    private static final PseudoClass CURRENT = PseudoClass.getPseudoClass("current");

    private final Carousel carousel;
    private final HBox dots = new HBox(6);

    public MovieRow(String title, List<Movie> movies, AppContext context, Runnable onSeeAll) {
        getStyleClass().add("movie-row");
        setSpacing(2);

        carousel = new Carousel(movies, context);

        Label heading = new Label(title);
        heading.getStyleClass().add("row-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        dots.setAlignment(Pos.CENTER_RIGHT);
        dots.getStyleClass().add("page-dots");
        HBox header = new HBox(14, heading, spacer, dots);
        header.setAlignment(Pos.BASELINE_LEFT);
        header.setPadding(new Insets(0, SIDE_PAD, 0, SIDE_PAD));
        if (onSeeAll != null) {
            Pressable seeAll = new SeeAllLink(onSeeAll);
            header.getChildren().add(seeAll);
        }
        getChildren().addAll(header, carousel);

        rebuildDots();
        carousel.pageProperty().addListener((o, was, now) -> updateDots());
    }

    public int pageCount() {
        return carousel.pageCount();
    }

    public int page() {
        return carousel.pageProperty().get();
    }

    public void goToPage(int page) {
        carousel.goToPage(page, true);
    }

    public int movieCount() {
        return carousel.cards.size();
    }

    /** Fades and slides the row in; rows are staggered by their delay when the page first appears. */
    public void playEntrance(double delayMillis) {
        setOpacity(0);
        setTranslateY(22);
        FadeTransition fade = new FadeTransition(Duration.millis(420), this);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(420), this);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition in = new ParallelTransition(fade, slide);
        in.setDelay(Duration.millis(delayMillis));
        in.play();
    }

    private void rebuildDots() {
        dots.getChildren().clear();
        if (carousel.pageCount() < 2) {
            return;
        }
        for (int i = 0; i < carousel.pageCount(); i++) {
            int target = i;
            Pressable dot = new Pressable() {
                { getStyleClass().add("page-dot"); setMinSize(22, 14); setMaxSize(22, 14); }
            };
            dot.setHoverSound(null);
            dot.setOnAction(() -> carousel.goToPage(target, true));
            dots.getChildren().add(dot);
        }
        updateDots();
    }

    private void updateDots() {
        for (int i = 0; i < dots.getChildren().size(); i++) {
            dots.getChildren().get(i).pseudoClassStateChanged(
                    CURRENT, i == carousel.pageProperty().get());
        }
    }

    /** "See all" text link in the row header. */
    private static final class SeeAllLink extends Pressable {
        SeeAllLink(Runnable action) {
            getStyleClass().add("see-all");
            Label label = new Label("See all");
            label.setMouseTransparent(true);
            getChildren().add(label);
            setOnAction(action);
        }
    }

    /** The clipped viewport plus the sliding track of cards. */
    private static final class Carousel extends Region {

        private final List<MovieCard> cards = new ArrayList<>();
        private final Pane track = new Pane();
        private final IconButton previous = new IconButton(Icons.CHEVRON_LEFT, 26, null);
        private final IconButton next = new IconButton(Icons.CHEVRON_RIGHT, 26, null);
        private final DoubleProperty offset = new SimpleDoubleProperty();
        private final IntegerProperty page = new SimpleIntegerProperty();
        private final Timeline slide = new Timeline();
        private long lastScroll;

        Carousel(List<Movie> movies, AppContext context) {
            getStyleClass().add("carousel");
            for (Movie movie : movies) {
                MovieCard card = new MovieCard(movie, context, 180);
                int index = cards.size();
                card.focusedProperty().addListener((o, was, focused) -> {
                    if (focused) {
                        goToPage(index / VISIBLE, true);
                    }
                });
                cards.add(card);
            }
            track.getChildren().addAll(cards);
            track.setPickOnBounds(false);
            getChildren().addAll(track, previous, next);

            Rectangle clip = new Rectangle();
            setClip(clip);
            // Clip only the track overflow. Keeping the clip flush with the row leaves the
            // carousel controls fully visible at both edges.
            layoutBoundsProperty().addListener((o, was, bounds) -> {
                clip.setX(0);
                clip.setWidth(Math.max(0, bounds.getWidth()));
                clip.setHeight(bounds.getHeight());
            });

            for (IconButton arrow : List.of(previous, next)) {
                arrow.getStyleClass().add("carousel-arrow");
                arrow.setHoverSound(null);
                arrow.setOpacity(0);
                arrow.setMouseTransparent(true);
            }
            previous.setOnAction(() -> goToPage(page.get() - 1, true));
            next.setOnAction(() -> goToPage(page.get() + 1, true));
            previous.setClickSound(null);
            next.setClickSound(null);

            offset.addListener((o, was, now) -> track.setTranslateX(-now.doubleValue()));
            page.addListener((o, was, now) -> refreshArrows());
            hoverProperty().addListener((o, was, now) -> refreshArrows());

            addEventFilter(ScrollEvent.SCROLL, e -> {
                if (Math.abs(e.getDeltaX()) > Math.abs(e.getDeltaY()) && Math.abs(e.getDeltaX()) > 2) {
                    long now = System.currentTimeMillis();
                    if (now - lastScroll > 420) {
                        lastScroll = now;
                        goToPage(page.get() + (e.getDeltaX() < 0 ? 1 : -1), true);
                    }
                    e.consume();
                }
            });
            setOnSwipeLeft(e -> goToPage(page.get() + 1, true));
            setOnSwipeRight(e -> goToPage(page.get() - 1, true));
        }

        IntegerProperty pageProperty() {
            return page;
        }

        int pageCount() {
            return Math.max(1, (int) Math.ceil(cards.size() / (double) VISIBLE));
        }

        void goToPage(int target, boolean animate) {
            int clamped = Math.max(0, Math.min(pageCount() - 1, target));
            boolean changed = clamped != page.get();
            page.set(clamped);
            double to = offsetForPage(clamped);
            slide.stop();
            if (animate && changed) {
                UiSound.play(Sound.SWIPE);
                slide.getKeyFrames().setAll(new KeyFrame(Duration.millis(520),
                        new KeyValue(offset, to, Interpolator.SPLINE(0.25, 0.1, 0.25, 1))));
                slide.play();
            } else {
                offset.set(to);
            }
        }

        private double cardWidth(double width) {
            double inner = Math.max(0, width - 2 * SIDE_PAD);
            return Math.max(70, (inner - GAP * (VISIBLE - 1)) / VISIBLE);
        }

        private double offsetForPage(int p) {
            double cardW = cardWidth(getWidth());
            double step = cardW + GAP;
            double inner = Math.max(0, getWidth() - 2 * SIDE_PAD);
            double maxOffset = Math.max(0, cards.size() * step - GAP - inner);
            return Math.min(p * VISIBLE * step, maxOffset);
        }

        private void refreshArrows() {
            boolean show = isHover();
            fadeTo(previous, show && page.get() > 0);
            fadeTo(next, show && page.get() < pageCount() - 1);
        }

        private void fadeTo(Node arrow, boolean visible) {
            arrow.setMouseTransparent(!visible);
            FadeTransition fade = new FadeTransition(Duration.millis(160), arrow);
            fade.setToValue(visible ? 1 : 0);
            fade.play();
        }

        @Override
        protected void layoutChildren() {
            double cardW = cardWidth(getWidth());
            double cardH = MovieCard.heightFor(cardW);
            double step = cardW + GAP;
            track.resizeRelocate(SIDE_PAD, 0, Math.max(0, cards.size() * step), getHeight());
            for (int i = 0; i < cards.size(); i++) {
                MovieCard card = cards.get(i);
                card.setCardWidth(cardW);
                card.resizeRelocate(i * step, VERT_PAD, cardW, cardH);
            }
            if (!slide.getStatus().equals(Animation.Status.RUNNING)) {
                offset.set(offsetForPage(page.get()));
            }
            double arrowY = VERT_PAD + cardW * 1.5 / 2 - previous.prefHeight(-1) / 2;
            previous.resizeRelocate(SIDE_PAD - 12, arrowY, previous.prefWidth(-1), previous.prefHeight(-1));
            next.resizeRelocate(getWidth() - SIDE_PAD - next.prefWidth(-1) + 12, arrowY, next.prefWidth(-1), next.prefHeight(-1));
        }

        @Override
        public Orientation getContentBias() {
            return Orientation.HORIZONTAL;
        }

        @Override
        protected double computePrefHeight(double width) {
            double w = width < 0 ? getWidth() : width;
            return 2 * VERT_PAD + MovieCard.heightFor(cardWidth(w));
        }

        @Override
        protected double computePrefWidth(double height) {
            return 600;
        }

        @Override
        protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }
    }
}
