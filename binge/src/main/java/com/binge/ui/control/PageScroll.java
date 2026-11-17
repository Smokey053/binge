package com.binge.ui.control;

import com.binge.util.Fmt;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;

/**
 * Vertical page scroller used by every view: content always fits the width, there is no horizontal
 * bar, and the mouse wheel moves a comfortable distance regardless of the page height.
 */
public final class PageScroll extends ScrollPane {

    private static final double WHEEL_SPEED = 3.0;

    public PageScroll(Node content) {
        super(content);
        getStyleClass().add("page-scroll");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setPannable(false);

        addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() == 0 || Math.abs(e.getDeltaX()) > Math.abs(e.getDeltaY())) {
                return;
            }
            double overflow = content.getLayoutBounds().getHeight() - getViewportBounds().getHeight();
            if (overflow > 0) {
                setVvalue(Fmt.clamp(getVvalue() - e.getDeltaY() * WHEEL_SPEED / overflow, 0, 1));
                e.consume();
            }
        });
    }

    public void scrollToTop() {
        setVvalue(0);
    }

    /** The vertical scroll bar, exposed so views can react to it if they ever need to. */
    public ScrollBar verticalBar() {
        return (ScrollBar) lookup(".scroll-bar:vertical");
    }
}
