package com.thevoxelbox.voxelsniper.jsap;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.util.StringUtils;
import org.bukkit.ChatColor;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * JSAP parser with help generating code.
 *
 * @author MikeMatrix
 */
public class HelpJSAP extends JSAP {

    private String name;
    private String explanation;
    private int screenWidth;

    /**
     * @param name
     * @param explanation
     * @param screenWidth
     */
    public HelpJSAP(final String name, final String explanation, final int screenWidth) {
        super();

        this.name = name;
        this.explanation = explanation;
        this.screenWidth = screenWidth;

        try {
            this.registerParameter(new Switch("help", JSAP.NO_SHORTFLAG, "help", "Displays this help page."));
        } catch (final JSAPException e) {
        }
    }

    /**
     * @param name
     * @param explanation
     * @param screenWidth
     * @param resourceName
     * @throws java.io.IOException                    if an I/O error occurs
     * @throws com.martiansoftware.jsap.JSAPException if the configuration is not valid
     */
    public HelpJSAP(final String name, final String explanation, final int screenWidth, final String resourceName) throws IOException, JSAPException {
        super(resourceName);

        this.name = name;
        this.explanation = explanation;
        this.screenWidth = screenWidth;

        try {
            this.registerParameter(new Switch("help", JSAP.NO_SHORTFLAG, "help", "Displays this help page."));
        } catch (final JSAPException e) {
        }
    }

    /**
     * @param name
     * @param explanation
     * @param screenWidth
     * @param jsapXML
     * @throws java.io.IOException                    if an I/O error occurs
     * @throws com.martiansoftware.jsap.JSAPException if the configuration is not valid
     */
    public HelpJSAP(final String name, final String explanation, final int screenWidth, final URL jsapXML) throws IOException, JSAPException {
        super(jsapXML);

        this.name = name;
        this.explanation = explanation;
        this.screenWidth = screenWidth;

        try {
            this.registerParameter(new Switch("help", JSAP.NO_SHORTFLAG, "help", "Displays this help page."));
        } catch (final JSAPException e) {
        }
    }

    /**
     * @return the explanation
     */
    public final String getExplanation() {
        return this.explanation;
    }

    /**
     * @param explanation the explanation to set
     */
    public final void setExplanation(final String explanation) {
        this.explanation = explanation;
    }

    /**
     * @return the name
     */
    public final String getName() {
        return this.name;
    }

    /**
     * @param name the name to set
     */
    public final void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the screenWidth
     */
    public final int getScreenWidth() {
        return this.screenWidth;
    }

    /**
     * @param screenWidth the screenWidth to set
     */
    public final void setScreenWidth(final int screenWidth) {
        this.screenWidth = screenWidth;
    }

    /**
     * @param jsapResult
     * @return if something has been written on writer.
     */
    public final List<String> writeHelpOrErrorMessageIfRequired(final JSAPResult jsapResult) {
        if (!(jsapResult.success()) || jsapResult.getBoolean("help")) {
            List<String> returnValue = new LinkedList<>();
            // To avoid spurious missing argument errors we never print errors if help is required.
            if (!jsapResult.getBoolean("help")) {
                for (final Iterator<?> err = jsapResult.getErrorMessageIterator(); err.hasNext(); ) {
                    returnValue.add(ChatColor.RED + "Error: " + ChatColor.DARK_RED + err.next());
                }

                return returnValue;
            }
            returnValue.add(ChatColor.GOLD + "Usage:");

            List<?> l = StringUtils.wrapToList(this.name + " " + this.getUsage(), this.screenWidth);
            for (final Object aL : l) {
                returnValue.add("  " + aL.toString());
            }

            if (this.explanation != null) {
                returnValue.add("");
                l = StringUtils.wrapToList(this.explanation, this.screenWidth);
                for (final Object aL : l) {
                    final String next = (String) aL;
                    returnValue.add(ChatColor.AQUA + next);
                }
            }

            returnValue.add("");
            returnValue.add(this.getHelp(this.screenWidth));
            return returnValue;
        }

        return Collections.emptyList();
    }
}
