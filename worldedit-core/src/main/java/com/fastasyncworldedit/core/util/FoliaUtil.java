package com.fastasyncworldedit.core.util;

/**
 * Utility for detecting whether the runtime server environment is Folia.
 *
 * @since TODO
 */
public class FoliaUtil {

    private static final Boolean FOLIA_DETECTED = detectFolia();

    /**
     * Checks if the server is running on Folia.
     *
     * @return true if running on Folia
     * @since TODO
     */
    public static boolean isFoliaServer() {
        return FOLIA_DETECTED;
    }

    /**
     * Checks if the server is running on Folia.
     *
     * @return true if running on Folia
     * @since TODO
     */
    public static boolean isFolia() {
        return FOLIA_DETECTED;
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
