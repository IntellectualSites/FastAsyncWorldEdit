package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.VoxelSniper;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import java.io.*;

/**
 * Overwrites signs. (Wiki:
 * http://www.voxelwiki.com/minecraft/VoxelSniper#Sign_Overwrite_Brush)
 *
 * @author Monofraps
 */
public class SignOverwriteBrush extends Brush {
    private static final int MAX_SIGN_LINE_LENGTH = 15;
    private static final int NUM_SIGN_LINES = 4;
    // these are no array indices
    private static final int SIGN_LINE_1 = 1;
    private static final int SIGN_LINE_2 = 2;
    private static final int SIGN_LINE_3 = 3;
    private static final int SIGN_LINE_4 = 4;
    private String[] signTextLines = new String[NUM_SIGN_LINES];
    private boolean[] signLinesEnabled = new boolean[NUM_SIGN_LINES];
    private boolean rangedMode = false;

    /**
     *
     */
    public SignOverwriteBrush() {
        this.setName("Sign Overwrite Brush");

        clearBuffer();
        resetStates();
    }

    /**
     * Sets the text of a given sign.
     *
     * @param sign
     */
    private void setSignText(final Sign sign) {
        for (int i = 0; i < this.signTextLines.length; i++) {
            if (this.signLinesEnabled[i]) {
                sign.setLine(i, this.signTextLines[i]);
            }
        }

        sign.update();
    }

    /**
     * Sets the text of the target sign if the target block is a sign.
     *
     * @param v
     */
    private void setSingle(final SnipeData v) {
        if (this.getTargetBlock().getState() instanceof Sign) {
            setSignText((Sign) this.getTargetBlock().getState());
        } else {
            v.sendMessage(ChatColor.RED + "Target block is not a sign.");
            return;
        }
    }

