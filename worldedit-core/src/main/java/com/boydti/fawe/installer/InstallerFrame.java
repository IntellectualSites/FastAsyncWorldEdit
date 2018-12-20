package com.boydti.fawe.installer;

import com.boydti.fawe.FaweVersion;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.StringMan;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class InstallerFrame extends JFrame {
    private final InvisiblePanel loggerPanel;
    private Color LIGHT_GRAY = new Color(0x66, 0x66, 0x66);
    private Color GRAY = new Color(0x44, 0x44, 0x46);
    private Color DARK_GRAY = new Color(0x33, 0x33, 0x36);
    private Color DARKER_GRAY = new Color(0x26, 0x26, 0x28);
    private Color INVISIBLE = new Color(0, 0, 0, 0);
    private Color OFF_WHITE = new Color(200, 200, 200);

    private JTextArea loggerTextArea;
    private BrowseButton browse;

    public InstallerFrame() throws Exception {
        final MovablePanel movable = new MovablePanel(this);

        Container content = this.getContentPane();
        content.add(movable);
        this.setSize(480, 320);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setUndecorated(true);
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - this.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - this.getHeight()) / 2);
        this.setLocation(x, y);
        this.setVisible(true);
        this.setOpacity(0);
        movable.setBackground(DARK_GRAY);
        movable.setLayout(new BorderLayout());

        fadeIn();

        JPanel topBar = new InvisiblePanel(new BorderLayout());
        {
            JPanel topBarLeft = new InvisiblePanel();
            JPanel topBarRight = new InvisiblePanel();

            JLabel title = new JLabel("FastAsyncWorldEdit Installer");
            title.setHorizontalAlignment(SwingConstants.CENTER);
            title.setAlignmentX(Component.RIGHT_ALIGNMENT);
            title.setForeground(LIGHT_GRAY);

            MinimizeButton minimize = new MinimizeButton(this);
            CloseButton exit = new CloseButton();

            topBarLeft.add(title);
            topBarRight.add(minimize);
            topBarRight.add(exit);

            topBar.add(topBarLeft, BorderLayout.CENTER);
            topBar.add(topBarRight, BorderLayout.EAST);
        }
        final JPanel mainContent = new InvisiblePanel(new BorderLayout());
        {
            final JPanel browseContent = new InvisiblePanel(new BorderLayout());
            File dir = MainUtil.getWorkingDirectory("minecraft");
            JLabel folder = new JLabel("Folder: ");
            folder.setForeground(OFF_WHITE);
            final InteractiveButton text = new InteractiveButton(dir.getPath(), DARKER_GRAY) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    browse.actionPerformed(e);
                }
            };
            text.setForeground(OFF_WHITE);
            text.setBackground(DARKER_GRAY);
            text.setOpaque(true);
            text.setBorder(new EmptyBorder(4, 4, 4, 4));
            browse = new BrowseButton("") {
                @Override
                public void onSelect(File folder) {
                    text.setText(folder.getPath());
                    movable.repaint();
                }
            };
            InteractiveButton install = new InteractiveButton(">> Create Profile <<", DARKER_GRAY) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        install(text.getText());
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            };
            browseContent.add(folder, BorderLayout.WEST);
            browseContent.add(text, BorderLayout.CENTER);
            browseContent.add(browse, BorderLayout.EAST);
            final JPanel installContent = new InvisiblePanel(new FlowLayout());
            install.setPreferredSize(new Dimension(416, 32));
            installContent.add(install);
            installContent.setBorder(new EmptyBorder(10, 0, 10, 0));
            this.loggerPanel = new InvisiblePanel(new BorderLayout());
            this.loggerPanel.setBackground(Color.GREEN);
            loggerPanel.setPreferredSize(new Dimension(416, 160));
            loggerTextArea = new JTextArea(12, 52);
            loggerTextArea.setBackground(GRAY);
            loggerTextArea.setForeground(DARKER_GRAY);
            loggerTextArea.setFont(new Font(loggerTextArea.getFont().getName(), Font.PLAIN, 9));
            JScrollPane scroll = new JScrollPane(loggerTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBackground(DARK_GRAY);
            scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
            loggerPanel.add(scroll);
            loggerPanel.setVisible(false);

            mainContent.setBorder(new EmptyBorder(6, 32, 6, 32));
            mainContent.add(browseContent, BorderLayout.NORTH);
            mainContent.add(installContent, BorderLayout.CENTER);
            mainContent.add(loggerPanel, BorderLayout.SOUTH);
        }
        JPanel bottomBar = new InvisiblePanel();
        {
            try {
                InputStream stream = getClass().getResourceAsStream("/fawe.properties");
                java.util.Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
                String versionString = scanner.next().trim();
                scanner.close();
                FaweVersion version = new FaweVersion(versionString);
                String date = new Date(100 + version.year, version.month, version.day).toGMTString();
                String build = "https://ci.athion.net/job/FastAsyncWorldEdit/" + version.build;
                String commit = "https://github.com/boy0001/FastAsyncWorldedit/commit/" + Integer.toHexString(version.hash);
                String footerMessage = "FAWE v" + version.major + "." + version.minor + "." + version.patch + " by Empire92 (c) 2017 (GPL v3.0)";
                URL licenseUrl = new URL("https://github.com/boy0001/FastAsyncWorldedit/blob/master/LICENSE");
                URLButton licenseButton = new URLButton(licenseUrl, footerMessage);
                bottomBar.add(licenseButton);
            } catch (Throwable ignore) {
                ignore.printStackTrace();
            }
            URL chat = new URL("https://discord.gg/ngZCzbU");
            URLButton chatButton = new URLButton(chat, "Chat");
            bottomBar.add(chatButton);
            URL wiki = new URL("https://github.com/boy0001/FastAsyncWorldedit/wiki");
            URLButton wikiButton = new URLButton(wiki, "Wiki");
            bottomBar.add(wikiButton);
            URL issue = new URL("https://github.com/boy0001/FastAsyncWorldedit/issues/new");
            URLButton issueButton = new URLButton(issue, "Report Issue");
            bottomBar.add(issueButton);
        }

        // We want to add these a bit later
        movable.add(topBar, BorderLayout.NORTH);
        this.setVisible(true);
        this.repaint();
        movable.add(mainContent, BorderLayout.CENTER);
        this.setVisible(true);
        this.repaint();
        movable.add(bottomBar, BorderLayout.SOUTH);
        this.setVisible(true);
        this.repaint();
    }

    private boolean newLine = false;

    public void prompt(String message) {
        JOptionPane.showMessageDialog(null, message);
    }

    public void debug(String m) {
        System.out.println(m);
    }

    public void install(String name) throws Exception {
        if (!loggerPanel.isVisible()) {
            loggerPanel.setVisible(true);
            this.repaint();
            System.setOut(new TextAreaOutputStream(loggerTextArea));
        }
        if (name == null || name.isEmpty()) {
            prompt("No folder selection");
            return;
        }
        final File dirMc = new File(name);
        if (!dirMc.exists()) {
            prompt("Folder does not exist");
            return;
        }
        if (!dirMc.isDirectory()) {
            prompt("You must select a folder, not a file");
            return;
        }
        Thread installThread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> supported = Arrays.asList("v1710", "v189", "v194", "v110", "v111");
                String supportedString = null;
                for (String version : supported) {
                    try {
                        Class.forName("com.boydti.fawe.forge." + version + ".ForgeChunk_All");
                        supportedString = version;
                        break;
                    } catch (ClassNotFoundException ignore) {
                    }
                }
                if (supportedString == null) {
                    prompt("This version of FAWE cannot be installed this way.");
                    return;
                }
                debug("Selected version " + supportedString);
                URL forgeUrl;
                URL worldEditUrl;
                URL worldEditCuiUrl;
                try {
                    switch (supportedString) {
                        case "v111":
                            forgeUrl = new URL("https://files.minecraftforge.net/maven/net/minecraftforge/forge/1.11.2-13.20.0.2201/forge-1.11.2-13.20.0.2201-installer.jar");
                            worldEditUrl = new URL("http://builds.enginehub.org/job/worldedit/9593/download/worldedit-forge-mc1.11-6.1.6-SNAPSHOT-dist.jar");
                            worldEditCuiUrl = new URL("https://addons-origin.cursecdn.com/files/2361/241/worldeditcuife-v1.0.6-mf-1.11.2-13.20.0.2201.jar");
                            break;
                        case "v110":
                            forgeUrl = new URL("http://files.minecraftforge.net/maven/net/minecraftforge/forge/1.10.2-12.18.3.2185/forge-1.10.2-12.18.3.2185-installer.jar");
                            worldEditUrl = new URL("http://builds.enginehub.org/job/worldedit/9395/download/worldedit-forge-mc1.10.2-6.1.4-SNAPSHOT-dist.jar");
                            worldEditCuiUrl = new URL("https://addons-origin.cursecdn.com/files/2361/239/WorldEditCuiFe-v1.0.6-mf-1.10.2-12.18.2.2125.jar");
                            break;
                        case "v194":
                            forgeUrl = new URL("https://files.minecraftforge.net/maven/net/minecraftforge/forge/1.9.4-12.17.0.2051/forge-1.9.4-12.17.0.2051-installer.jar");
                            worldEditUrl = new URL("http://builds.enginehub.org/job/worldedit/9171/download/worldedit-forge-mc1.9.4-6.1.3-SNAPSHOT-dist.jar");
                            worldEditCuiUrl = new URL("https://addons-origin.cursecdn.com/files/2361/236/WorldEditCuiFe-v1.0.6-mf-1.9.4-12.17.0.1976.jar");
                            break;
                        case "v189":
                            forgeUrl = new URL("https://files.minecraftforge.net/maven/net/minecraftforge/forge/1.8.9-11.15.1.1902-1.8.9/forge-1.8.9-11.15.1.1902-1.8.9-installer.jar");
                            worldEditUrl = new URL("http://builds.enginehub.org/job/worldedit/8755/download/worldedit-forge-mc1.8.9-6.1.1-dist.jar");
                            worldEditCuiUrl = new URL("https://addons-origin.cursecdn.com/files/2361/235/WorldEditCuiFe-v1.0.6-mf-1.8.9-11.15.1.1855.jar");
                            break;
                        case "v1710":
                            forgeUrl = new URL("https://files.minecraftforge.net/maven/net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/forge-1.7.10-10.13.4.1614-1.7.10-installer.jar");
                            worldEditUrl = new URL("http://builds.enginehub.org/job/worldedit/9194/download/worldedit-forge-mc1.7.10-6.1.2-SNAPSHOT-dist.jar");
                            worldEditCuiUrl = new URL("https://addons-origin.cursecdn.com/files/2361/234/WorldEditCuiFe-v1.0.6-mf-1.7.10-10.13.4.1566.jar");
                            break;
                        default:
                            return;
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return;
                }
                try { // install forge
                    debug("Downloading forge installer from:\n - https://files.minecraftforge.net/");
                    URLClassLoader loader = new URLClassLoader(new URL[]{forgeUrl});
                    debug("Connected");
                    Class<?> forgeInstallClass = loader.loadClass("net.minecraftforge.installer.ClientInstall");
                    debug("Found ClientInstall class");
                    Object forgeInstallInstance = forgeInstallClass.newInstance();
                    debug(forgeInstallInstance + " | " + forgeInstallClass + " | " + StringMan.getString(forgeInstallClass.getMethods()));
                    debug("Created instance " + forgeInstallInstance);
                    Method methodRun = forgeInstallClass.getDeclaredMethods()[0];//("run", File.class, Predicate.class);
                    Object alwaysTrue = loader.loadClass("com.google.common.base.Predicates").getDeclaredMethod("alwaysTrue").invoke(null);
                    methodRun.invoke(forgeInstallInstance, dirMc, alwaysTrue);
                    debug("Forge profile created, now installing WorldEdit");
                } catch (Throwable e) {
                    e.printStackTrace();
                    prompt("[ERROR] Forge install failed, download from:\nhttps://files.minecraftforge.net/");
                }
                File mods = new File(dirMc, "mods");
                if (!mods.exists()) {
                    debug("Creating mods directory");
                    mods.mkdirs();
                } else {
                    for (File file : mods.listFiles()) {
                        String name = file.getName().toLowerCase();
                        if ((name.contains("worldedit") || name.contains("fawe"))) {
                            debug("Delete existing: " + file.getName());
                            file.delete();
                        }
                    }
                }
                try { // install worldedit
                    debug("Downloading WE-CUI from:\n - https://minecraft.curseforge.com/projects/worldeditcui-forge-edition");
                    try (ReadableByteChannel rbc = Channels.newChannel(worldEditCuiUrl.openStream())) {
                        try (FileOutputStream fos = new FileOutputStream(new File(mods, "WorldEditCUI.jar"))) {
                            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                        }
                    }
                    debug("Successfully downloaded WorldEdit-CUI");
                } catch (Throwable e) {
                    prompt("[ERROR] WorldEdit install failed, download from:\nhttp://builds.enginehub.org/job/worldedit");
                }
                try { // install worldedit
                    debug("Downloading WorldEdit from:\n - http://builds.enginehub.org/job/worldedit");
                    try (ReadableByteChannel rbc = Channels.newChannel(worldEditUrl.openStream())) {
                        try (FileOutputStream fos = new FileOutputStream(new File(mods, "WorldEdit.jar"))) {
                            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                        }
                    }
                    debug("Successfully downloaded WorldEdit");
                } catch (Throwable e) {
                    prompt("[ERROR] WorldEdit install failed, download from:\nhttp://builds.enginehub.org/job/worldedit");
                }
                try { // install FAWE
                    debug("Copying FastAsyncWorldEdit to mods directory");
                    File file = new File(InstallerFrame.class.getProtectionDomain().getCodeSource().getLocation().getPath());
                    debug(" - " + file.getPath());
                    MainUtil.copyFile(file, new File(mods, "FastAsyncWorldEdit.jar"));
                    debug("Installation complete!");
                } catch (Throwable e) {
                    prompt("[ERROR] Copy installer failed, please copy this installer jar manually");
                }
                prompt("Installation complete!\nLaunch the game using the forge profile.");
            }
        });
        installThread.start();
    }

    public void fadeIn() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (float i = 0; i <= 1; i += 0.001) {
                    InstallerFrame.this.setOpacity(i);
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public static void main(String[] args) throws Exception {
        InstallerFrame window = new InstallerFrame();
    }
}
