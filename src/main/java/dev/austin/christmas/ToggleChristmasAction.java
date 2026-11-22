package dev.austin.christmas;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;

public final class ToggleChristmasAction extends ToggleAction implements DumbAware {
    @Override public ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.EDT; }

    @Override public boolean isSelected(AnActionEvent event) {
        return event.getProject() != null
                && event.getProject().getService(ChristmasBackgroundService.class).isEnabled();
    }

    @Override public void setSelected(AnActionEvent event, boolean selected) {
        if (event.getProject() != null)
            event.getProject().getService(ChristmasBackgroundService.class).setEnabled(selected);
    }

    @Override public void update(AnActionEvent event) {
        super.update(event);
        event.getPresentation().setEnabled(event.getProject() != null);
    }
}
