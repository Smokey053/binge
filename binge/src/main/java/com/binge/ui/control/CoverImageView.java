package com.binge.ui.control;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.geometry.Rectangle2D;
import javafx.scene.shape.Rectangle;

/**
 * Shows an image scaled to <em>cover</em> the available area (like CSS {@code object-fit: cover}),
 * cropping the overflow instead of stretching. The crop is anchored with {@code focusY} so faces
 * and titles in portrait posters stay visible.
 */
public final class CoverImageView extends Region {

    private final ImageView view = new ImageView();
    private double focusX = 0.5;
    private double focusY = 0.35;

    public CoverImageView() {
        view.setSmooth(true);
        view.setPreserveRatio(false);
        view.setMouseTransparent(true);
        getChildren().add(view);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
    }

    public void setImage(Image image) {
        view.setImage(image);
        if (image != null && image.getProgress() < 1) {
            image.progressProperty().addListener((o, was, now) -> {
                if (now.doubleValue() >= 1) {
                    requestLayout();
                }
            });
        }
        requestLayout();
    }

    public Image getImage() {
        return view.getImage();
    }

    public void setFocus(double x, double y) {
        this.focusX = x;
        this.focusY = y;
        requestLayout();
    }

    public ImageView imageView() {
        return view;
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        Image image = view.getImage();
        view.setFitWidth(w);
        view.setFitHeight(h);
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0 || w <= 0 || h <= 0) {
            view.setViewport(null);
            return;
        }
        double scale = Math.max(w / image.getWidth(), h / image.getHeight());
        double viewW = w / scale;
        double viewH = h / scale;
        double x = (image.getWidth() - viewW) * focusX;
        double y = (image.getHeight() - viewH) * focusY;
        view.setViewport(new Rectangle2D(x, y, viewW, viewH));
        view.relocate(0, 0);
    }
}
