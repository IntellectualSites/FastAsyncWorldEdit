package com.boydti.fawe.installer;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class URLButton extends InteractiveButton {
    private final URL url;

    public URLButton(URL url, String text) {
        super("<HTML>" + text + "</HTML>");
        this.url = url;
        setFont(new Font(getFont().getName(), Font.PLAIN, 9));
        setForeground(new Color(0x77, 0x77, 0x77));
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(url.toURI());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return;
        }
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Clipboard systemClipboard = defaultToolkit.getSystemClipboard();
        systemClipboard.setContents(new StringSelection(url.toString()), null);
    }
}
