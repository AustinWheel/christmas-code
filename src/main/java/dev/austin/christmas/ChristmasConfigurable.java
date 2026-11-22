package dev.austin.christmas;

import com.intellij.openapi.options.Configurable;
import javax.swing.*;
import java.awt.*;

/** Settings → Tools → Christmas Code. Changes are picked up by the animation timer. */
public final class ChristmasConfigurable implements Configurable {
    private JPanel panel;
    private JCheckBox shared;
    private JCheckBox autoEnable;
    private JSpinner opacity;

    @Override public String getDisplayName() { return "Christmas Code"; }

    @Override public JComponent createComponent() {
        panel = new JPanel(new BorderLayout());
        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        autoEnable = new JCheckBox("Enable automatically when opening a project");
        autoEnable.setToolTipText("Applies the next time a project opens. The Tools toggle still controls the current session.");
        autoEnable.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(autoEnable);
        fields.add(Box.createVerticalStrut(8));
        shared = new JCheckBox("Share one scene across split editors");
        shared.setToolTipText("When unchecked, each editor pane has its own tree and snow.");
        shared.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(shared);
        fields.add(Box.createVerticalStrut(12));
        JPanel opacityRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        opacityRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel label = new JLabel("Background opacity: ");
        opacity = new JSpinner(new SpinnerNumberModel(25, 0, 100, 1));
        label.setLabelFor(opacity);
        opacityRow.add(label);
        opacityRow.add(opacity);
        opacityRow.add(new JLabel(" %  (0 = invisible, 100 = fully opaque)"));
        fields.add(opacityRow);
        fields.add(Box.createVerticalStrut(12));
        JLabel help = new JLabel("Applies to all projects. Enable the background from the Tools menu.");
        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(help);
        panel.add(fields, BorderLayout.NORTH);
        reset();
        return panel;
    }

    @Override public boolean isModified() {
        if (panel == null) return false;
        var saved = ChristmasSettings.getInstance().getState();
        return autoEnable.isSelected() != saved.enableOnProjectOpen
                || shared.isSelected() != saved.sharedAcrossSplits
                || ((Number) opacity.getValue()).intValue() != saved.opacityPercent;
    }

    @Override public void apply() throws com.intellij.openapi.options.ConfigurationException {
        if (panel == null) return;
        try {
            opacity.commitEdit();
        } catch (java.text.ParseException e) {
            throw new com.intellij.openapi.options.ConfigurationException("Enter an opacity from 0 to 100.");
        }
        int value = ((Number) opacity.getValue()).intValue();
        if (value < 0 || value > 100)
            throw new com.intellij.openapi.options.ConfigurationException("Enter an opacity from 0 to 100.");
        ChristmasSettings.getInstance().update(shared.isSelected(), value, autoEnable.isSelected());
    }

    @Override public void reset() {
        if (panel == null) return;
        var saved = ChristmasSettings.getInstance().getState();
        shared.setSelected(saved.sharedAcrossSplits);
        autoEnable.setSelected(saved.enableOnProjectOpen);
        opacity.setValue(saved.opacityPercent);
    }

    @Override public void disposeUIResources() {
        panel = null;
        shared = null;
        autoEnable = null;
        opacity = null;
    }
}
