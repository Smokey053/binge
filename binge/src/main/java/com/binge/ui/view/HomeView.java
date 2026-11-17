package com.binge.ui.view;

import com.binge.model.Category;
import com.binge.model.Movie;
import com.binge.ui.AppContext;
import com.binge.ui.component.HeroBanner;
import com.binge.ui.component.MovieRow;
import com.binge.ui.control.PageScroll;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * The landing page: the featured hero banner, the viewer's own shelves (recently watched and
 * My List when they exist) and one labelled row per genre category.
 */
public final class HomeView extends StackPane {

    private final AppContext context;
    private final HeroBanner hero;
    private final VBox personalRows = new VBox(6);
    private final VBox categoryRows = new VBox(6);
    private final PageScroll scroll;
    private boolean entrancePlayed;

    public HomeView(AppContext context) {
        this.context = context;
        getStyleClass().add("home-view");

        hero = new HeroBanner(context.catalog().featuredMovies(), context);

        for (Category category : context.catalog().categories()) {
            categoryRows.getChildren().add(new MovieRow(
                    category.name(), category.movies(), context,
                    () -> context.navigator().browse(category.id())));
        }

        VBox rows = new VBox(8, personalRows, categoryRows);
        rows.setPadding(new Insets(14, 24, 0, 24));
        VBox page = new VBox(hero, rows);
        page.setPadding(new Insets(0, 0, 34, 0));
        scroll = new PageScroll(page);
        scroll.viewportBoundsProperty().addListener((o, was, bounds) -> hero.setViewportHeight(bounds.getHeight()));
        getChildren().add(scroll);

        context.library().myList().addListener((ListChangeListener<String>) c -> rebuildPersonalRows());
        context.library().recent().addListener((ListChangeListener<String>) c -> rebuildPersonalRows());
        rebuildPersonalRows();
    }

    /** Called whenever the page becomes the active one. */
    public void onShown() {
        hero.resume();
        if (!entrancePlayed) {
            entrancePlayed = true;
            double delay = 120;
            for (var node : categoryRows.getChildren()) {
                ((MovieRow) node).playEntrance(delay);
                delay += 110;
            }
        }
    }

    public void onHidden() {
        hero.pause();
    }

    public int categoryRowCount() {
        return categoryRows.getChildren().size();
    }

    private void rebuildPersonalRows() {
        personalRows.getChildren().clear();
        addPersonalRow("Recently Watched", context.library().recent());
        addPersonalRow("My List", context.library().myList());
    }

    private void addPersonalRow(String title, List<String> ids) {
        List<Movie> movies = ids.stream().map(context.catalog()::find).flatMap(java.util.Optional::stream).toList();
        if (!movies.isEmpty()) {
            personalRows.getChildren().add(new MovieRow(title, movies, context, null));
        }
    }
}
