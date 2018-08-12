package com.boydti.fawe.installer;

import java.awt.Color;
import java.awt.event.ActionEvent;

public class CloseButton extends InteractiveButton {
    public CloseButton() {
        super("X");
        setColor(new Color(0x66, 0x33, 0x33));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.exit(0);
    }
}
