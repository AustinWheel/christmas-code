package dev.austin.christmas;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer;
import com.intellij.openapi.editor.markup.CustomHighlighterOrder;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.Disposer;
import dev.austin.christmas.scene.ChristmasScene;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** All animation and painting happens on Swing's event dispatch thread. */
@Service(Service.Level.PROJECT)
public final class ChristmasBackgroundService implements Disposable {
    private static final Logger LOG = Logger.getInstance(ChristmasBackgroundService.class);
    private final Project project;
    private final Map<Editor, Background> backgrounds = new HashMap<>();
    private final Map<Component, SharedScene> scenes = new HashMap<>();
    private final Timer timer;
    private boolean enabled;
    private long lastTick;

    public ChristmasBackgroundService(Project project) {
        this.project = project;
        timer = new Timer(33, event -> tick());
        timer.setCoalesce(true);
        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
            @Override public void editorCreated(EditorFactoryEvent event) {
                if (enabled) attach(event.getEditor());
            }
            @Override public void editorReleased(EditorFactoryEvent event) {
                Background background = backgrounds.remove(event.getEditor());
                if (background != null) Disposer.dispose(background);
            }
        }, this);
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        LOG.info("Christmas background enabled=" + enabled);
        if (enabled) {
            for (Editor editor : EditorFactory.getInstance().getAllEditors()) attach(editor);
            LOG.info("Christmas background attached editor count=" + backgrounds.size());
            lastTick = System.nanoTime();
            timer.start();
        } else {
            timer.stop();
            clear();
        }
    }

    private void attach(Editor editor) {
        if (editor.getProject() != project || editor.isViewer()
                || editor.getEditorKind() != EditorKind.MAIN_EDITOR || backgrounds.containsKey(editor)) return;
        Background background = new Background(editor);
        Disposer.register(this, background);
        backgrounds.put(editor, background);
        LOG.info("Christmas background attached to " + editor.getEditorKind());
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = (now - lastTick) / 1e9;
        lastTick = now;
        var preferences = ChristmasSettings.getInstance().getState();
        float opacity = preferences.opacityPercent / 100f;
        Map<Component, Rectangle> visibleAreas = new HashMap<>();
        for (Background background : backgrounds.values()) {
            background.shared = null;
            Window window = SwingUtilities.getWindowAncestor(background.component);
            Rectangle viewport = background.component.getVisibleRect();
            if (!background.component.isShowing() || window == null || viewport.isEmpty()) continue;
            Rectangle area = SwingUtilities.convertRectangle(background.component, viewport, window);
            Component key = preferences.sharedAcrossSplits ? window : background.component;
            visibleAreas.merge(key, area, (left, right) -> left.union(right));
        }
        // A detached editor window gets its own scene; splits inside a window share one.
        scenes.entrySet().removeIf(entry -> {
            if (visibleAreas.containsKey(entry.getKey())) return false;
            entry.getValue().dispose();
            return true;
        });
        for (var entry : visibleAreas.entrySet()) {
            Component key = entry.getKey();
            Window window = key instanceof Window w ? w : SwingUtilities.getWindowAncestor(key);
            SharedScene shared = scenes.compute(key, (ignored, existing) -> {
                if (existing != null && existing.window == window) return existing;
                if (existing != null) existing.dispose();
                return new SharedScene(window);
            });
            Rectangle bounds = entry.getValue();
            if (window.isActive() || shared.image == null || !bounds.equals(shared.bounds) || shared.opacity != opacity)
                shared.frame(window.isActive() ? dt : 0, bounds, opacity);
        }
        for (Background background : backgrounds.values()) {
            if (!background.component.isShowing()) continue;
            Window window = SwingUtilities.getWindowAncestor(background.component);
            background.shared = scenes.get(preferences.sharedAcrossSplits ? window : background.component);
            if (background.shared != null && (window.isActive() || background.shared.rendered))
                background.component.repaint(background.component.getVisibleRect());
        }
        scenes.values().forEach(scene -> scene.rendered = false);
    }

    private void clear() {
        for (Background background : backgrounds.values()) Disposer.dispose(background);
        backgrounds.clear();
        scenes.values().forEach(SharedScene::dispose);
        scenes.clear();
    }

    @Override public void dispose() {
        timer.stop();
        // Child backgrounds and the listener are removed by Disposer.
        backgrounds.clear();
        scenes.values().forEach(SharedScene::dispose);
        scenes.clear();
    }

    private static final class Background implements Disposable {
        private final JComponent component;
        private final RangeHighlighter highlighter;
        private SharedScene shared;
        private boolean painted;

        Background(Editor editor) {
            component = editor.getContentComponent();
            // Register directly in the editor's paint pipeline. A background-fill transform
            // is not sufficient when the content component delegates its fill to a parent.
            highlighter = editor.getMarkupModel().addRangeHighlighter(0, editor.getDocument().getTextLength(),
                    HighlighterLayer.FIRST, null, HighlighterTargetArea.EXACT_RANGE);
            highlighter.setGreedyToLeft(true);
            highlighter.setGreedyToRight(true);
            highlighter.setCustomRenderer(new CustomHighlighterRenderer() {
                @Override public CustomHighlighterOrder getOrder() {
                    // After the base fill, before selection backgrounds and the code itself.
                    return CustomHighlighterOrder.BEFORE_BACKGROUND;
                }

                @Override public void paint(Editor target, RangeHighlighter range, Graphics graphics) {
                    SharedScene canvas = shared;
                    if (canvas == null || canvas.image == null) return;
                    if (!painted) {
                        painted = true;
                        LOG.info("Christmas background painter reached the editor");
                    }
                    Rectangle viewport = component.getVisibleRect();
                    Point origin = SwingUtilities.convertPoint(canvas.window,
                            canvas.bounds.x, canvas.bounds.y, component);
                    Graphics copy = graphics.create();
                    try {
                        copy.clipRect(viewport.x, viewport.y, viewport.width, viewport.height);
                        copy.drawImage(canvas.image, origin.x, origin.y, null);
                    } finally {
                        copy.dispose();
                    }
                }
            });
        }

        @Override public void dispose() {
            highlighter.dispose();
            shared = null;
            component.repaint();
        }
    }

    /** A cached scene per window or pane, always positioned in window coordinates. */
    private static final class SharedScene {
        private final Window window;
        private final ChristmasScene scene = new ChristmasScene();
        private Rectangle bounds = new Rectangle();
        private BufferedImage image;
        private float opacity = -1;
        private boolean rendered;

        SharedScene(Window window) {
            this.window = window;
        }

        void frame(double dt, Rectangle area, float opacity) {
            this.opacity = opacity;
            rendered = true;
            bounds = new Rectangle(area);
            if (image == null || image.getWidth() != area.width || image.getHeight() != area.height) {
                if (image != null) image.flush();
                image = new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_ARGB);
                LOG.info("Christmas shared background frame size=" + area.width + "x" + area.height);
            }
            scene.update(dt, area.width, area.height);
            Graphics2D g = image.createGraphics();
            try {
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 0, image.getWidth(), image.getHeight());
                g.setComposite(AlphaComposite.SrcOver);
                scene.paint(g, image.getWidth(), image.getHeight(), opacity);
            } finally {
                g.dispose();
            }
        }

        void dispose() {
            if (image != null) image.flush();
            image = null;
        }
    }
}
