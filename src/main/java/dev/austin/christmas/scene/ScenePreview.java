package dev.austin.christmas.scene;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class ScenePreview {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--check")) {
            check();
            return;
        }
        if (args.length > 0 && args[0].equals("--turntable")) {
            exportTurntable();
            return;
        }
        if (args.length > 0 && args[0].equals("--snapshot")) {
            ChristmasScene scene = new ChristmasScene();
            for (int i = 0; i < 90; i++) scene.update(1.0 / 30, 1000, 720);
            BufferedImage image = new BufferedImage(1000, 720, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(new Color(5, 10, 18));
            g.fillRect(0, 0, 1000, 720);
            scene.paint(g, 1000, 720, 0.95f);
            g.dispose();
            try {
                javax.imageio.ImageIO.write(image, "png", new java.io.File("build/scene-preview.png"));
            } catch (java.io.IOException exception) { throw new RuntimeException(exception); }
            return;
        }
        SwingUtilities.invokeLater(() -> {
            ChristmasScene scene = new ChristmasScene();
            JPanel panel = new JPanel() {
                @Override protected void paintComponent(Graphics graphics) {
                    super.paintComponent(graphics);
                    scene.paint((Graphics2D) graphics, getWidth(), getHeight(), 0.95f);
                }
            };
            panel.setBackground(new Color(5, 10, 18));
            panel.setPreferredSize(new Dimension(1000, 720));
            JFrame frame = new JFrame("Christmas Code — animated ASCII tree");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            long[] last = {System.nanoTime()};
            Timer timer = new Timer(33, event -> {
                long now = System.nanoTime();
                if (frame.isActive()) {
                    scene.update((now - last[0]) / 1e9, panel.getWidth(), panel.getHeight());
                    panel.repaint();
                }
                last[0] = now;
            });
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosed(java.awt.event.WindowEvent event) { timer.stop(); }
            });
            frame.setVisible(true);
            timer.start();
        });
    }

    private static void exportTurntable() {
        java.io.File directory = new java.io.File("build/turntable");
        directory.mkdirs();
        AsciiTree tree = new AsciiTree();
        long[] timings = new long[96];
        for (int frame = 0; frame < timings.length; frame++) {
            BufferedImage image = new BufferedImage(720, 720, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(new Color(5, 10, 18));
            g.fillRect(0, 0, 720, 720);
            long start = System.nanoTime();
            tree.paint(g, 720, 720, frame / 3.0);
            timings[frame] = System.nanoTime() - start;
            g.dispose();
            try {
                javax.imageio.ImageIO.write(image, "png", new java.io.File(directory, String.format("%03d.png", frame)));
            } catch (java.io.IOException exception) { throw new RuntimeException(exception); }
        }
        Arrays.sort(timings);
        System.out.printf("720px tree render: median %.1f ms, p95 %.1f ms (including cold frames)%n",
                timings[48] / 1e6, timings[91] / 1e6);
    }

    private static void check() {
        ChristmasScene scene = new ChristmasScene();
        int[] before = render(scene);
        scene.update(0, 1000, 720); // Seed snow without advancing the tree.
        checkMask(before, render(scene), new AsciiTree().occlusion(1000, 720, 0));
        AsciiTree tree = new AsciiTree();
        SnowField rearSnow = new SnowField();
        rearSnow.update(0, 1000, 720);
        for (double time : new double[]{0, 4, 8, 16, 24}) {
            BufferedImage bare = new BufferedImage(1000, 720, BufferedImage.TYPE_INT_ARGB);
            Graphics2D plain = bare.createGraphics();
            tree.paint(plain, 1000, 720, time);
            plain.dispose();
            BufferedImage layered = new BufferedImage(1000, 720, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = layered.createGraphics();
            java.awt.geom.Area visible = new java.awt.geom.Area(new Rectangle(0, 0, 1000, 720));
            Shape mask = tree.occlusion(1000, 720, time);
            visible.subtract(new java.awt.geom.Area(mask));
            g.clip(visible);
            rearSnow.paint(g, 1000, 720);
            g.setClip(null);
            tree.paint(g, 1000, 720, time);
            g.dispose();
            checkMask(bare.getRGB(0,0,1000,720,null,0,1000), layered.getRGB(0,0,1000,720,null,0,1000), mask);
        }
        for (int i = 0; i < 90; i++) scene.update(1.0 / 30, 1000, 720);
        int[] after = render(scene);
        if (Arrays.equals(before, after)) throw new AssertionError("Animation did not change pixels");
        if (Arrays.stream(after).noneMatch(pixel -> (pixel >>> 24) > 0))
            throw new AssertionError("Scene is empty");
        if (Arrays.stream(after).noneMatch(pixel -> (pixel >>> 24) == 0)) throw new AssertionError("Scene unexpectedly fills the background");
        // Painting must be pure: IntelliJ may repaint several times per animation tick.
        if (!Arrays.equals(after, render(scene))) throw new AssertionError("Painting changed simulation state");
        SnowField snow = new SnowField();
        snow.update(0, 1000, 720);
        int[] snowBefore = renderSnow(snow, 1000, 720);
        for (int i = 0; i < 180; i++) snow.update(1.0 / 30, 1000, 720);
        if (Arrays.equals(snowBefore, renderSnow(snow, 1000, 720)))
            throw new AssertionError("Snow did not move");
        snow.update(0.03, 0, 0);
        for (int i = 0; i < 3000; i++) snow.update(0.05, 180, 120);
        if (Arrays.stream(renderSnow(snow, 180, 120)).noneMatch(pixel -> (pixel >>> 24) > 0))
            throw new AssertionError("Snow vanished after resize/recycling");
        for (int frame = 0; frame < 120; frame++) {
            snow.update(.05, 180, 120);
            int[] landed = renderSnow(snow, 180, 120);
            for (int y = 0; y < 120; y++) for (int x = 0; x < 180; x++)
                if (y > SnowGround.surface(x, 180, 120) + 2 && (landed[y * 180 + x] >>> 24) != 0)
                    throw new AssertionError("Snow fell through the ground");
        }
        System.out.println("Scene check passed: animation, transparency, pure painting, snow occlusion, snow movement, ground collision, resize/recycling.");
    }

    private static void checkMask(int[] before, int[] withSnow, Shape silhouette) {
        boolean outsideChanged = false;
        for (int y = 0; y < 720; y++) for (int x = 0; x < 1000; x++) {
            int index = y * 1000 + x;
            if (silhouette.contains(x - 1, y - 1, 3, 3) && before[index] != withSnow[index])
                throw new AssertionError("Snow leaked through a rotating object");
            if (!silhouette.contains(x, y) && before[index] != withSnow[index]) outsideChanged = true;
        }
        if (!outsideChanged) throw new AssertionError("Snow is missing outside the silhouettes");
    }

    private static int[] renderSnow(SnowField snow, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        snow.paint(g, width, height);
        g.dispose();
        return image.getRGB(0, 0, width, height, null, 0, width);
    }

    private static int[] render(ChristmasScene scene) {
        BufferedImage image = new BufferedImage(1000, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        scene.paint(g, 1000, 720, 0.3f);
        g.dispose();
        return image.getRGB(0, 0, 1000, 720, null, 0, 1000);
    }
}
