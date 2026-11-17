package com.binge.ui.view;

import com.binge.model.Movie;
import com.binge.ui.AppContext;
import com.binge.ui.Icons;
import com.binge.ui.component.PosterGrid;
import com.binge.ui.control.Icon;
import com.binge.ui.control.PageScroll;
import com.binge.ui.control.PillButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Shared scaffold of the grid pages (Browse, My List, Search results): a heading, an optional
 * filter bar, the responsive {@link PosterGrid} and an empty-state message.
 */
public abstract class GalleryView extends StackPane {

    private final Label heading = new Label();
    private final Label detail = new Label();
    private final VBox filterHolder = new VBox();
    private final PosterGrid grid;
    private final VBox emptyState = new VBox(10);
    private final Label emptyTitle = new Label();
    private final Label emptyMessage = new Label();
    private final VBox emptyAction = new VBox();
    private final PageScroll scroll;

    protected GalleryView(AppContext context) {
        getStyleClass().add("gallery-view");
        heading.getStyleClass().add("page-heading");
        detail.getStyleClass().add("page-detail");
        VBox header = new VBox(5, heading, detail, filterHolder);
        header.setPadding(new Insets(106, 44, 6, 44));
        filterHolder.setPadding(new Insets(14, 0, 0, 0));

        grid = new PosterGrid(context);
        VBox gridBox = new VBox(grid);
        gridBox.setPadding(new Insets(0, 24, 40, 24));

        Icon icon = new Icon(Icons.FILM, 44);
        icon.getStyleClass().add("empty-icon");
        emptyTitle.getStyleClass().add("empty-title");
        emptyMessage.getStyleClass().add("empty-message");
        emptyMessage.setWrapText(true);
        emptyMessage.setMaxWidth(420);
        emptyMessage.setAlignment(Pos.CENTER);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(70, 24, 90, 24));
        emptyState.getChildren().addAll(icon, emptyTitle, emptyMessage, emptyAction);
        emptyAction.setAlignment(Pos.CENTER);

        scroll = new PageScroll(new VBox(header, gridBox, emptyState));
        getChildren().add(scroll);
        showEmpty(false);
    }

    protected void setHeading(String title, String detail) {
        heading.setText(title);
        this.detail.setText(detail == null ? "" : detail);
        this.detail.setVisible(detail != null && !detail.isBlank());
        this.detail.setManaged(detail != null && !detail.isBlank());
    }

    protected void setFilters(Node filters) {
        filterHolder.getChildren().setAll(filters);
    }

    protected void setMovies(List<Movie> movies) {
        grid.setMovies(movies);
        showEmpty(movies.isEmpty());
    }

    protected void setEmpty(String title, String message, PillButton action) {
        emptyTitle.setText(title);
        emptyMessage.setText(message);
        emptyAction.getChildren().setAll(action == null ? List.of() : List.of(action));
    }

    protected int gridSize() {
        return grid.size();
    }

    public void scrollToTop() {
        scroll.scrollToTop();
    }

    private void showEmpty(boolean empty) {
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
    }
}
