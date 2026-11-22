package dev.austin.christmas.scene;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Persistent particles, advanced in seconds and recycled at viewport edges. */
public final class SnowField {
    private static final class Flake {
        double x, y, speed, phase, resting;
        boolean landed;
        int size, alpha, shape;
    }
    private static final BufferedImage[][] SPRITES = createSprites();

    private static BufferedImage[][] createSprites() {
        BufferedImage[][] sprites = new BufferedImage[3][3];
        for (int size = 1; size <= 3; size++) for (int shape = 0; shape < 3; shape++) {
            BufferedImage image = new BufferedImage(17, 17, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int pass = 0; pass < 3; pass++) {
                g.setColor(pass == 0 ? new Color(190, 216, 245, 25)
                        : pass == 1 ? new Color(215, 233, 255, 75) : new Color(245, 250, 255, 245));
                g.setStroke(new BasicStroke(pass == 0 ? 4f : pass == 1 ? 2.4f : 1.1f,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (size == 1) {
                    g.drawLine(8,8,8,8);
                } else {
                    int r = size;
                    g.drawLine(8-r,8,8+r,8);
                    g.drawLine(8,8-r,8,8+r);
                    if (shape == 1) {
                        g.drawLine(8-r+1,8-r+1,8+r-1,8+r-1);
                        g.drawLine(8-r+1,8+r-1,8+r-1,8-r+1);
                    }
                }
            }
            g.dispose();
            sprites[size-1][shape] = image;
        }
        return sprites;
    }

    private final Random random = new Random(1225);
    private final List<Flake> flakes = new ArrayList<>();
    private double time;
    private int previousWidth, previousHeight;

    public void update(double dt, int width, int height) {
        if (width <= 0 || height <= 0) return;
        dt = Math.max(0, Math.min(dt, 0.05));
        time += dt;
        if (previousWidth > 0 && previousHeight > 0 && (width != previousWidth || height != previousHeight)) {
            for (Flake f : flakes) {
                f.x *= width / (double) previousWidth;
                f.y *= height / (double) previousHeight;
            }
        }
        previousWidth = width; previousHeight = height;
        int target = Math.max(35, Math.min(500, (int) ((long) width * height / 2100)));
        while (flakes.size() > target) flakes.remove(flakes.size() - 1);
        while (flakes.size() < target) {
            Flake f = new Flake();
            f.x = random.nextDouble() * width;
            f.y = random.nextDouble() * Math.max(0, SnowGround.surface(f.x, width, height) - 6);
            double depth = random.nextDouble();
            f.speed = 13 + depth * 43;
            f.size = depth > 0.70 ? 3 : depth > 0.25 ? 2 : 1;
            f.alpha = (int) (155 + depth * 100);
            f.shape = random.nextInt(3);
            f.phase = random.nextDouble() * Math.PI * 2;
            flakes.add(f);
        }
        for (Flake f : flakes) {
            double landing = SnowGround.surface(f.x, width, height) - f.size - 3;
            if (f.landed) {
                f.y = landing;
                f.resting += dt;
                if (f.resting >= .8) {
                    f.landed = false;
                    f.resting = 0;
                    f.y = -f.size - 4;
                    f.x = random.nextDouble() * width;
                }
                continue;
            }
            f.y += f.speed * dt;
            f.x += (3 + Math.sin(time * 0.65 + f.phase) * 9) * dt;
            if (f.x > width + f.size) f.x = -f.size;
            if (f.x < -f.size) f.x = width + f.size;
            landing = SnowGround.surface(f.x, width, height) - f.size - 3;
            if (f.y >= landing) {
                f.y = landing;
                f.landed = true;
                f.resting = 0;
            }
        }
    }

    public void paint(Graphics2D graphics, int width, int height) {
        Graphics2D g = (Graphics2D) graphics.create();
        float sceneAlpha = g.getComposite() instanceof AlphaComposite composite ? composite.getAlpha() : 1;
        try {
            for (Flake f : flakes) {
                float alpha = (float) (f.alpha / 255.0 * (f.landed ? Math.max(0, 1 - f.resting / .8) : 1));
                g.setComposite(AlphaComposite.SrcOver.derive(sceneAlpha * alpha));
                int x = (int) Math.round(f.x), y = (int) Math.round(f.y);
                g.drawImage(SPRITES[f.size-1][f.shape], x-8, y-8, null);
            }
        } finally { g.dispose(); }
    }
}
