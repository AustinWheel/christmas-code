package dev.austin.christmas;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

/** Application-wide preferences, saved independently of the per-project on/off toggle. */
@Service(Service.Level.APP)
@State(name = "ChristmasCodeSettings", storages = @Storage("christmas-code.xml"))
public final class ChristmasSettings implements PersistentStateComponent<ChristmasSettings.Preferences> {
    public static final class Preferences {
        public boolean sharedAcrossSplits = true;
        public boolean enableOnProjectOpen = false;
        public int opacityPercent = 25;
    }

    private volatile Preferences preferences = new Preferences();

    public static ChristmasSettings getInstance() {
        return ApplicationManager.getApplication().getService(ChristmasSettings.class);
    }

    @Override public Preferences getState() { return preferences; }

    @Override public void loadState(Preferences state) {
        update(state.sharedAcrossSplits, state.opacityPercent, state.enableOnProjectOpen);
    }

    public void update(boolean sharedAcrossSplits, int opacityPercent, boolean enableOnProjectOpen) {
        Preferences next = new Preferences();
        next.sharedAcrossSplits = sharedAcrossSplits;
        next.enableOnProjectOpen = enableOnProjectOpen;
        next.opacityPercent = Math.max(0, Math.min(100, opacityPercent));
        preferences = next;
    }
}
