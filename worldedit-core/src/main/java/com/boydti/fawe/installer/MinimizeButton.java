package com.boydti.fawe.installer;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;

public class MinimizeButton extends InteractiveButton {
    private final JFrame window;

    public MinimizeButton(JFrame window) {
        super("-");
        this.window = window;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        window.setState(Frame.ICONIFIED);
    }
}
