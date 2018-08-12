package com.boydti.fawe.installer;

import com.boydti.fawe.Fawe;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.DirectoryChooser;

public abstract class BrowseButton extends InteractiveButton {
    private final String id;

    public BrowseButton(String id) {
        super("Browse");
        this.id = id;
    }

    public abstract void onSelect(File folder);

    @Override
    public void actionPerformed(ActionEvent e) {
        Preferences prefs = Preferences.userRoot().node(Fawe.class.getName());
        String lastUsed = prefs.get("LAST_USED_FOLDER", null);
        final File lastFile = lastUsed == null ? null : new File(lastUsed).getParentFile();
        browse(lastFile);
    }

    public void browse(File from) {
        DirectoryChooser folderChooser = new DirectoryChooser();
        folderChooser.setInitialDirectory(from);

        new JFXPanel(); // Init JFX Platform
        Platform.runLater(() -> {
            File file = folderChooser.showDialog(null);
            if (file != null && file.exists()) {
                File parent = file.getParentFile();
                if (parent == null) parent = file;
                Preferences.userRoot().node(Fawe.class.getName()).put("LAST_USED_FOLDER" + id, parent.getPath());
                onSelect(file);
            }
        });
    }
}
