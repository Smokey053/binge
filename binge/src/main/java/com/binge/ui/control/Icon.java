package com.binge.ui.control;

import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;

/** A scalable vector icon. The glyph is a CSS-coloured {@link Region} shaped by an SVG path, so
 its colour follows the {@code icon} style class and any parent override (see app.css).
 */
public final class Icon extends Region {

    private final SVGPath shape = new SVGPath();

    public Icon(String path, double size) {
        getStyleClass().add("icon");
        shape.setContent(path);
        setShape(shape);
        setScaleShape(true);
        setCenterShape(true);
        setMouseTransparent(true);
        resize(size);
    }

    public void setPath(String path) {
        shape.setContent(path);
        setShape(null);
        setShape(shape);
    }

    public void resize(double size) {
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
    }
}
