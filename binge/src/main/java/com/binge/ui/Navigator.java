package com.binge.ui;

import com.binge.model.Movie;

/** What the UI components can ask the application shell to do. */
public interface Navigator {

    /** Opens the trailer player for a movie. */
    void play(Movie movie);

    /** Opens the Browse page, optionally pre-filtered to a category id ({@code null} shows everything). */
    void browse(String categoryId);
}
