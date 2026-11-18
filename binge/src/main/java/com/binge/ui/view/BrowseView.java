package com.binge.ui.view;

import com.binge.model.Category;
import com.binge.model.Movie;
import com.binge.ui.AppContext;
import com.binge.ui.control.Chip;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All movies in one grid, filtered by genre category and sortable. */
public final class BrowseView extends GalleryView {

    /** Sort orders offered next to the category chips. */
    private enum Sort {
        DEFAULT("Default", null),
        TITLE("A – Z", Comparator.comparing(Movie::title, String.CASE_INSENSITIVE_ORDER)),
        NEWEST("Newest", Comparator.comparingInt(Movie::year).reversed().thenComparing(Movie::title)),
        OLDEST("Oldest", Comparator.comparingInt(Movie::year).thenComparing(Movie::title));

        private final String label;
        private final Comparator<Movie> comparator;

        Sort(String label, Comparator<Movie> comparator) {
            this.label = label;
            this.comparator = comparator;
        }
    }

    private final AppContext context;
    private final Map<String, Chip> categoryChips = new LinkedHashMap<>();
    private final Map<Sort, Chip> sortChips = new LinkedHashMap<>();
    private String categoryId;
    private Sort sort = Sort.DEFAULT;

    public BrowseView(AppContext context) {
        super(context);
        this.context = context;
        setFilters(buildFilters());
        refresh();
    }

    /** Shows one category, or everything when {@code id} is {@code null}. */
    public void showCategory(String id) {
        this.categoryId = id;
        refresh();
        scrollToTop();
    }

    public int visibleCount() {
        return gridSize();
    }

    private VBox buildFilters() {
        FlowPane genres = new FlowPane(8, 8);
        categoryChips.put(null, chip("All", genres, () -> showCategory(null)));
        for (Category category : context.catalog().categories()) {
            categoryChips.put(category.id(), chip(category.name(), genres, () -> showCategory(category.id())));
        }

        FlowPane sorting = new FlowPane(8, 8);
        Label sortLabel = new Label("Sort by");
        sortLabel.getStyleClass().add("filter-label");
        HBox sortRow = new HBox(12, sortLabel, sorting);
        sortRow.setAlignment(Pos.CENTER_LEFT);
        for (Sort option : Sort.values()) {
            sortChips.put(option, chip(option.label, sorting, () -> {
                sort = option;
                refresh();
            }));
        }
        return new VBox(12, genres, sortRow);
    }

    private Chip chip(String text, FlowPane parent, Runnable action) {
        Chip chip = new Chip(text);
        chip.setOnAction(action);
        parent.getChildren().add(chip);
        return chip;
    }

    private void refresh() {
        List<Movie> movies = new ArrayList<>(categoryId == null
                ? context.catalog().allMovies()
                : context.catalog().category(categoryId).map(Category::movies).orElse(List.of()));
        if (sort.comparator != null) {
            movies.sort(sort.comparator);
        }
        String name = categoryId == null ? "Browse"
                : context.catalog().category(categoryId).map(Category::name).orElse("Browse");
        String detail = categoryId == null
                ? "All " + movies.size() + " titles across " + context.catalog().categories().size() + " genres"
                : context.catalog().category(categoryId).map(Category::tagline).orElse("");
        setHeading(name, detail);
        categoryChips.forEach((id, chip) -> chip.setSelected(java.util.Objects.equals(id, categoryId)));
        sortChips.forEach((option, chip) -> chip.setSelected(option == sort));
        setEmpty("Nothing here yet", "There are no movies in this category.", null);
        setMovies(movies);
    }
}
