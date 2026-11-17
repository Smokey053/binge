package com.binge.ui.component;

import com.binge.model.Movie;
import com.binge.ui.AppContext;
import javafx.geometry.Orientation;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * A responsive wall of thumbnails. The number of columns follows the available width so cards
 * never get smaller than {@value #MIN_CARD_WIDTH}px; the leftover space is shared between them.
 */
public final class PosterGrid extends Region {

    private static final double MIN_CARD_WIDTH = 168;
    private static final double GAP = 18;
    private static final double SIDE_PAD = 20;
    private static final double VERT_PAD = 16;

    private final AppContext context;
    private final List<MovieCard> cards = new ArrayList<>();

    public PosterGrid(AppContext context) {
        this.context = context;
        getStyleClass().add("poster-grid");
    }

    public void setMovies(List<Movie> movies) {
        getChildren().clear();
        cards.clear();
        for (Movie movie : movies) {
            MovieCard card = new MovieCard(movie, context, MIN_CARD_WIDTH);
            cards.add(card);
        }
        getChildren().addAll(cards);
        requestLayout();
    }

    public int size() {
        return cards.size();
    }

    private int columns(double width) {
        double usable = Math.max(0, width - 2 * SIDE_PAD);
        return Math.max(2, (int) ((usable + GAP) / (MIN_CARD_WIDTH + GAP)));
    }

    private double cardWidth(double width, int columns) {
        double usable = Math.max(0, width - 2 * SIDE_PAD);
        return Math.max(100, (usable - GAP * (columns - 1)) / columns);
    }

    @Override
    protected void layoutChildren() {
        int columns = columns(getWidth());
        double cardW = cardWidth(getWidth(), columns);
        double cardH = MovieCard.heightFor(cardW);
        for (int i = 0; i < cards.size(); i++) {
            MovieCard card = cards.get(i);
            card.setCardWidth(cardW);
            card.resizeRelocate(SIDE_PAD + (i % columns) * (cardW + GAP),
                    VERT_PAD + (i / columns) * (cardH + GAP), cardW, cardH);
        }
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computePrefHeight(double width) {
        double w = width < 0 ? getWidth() : width;
        if (cards.isEmpty()) {
            return 0;
        }
        int columns = columns(w);
        int rows = (int) Math.ceil(cards.size() / (double) columns);
        double cardH = MovieCard.heightFor(cardWidth(w, columns));
        return 2 * VERT_PAD + rows * cardH + (rows - 1) * GAP;
    }

    @Override
    protected double computePrefWidth(double height) {
        return 600;
    }
}
