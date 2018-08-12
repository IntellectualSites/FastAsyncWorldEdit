package com.boydti.fawe.installer;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JButton;

public class InteractiveButton extends JButton implements ActionListener, MouseListener {
    private Color background;

    public InteractiveButton(String text) {
        this(text, new Color(0, 0, 0, 0));
    }

    public InteractiveButton(String text, Color background) {
        setText(text);
        setBorderPainted(false);
        setVisible(true);
        setForeground(new Color(200, 200, 200));
        addActionListener(this);
        addMouseListener(this);
        setFocusable(false);
        if (background.getAlpha() != 0) {
            this.background = background;
        } else {
            this.background = new Color(0x38, 0x38, 0x39);
        }
        setBackground(this.background);
    }

    public void setColor(Color background) {
        setBackground(background);
        this.background = background;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        setBackground(new Color(0x44, 0x44, 0x44));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        setBackground(this.background);
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        setBackground(new Color(0x77, 0x77, 0x77));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        setBackground(new Color(0x33, 0x33, 0x36));
    }
}