    /**
     * Sets all signs in a range of box{x=z=brushSize*2+1 ; z=voxelHeight*2+1}.
     *
     * @param v
     */
    private void setRanged(final SnipeData v) {
        final int minX = getTargetBlock().getX() - v.getBrushSize();
        final int maxX = getTargetBlock().getX() + v.getBrushSize();
        final int minY = getTargetBlock().getY() - v.getVoxelHeight();
        final int maxY = getTargetBlock().getY() + v.getVoxelHeight();
        final int minZ = getTargetBlock().getZ() - v.getBrushSize();
        final int maxZ = getTargetBlock().getZ() + v.getBrushSize();

        boolean signFound = false; // indicates whether or not a sign was set

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState blockState = this.getWorld().getBlockAt(x, y, z).getState();
                    if (blockState instanceof Sign) {
                        setSignText((Sign) blockState);
                        signFound = true;
                    }
                }
            }
        }

        if (!signFound) {
            v.sendMessage(ChatColor.RED + "Did not found any sign in selection box.");
        }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        if (this.rangedMode) {
            setRanged(v);
        } else {
            setSingle(v);
        }
    }

    @Override
    protected final void powder(final SnipeData v) {
        if (this.getTargetBlock().getState() instanceof Sign) {
            Sign sign = (Sign) this.getTargetBlock().getState();

            for (int i = 0; i < this.signTextLines.length; i++) {
                if (this.signLinesEnabled[i]) {
                    this.signTextLines[i] = sign.getLine(i);
                }
            }

            displayBuffer(v);
        } else {
            v.sendMessage(ChatColor.RED + "Target block is not a sign.");
        }
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        boolean textChanged = false;

        for (int i = 0; i < par.length; i++) {
            String parameter = par[i];

            try {
                if (parameter.equalsIgnoreCase("info")) {
                    v.sendMessage(ChatColor.AQUA + "Sign Overwrite Brush Powder/Arrow:");
                    v.sendMessage(ChatColor.BLUE + "The arrow writes the internal line buffer to the tearget sign.");
                    v.sendMessage(ChatColor.BLUE + "The powder reads the text of the target sign into the internal buffer.");
                    v.sendMessage(ChatColor.AQUA + "Sign Overwrite Brush Parameters:");
                    v.sendMessage(ChatColor.GREEN + "-1[:(enabled|disabled)] ... " + ChatColor.BLUE + "-- Sets the text of the first sign line. (e.g. -1 Blah Blah)");
                    v.sendMessage(ChatColor.GREEN + "-2[:(enabled|disabled)] ... " + ChatColor.BLUE + "-- Sets the text of the second sign line. (e.g. -2 Blah Blah)");
                    v.sendMessage(ChatColor.GREEN + "-3[:(enabled|disabled)] ... " + ChatColor.BLUE + "-- Sets the text of the third sign line. (e.g. -3 Blah Blah)");
                    v.sendMessage(ChatColor.GREEN + "-4[:(enabled|disabled)] ... " + ChatColor.BLUE + "-- Sets the text of the fourth sign line. (e.g. -4 Blah Blah)");
                    v.sendMessage(ChatColor.GREEN + "-clear " + ChatColor.BLUE + "-- Clears the line buffer. (Alias: -c)");
                    v.sendMessage(ChatColor.GREEN + "-clearall " + ChatColor.BLUE + "-- Clears the line buffer and sets all lines back to enabled. (Alias: -ca)");
                    v.sendMessage(ChatColor.GREEN + "-multiple [on|off] " + ChatColor.BLUE + "-- Enables or disables ranged mode. (Alias: -m) (see Wiki for more information)");
                    v.sendMessage(ChatColor.GREEN + "-save (name) " + ChatColor.BLUE + "-- Save you buffer to a file named [name]. (Alias: -s)");
                    v.sendMessage(ChatColor.GREEN + "-open (name) " + ChatColor.BLUE + "-- Loads a buffer from a file named [name]. (Alias: -o)");
                } else if (parameter.startsWith("-1")) {
                    textChanged = true;
                    i = parseSignLineFromParam(par, SIGN_LINE_1, v, i);
                } else if (parameter.startsWith("-2")) {
                    textChanged = true;
                    i = parseSignLineFromParam(par, SIGN_LINE_2, v, i);
                } else if (parameter.startsWith("-3")) {
                    textChanged = true;
                    i = parseSignLineFromParam(par, SIGN_LINE_3, v, i);
                } else if (parameter.startsWith("-4")) {
                    textChanged = true;
                    i = parseSignLineFromParam(par, SIGN_LINE_4, v, i);
                } else if (parameter.equalsIgnoreCase("-clear") || parameter.equalsIgnoreCase("-c")) {
                    clearBuffer();
                    v.sendMessage(ChatColor.BLUE + "Internal text buffer cleard.");
                } else if (parameter.equalsIgnoreCase("-clearall") || parameter.equalsIgnoreCase("-ca")) {
                    clearBuffer();
                    resetStates();
                    v.sendMessage(ChatColor.BLUE + "Internal text buffer cleard and states back to enabled.");
                } else if (parameter.equalsIgnoreCase("-multiple") || parameter.equalsIgnoreCase("-m")) {
                    if ((i + 1) >= par.length) {
                        v.sendMessage(ChatColor.RED + String.format("Missing parameter after %s.", parameter));
                        continue;
                    }

                    rangedMode = (par[++i].equalsIgnoreCase("on") || par[++i].equalsIgnoreCase("yes"));
                    v.sendMessage(ChatColor.BLUE + String.format("Ranged mode is %s", ChatColor.GREEN + (rangedMode ? "enabled" : "disabled")));
                    if (this.rangedMode) {
                        v.sendMessage(ChatColor.GREEN + "Brush size set to " + ChatColor.RED + v.getBrushSize());
                        v.sendMessage(ChatColor.AQUA + "Brush height set to " + ChatColor.RED + v.getVoxelHeight());
                    }
                } else if (parameter.equalsIgnoreCase("-save") || parameter.equalsIgnoreCase("-s")) {
                    if ((i + 1) >= par.length) {
                        v.sendMessage(ChatColor.RED + String.format("Missing parameter after %s.", parameter));
                        continue;
                    }

                    String fileName = par[++i];
                    saveBufferToFile(fileName, v);
                } else if (parameter.equalsIgnoreCase("-open") || parameter.equalsIgnoreCase("-o")) {
                    if ((i + 1) >= par.length) {
                        v.sendMessage(ChatColor.RED + String.format("Missing parameter after %s.", parameter));
                        continue;
                    }

                    String fileName = par[++i];
                    loadBufferFromFile(fileName, "", v);
                    textChanged = true;
                }
            } catch (Exception exception) {
                v.sendMessage(ChatColor.RED + String.format("Error while parsing parameter %s", parameter));
                exception.printStackTrace();
            }
        }

        if (textChanged) {
            displayBuffer(v);
        }
    }

    /**
     * Parses parameter input text of line [param:lineNumber].
     * Iterates though the given array until the next top level param (a parameter which starts
     * with a dash -) is found.
     *
     * @param params
     * @param lineNumber
     * @param v
     * @param i
     * @return
     */
    private int parseSignLineFromParam(final String[] params, final int lineNumber, final SnipeData v, int i) {
        final int lineIndex = lineNumber - 1;
        final String parameter = params[i];

        boolean statusSet = false;

        if (parameter.contains(":")) {
            this.signLinesEnabled[lineIndex] = parameter.substring(parameter.indexOf(":")).equalsIgnoreCase(":enabled");
            v.sendMessage(ChatColor.BLUE + "Line " + lineNumber + " is " + ChatColor.GREEN + (this.signLinesEnabled[lineIndex] ? "enabled" : "disabled"));
            statusSet = true;
        }

        if ((i + 1) >= params.length) {
            // return if the user just wanted to set the status
            if (statusSet) {
                return i;
            }

            v.sendMessage(ChatColor.RED + "Warning: No text after -" + lineNumber + ". Setting buffer text to \"\" (empty string)");
            signTextLines[lineIndex] = "";
            return i;
        }

        String newText = "";

        // go through the array until the next top level parameter is found
        for (i++; i < params.length; i++) {
            final String currentParameter = params[i];

            if (currentParameter.startsWith("-")) {
                i--;
                break;
            } else {
                newText += currentParameter + " ";
            }
        }

        newText = ChatColor.translateAlternateColorCodes('&', newText);

        // remove last space or return if the string is empty and the user just wanted to set the status
        if (!newText.isEmpty() && newText.endsWith(" ")) {
            newText = newText.substring(0, newText.length() - 1);
        } else if (newText.isEmpty()) {
            if (statusSet) {
                return i;
            }
            v.sendMessage(ChatColor.RED + "Warning: No text after -" + lineNumber + ". Setting buffer text to \"\" (empty string)");
        }

        // check the line length and cut the text if needed
        if (newText.length() > MAX_SIGN_LINE_LENGTH) {
            v.sendMessage(ChatColor.RED + "Warning: Text on line " + lineNumber + " exceeds the maximum line length of " + MAX_SIGN_LINE_LENGTH + " characters. Your text will be cut.");
            newText = newText.substring(0, MAX_SIGN_LINE_LENGTH);
        }

        this.signTextLines[lineIndex] = newText;
        return i;
    }

    private void displayBuffer(final SnipeData v) {
        v.sendMessage(ChatColor.BLUE + "Buffer text set to: ");
        for (int i = 0; i < this.signTextLines.length; i++) {
            v.sendMessage((this.signLinesEnabled[i] ? ChatColor.GREEN + "(E): " : ChatColor.RED + "(D): ") + ChatColor.BLACK + this.signTextLines[i]);
        }
    }

    /**
     * Saves the buffer to file.
     *
     * @param fileName
     * @param v
     */
    private void saveBufferToFile(final String fileName, final SnipeData v) {
        final File store = new File(VoxelSniper.getInstance().getDataFolder() + "/" + fileName + ".vsign");
        if (store.exists()) {
            v.sendMessage("This file already exists.");
            return;
        }

        try {
            store.createNewFile();
            FileWriter outFile = new FileWriter(store);
            BufferedWriter outStream = new BufferedWriter(outFile);

            for (int i = 0; i < this.signTextLines.length; i++) {
                outStream.write(this.signLinesEnabled[i] + "\n");
                outStream.write(this.signTextLines[i] + "\n");
            }

            outStream.close();
            outFile.close();

            v.sendMessage(ChatColor.BLUE + "File saved successfully.");
        } catch (IOException exception) {
            v.sendMessage(ChatColor.RED + "Failed to save file. " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * Loads a buffer from a file.
     *
     * @param fileName
     * @param userDomain
     * @param v
     */
    private void loadBufferFromFile(final String fileName, final String userDomain, final SnipeData v) {
        final File store = new File(VoxelSniper.getInstance().getDataFolder() + "/" + fileName + ".vsign");
        if (!store.exists()) {
            v.sendMessage("This file does not exist.");
            return;
        }

        try {
            FileReader inFile = new FileReader(store);
            BufferedReader inStream = new BufferedReader(inFile);

            for (int i = 0; i < this.signTextLines.length; i++) {
                this.signLinesEnabled[i] = Boolean.valueOf(inStream.readLine());
                this.signTextLines[i] = inStream.readLine();
            }

            inStream.close();
            inFile.close();

            v.sendMessage(ChatColor.BLUE + "File loaded successfully.");
        } catch (IOException exception) {
            v.sendMessage(ChatColor.RED + "Failed to load file. " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * Clears the internal text buffer. (Sets it to empty strings)
     */
    private void clearBuffer() {
        for (int i = 0; i < this.signTextLines.length; i++) {
            this.signTextLines[i] = "";
        }
    }

    /**
     * Resets line enabled states to enabled.
     */
    private void resetStates() {
        for (int i = 0; i < this.signLinesEnabled.length; i++) {
            this.signLinesEnabled[i] = true;
        }
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName("Sign Overwrite Brush");

        vm.custom(ChatColor.BLUE + "Buffer text: ");
        for (int i = 0; i < this.signTextLines.length; i++) {
            vm.custom((this.signLinesEnabled[i] ? ChatColor.GREEN + "(E): " : ChatColor.RED + "(D): ") + ChatColor.BLACK + this.signTextLines[i]);
        }

        vm.custom(ChatColor.BLUE + String.format("Ranged mode is %s", ChatColor.GREEN + (rangedMode ? "enabled" : "disabled")));
        if (rangedMode) {
            vm.size();
            vm.height();
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.signoverwrite";
    }
}
