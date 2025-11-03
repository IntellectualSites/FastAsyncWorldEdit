package com.fastasyncworldedit.core.util;

/**
 * Utility class for Folia server detection and compatibility.
 */
public class FoliaUtil {

    private static final Boolean FOLIA_DETECTED = detectFolia();

    /**
     * Check if we're running on a Folia server.
     *
     * @return true if running on Folia
     */
    public static boolean isFoliaServer() {
        return FOLIA_DETECTED;
    }

    /**
     * Detect if Folia is available by checking for the RegionizedServer class.
     * This method is called once during class initialization for performance.
     *
     * @return true if Folia is detected
     */
    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
