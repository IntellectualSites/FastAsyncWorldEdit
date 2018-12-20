package com.boydti.fawe.installer;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import javax.swing.JPanel;

public class InvisiblePanel extends JPanel {
    public InvisiblePanel(LayoutManager layout) {
        super(layout);
        setBackground(new Color(0, 0, 0, 0));
    }

    public InvisiblePanel() {
        this(new FlowLayout());
    }
}
