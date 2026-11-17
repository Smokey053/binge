package com.binge.ui.view;

import com.binge.model.Movie;
import com.binge.ui.AppContext;

import java.util.List;

/** Results of the search box, ranked by relevance. */
public final class SearchView extends GalleryView {

    private final AppContext context;

    public SearchView(AppContext context) {
        super(context);
        this.context = context;
    }

    public void search(String query) {
        List<Movie> results = context.catalog().search(query);
        setHeading("Results for “" + query.strip() + "”",
                results.size() + (results.size() == 1 ? " title found" : " titles found"));
        setEmpty("No matches", "Try a movie name, a director, a genre like “horror”, or a year.", null);
        setMovies(results);
        scrollToTop();
    }
}
