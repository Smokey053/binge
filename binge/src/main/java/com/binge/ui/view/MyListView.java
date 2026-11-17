package com.binge.ui.view;

import com.binge.model.Movie;
import com.binge.ui.AppContext;
import com.binge.ui.Icons;
import com.binge.ui.control.PillButton;
import javafx.collections.ListChangeListener;

import java.util.List;
import java.util.Optional;

/** The titles the viewer saved, newest first. */
public final class MyListView extends GalleryView {

    private final AppContext context;

    public MyListView(AppContext context) {
        super(context);
        this.context = context;
        PillButton browse = new PillButton("Browse movies", Icons.GRID, PillButton.Kind.PRIMARY);
        browse.setOnAction(() -> context.navigator().browse(null));
        setEmpty("Your list is empty",
                "Hover over any thumbnail and press + to save it here. Your list is remembered next time you open Binge.",
                browse);
        context.library().myList().addListener((ListChangeListener<String>) c -> refresh());
        refresh();
    }

    public void refresh() {
        List<Movie> movies = context.library().myList().stream()
                .map(context.catalog()::find)
                .flatMap(Optional::stream)
                .toList();
        int count = movies.size();
        setHeading("My List", count == 0 ? "Nothing saved yet"
                : count + (count == 1 ? " title saved" : " titles saved"));
        setMovies(movies);
    }
}
