package dev.austin.christmas;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

/** Opt-in startup; keep Swing and editor mutations on the event dispatch thread. */
public final class ChristmasStartupActivity implements StartupActivity.DumbAware {
    @Override public void runActivity(Project project) {
        if (!ChristmasSettings.getInstance().getState().enableOnProjectOpen) return;
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed() && ChristmasSettings.getInstance().getState().enableOnProjectOpen)
                project.getService(ChristmasBackgroundService.class).setEnabled(true);
        }, project.getDisposed());
    }
}
