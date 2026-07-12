package com.fastasyncworldedit.bukkit.util;

import com.google.common.collect.ComparisonChain;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

/**
 * Utility class for retrieving and comparing minecraft server versions.
 */
public class MinecraftVersion implements Comparable<MinecraftVersion> {

    public static final MinecraftVersion NETHER = new MinecraftVersion(1, 16);
    public static final MinecraftVersion CAVES_17 = new MinecraftVersion(1, 17);
    public static final MinecraftVersion CAVES_18 = new MinecraftVersion(1, 18);
    private static MinecraftVersion current = null;

    private final int major;
    private final int minor;
    private final int release;

    /**
     * Construct a new version with major, minor and release version.
     *
     * @param major   Major part of the version, only {@code 1} would make sense.
     * @param minor   Minor part, full updates, e.g. Nether &amp; Caves &amp; Cliffs
     * @param release Release, changes for the server software during a minor update.
     */
    public MinecraftVersion(int major, int minor, int release) {
        this.major = major;
        this.minor = minor;
        this.release = release;
    }

    /**
     * Construct a new version with major and minor version.
     * The release version is set to 0, therefore ignored.
     *
     * @see MinecraftVersion#MinecraftVersion(int, int, int)
     */
    public MinecraftVersion(int major, int minor) {
        this(major, minor, 0);
    }

    /**
     * Construct a new version with major, minor and release based on the server version.
     * @deprecated use {@link MinecraftVersion#getCurrent()} instead.
     */
    @Deprecated(since = "2.4.10", forRemoval = true)
    public MinecraftVersion() {
        MinecraftVersion current = getCurrent();

        this.major = current.getMajor();
        this.minor = current.getMinor();
        this.release = current.getRelease();
    }

    /**
     * Get the minecraft version that the server is currently running
     *
     * @since 2.1.0
     */
    public static MinecraftVersion getCurrent() {
        if (current == null) {
            return current = detectMinecraftVersion();
        }
        return current;
    }

    private static MinecraftVersion detectMinecraftVersion() {
        int major;
        int minor;
        int release;
        if (PaperLib.isPaper()) {
            // Snapshots, pre-releases, release candidates have a space after the version.
            // Let's ignore that here
            String minecraftVersion = Bukkit.getMinecraftVersion().split(" ")[0];
            String[] parts = minecraftVersion.split(Pattern.quote("."));
            if (parts.length != 2 && parts.length != 3) {
                throw new IllegalStateException("Failed to determine minecraft version!");
            }
            major = Integer.parseInt(parts[0]);
            minor = Integer.parseInt(parts[1]);
            release = parts.length == 3 ? Integer.parseInt(parts[2]) : 0; // e.g. 1.18
        } else {
            String packageVersion = getPackageVersion();
            String[] parts = packageVersion.split("_");
            if (parts.length == 3 && parts[0].startsWith("v") && parts[2].startsWith("R")) {
                major = Integer.parseInt(parts[0].substring(1));
                minor = Integer.parseInt(parts[1]);
                release = Integer.parseInt(parts[2].substring(1));
            } else {
                // Newer servers may expose unversioned CraftBukkit packages (e.g. "craftbukkit").
                String version = Bukkit.getServer().getBukkitVersion().split("-")[0];
                String[] dotted = version.split(Pattern.quote("."));
                if (dotted.length != 2 && dotted.length != 3) {
                    throw new IllegalStateException("Failed to determine minecraft version!");
                }
                major = Integer.parseInt(dotted[0]);
                minor = Integer.parseInt(dotted[1]);
                release = dotted.length == 3 ? Integer.parseInt(dotted[2]) : 0;
            }
        }
        return new MinecraftVersion(major, minor, release);
    }

    /**
     * Determines the server version based on the CraftBukkit package path, e.g. {@code org.bukkit.craftbukkit.v1_16_R3},
     * where v1_16_R3 is the resolved version.
     *
     * @return The package version.
     */
    private static String getPackageVersion() {
        String fullPackagePath = Bukkit.getServer().getClass().getPackage().getName();
        if (fullPackagePath.contains(".")) {
            String token = fullPackagePath.substring(fullPackagePath.lastIndexOf('.') + 1);
            // Keep legacy behavior only for versioned CraftBukkit package names (e.g. v1_21_R7).
            if (token.startsWith("v") && token.contains("_R")) {
                return token;
            }
        }
        String version = Bukkit.getServer().getBukkitVersion();
        return version.split("-")[0];
    }

    /**
     * @param other The other version to compare against.
     * @return {@code true} if this version is equal to the other version.
     */
    public boolean isEqual(MinecraftVersion other) {
        return compareTo(other) == 0;
    }

    /**
     * @param other The other version to compare against.
     * @return {@code true} if this version is higher or equal compared to the other version.
     */
    public boolean isEqualOrHigherThan(MinecraftVersion other) {
        return compareTo(other) >= 0;
    }

    /**
     * @param other The other version to compare against.
     * @return {@code true} if this version is lower or equal compared to the other version.
     */
    public boolean isEqualOrLowerThan(MinecraftVersion other) {
        return compareTo(other) <= 0;
    }

    /**
     * @param other The other version to compare against.
     * @return {@code true} if this version is higher than the other version.
     */
    public boolean isHigherThan(MinecraftVersion other) {
        return compareTo(other) > 0;
    }

    /**
     * @param other The other version to compare against.
     * @return {@code true} if this version is lower than to the other version.
     */
    public boolean isLowerThan(MinecraftVersion other) {
        return compareTo(other) < 0;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getRelease() {
        return release;
    }

    @Override
    public int compareTo(@Nonnull MinecraftVersion other) {
        if (other.equals(this)) {
            return 0;
        }
        return ComparisonChain.start()
                .compare(getMajor(), other.getMajor())
                .compare(getMinor(), other.getMinor())
                .compare(getRelease(), other.getRelease()).result();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MinecraftVersion that = (MinecraftVersion) o;

        if (getMajor() != that.getMajor()) {
            return false;
        }
        if (getMinor() != that.getMinor()) {
            return false;
        }
        return getRelease() == that.getRelease();
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + release;
    }

}
