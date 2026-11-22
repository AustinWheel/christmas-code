package dev.austin.christmas;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChristmasSettingsTest {
    @Test public void firstInstallIsOptInWithSubtleSharedScene() {
        var state = new ChristmasSettings().getState();
        assertFalse(state.enableOnProjectOpen);
        assertTrue(state.sharedAcrossSplits);
        assertEquals(25, state.opacityPercent);
    }

    @Test public void restoresAllPreferencesWithoutKeepingMutableInput() {
        var saved = new ChristmasSettings.Preferences();
        saved.sharedAcrossSplits = false;
        saved.opacityPercent = 42;
        saved.enableOnProjectOpen = true;
        var settings = new ChristmasSettings();
        settings.loadState(saved);
        saved.opacityPercent = 0;
        assertFalse(settings.getState().sharedAcrossSplits);
        assertTrue(settings.getState().enableOnProjectOpen);
        assertEquals(42, settings.getState().opacityPercent);
    }

    @Test public void invalidStoredOpacityIsClampedToPreventPaintingExceptions() {
        var settings = new ChristmasSettings();
        var saved = new ChristmasSettings.Preferences();
        saved.opacityPercent = -30;
        settings.loadState(saved);
        assertEquals(0, settings.getState().opacityPercent);
        saved.opacityPercent = 300;
        settings.loadState(saved);
        assertEquals(100, settings.getState().opacityPercent);
    }

    @Test public void applyingAppearanceDoesNotLoseStartupPreference() {
        var settings = new ChristmasSettings();
        settings.update(false, 0, true);
        assertTrue(settings.getState().enableOnProjectOpen);
        assertFalse(settings.getState().sharedAcrossSplits);
        assertEquals(0, settings.getState().opacityPercent);
        settings.update(true, 100, false);
        assertFalse(settings.getState().enableOnProjectOpen);
        assertTrue(settings.getState().sharedAcrossSplits);
        assertEquals(100, settings.getState().opacityPercent);
    }
}
