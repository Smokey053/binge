# Icon sources

`src/main/resources/com/binge/icons/*.png` are the images the app draws. They are generated from the
SVG files in this folder (Material icons, Apache 2.0) by `tools/render_icons.py`.

To change an icon, edit or replace its SVG (or drop in your own PNG with the same name) and keep it
**white on a transparent background, 96x96**. The app tints icons at runtime to the colour set by
`-fx-icon-color` in `app.css`, so a coloured image would be recoloured to the theme colour anyway.

To add an icon: add `name.svg` here, run `python3 tools/render_icons.py`, and add a constant
`NAME = "name"` to `Icons.java`.
