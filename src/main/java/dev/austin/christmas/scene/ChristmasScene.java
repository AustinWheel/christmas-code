package dev.austin.christmas.scene;

import java.awt.*;
import java.awt.geom.Area;

/** IDE-independent animation: the same code runs in the preview and plugin. */
public final class ChristmasScene {
    private final AsciiTree tree = new AsciiTree();
    private final SnowField snow = new SnowField();
    private double time;

    public void update(double dt, int width, int height) {
        // Avoid jumping when a window is hidden or a debugger stops execution.
        dt = Math.max(0, Math.min(dt, 0.05));
        time += dt;
        snow.update(dt, width, height);
    }

    public void paint(Graphics2D graphics, int width, int height, float opacity) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setComposite(AlphaComposite.SrcOver.derive(opacity));
            Graphics2D background = (Graphics2D) g.create();
            try {
                Area visibleSnow = new Area(new Rectangle(0, 0, width, height));
                visibleSnow.subtract(tree.occlusion(width, height, time));
                background.clip(visibleSnow);
                snow.paint(background, width, height);
            } finally { background.dispose(); }
            tree.paint(g, width, height, time);
        } finally {
            g.dispose();
        }
    }
}
