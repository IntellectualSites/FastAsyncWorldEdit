package com.fastasyncworldedit.bukkit.util;

import com.google.common.collect.ComparisonChain;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for retrieving and comparing minecraft server versions.
 */
public class MinecraftVersion implements Comparable<MinecraftVersion> {

    public static final MinecraftVersion NETHER = new MinecraftVersion(1, 16);

    private final int major;
    private final int minor;
    private final int release;

    /**
     * Construct a new version with major, minor and release version.
     *
     * @param major   Major part of the version, only {@code 1} would make sense.
     * @param minor   Minor part, full updates, e.g. Nether & Caves & Cliffs
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
     */
    public MinecraftVersion() {
        // Array consists of three version parts, eg. ['v1', '16', 'R3']
        String[] versionParts = getPackageVersion().split("_");

        if (versionParts.length != 3) {
            throw new IllegalStateException("Failed to determine minecraft version!");
        }

        this.major = Integer.parseInt(versionParts[0].substring(1));
        this.minor = Integer.parseInt(versionParts[1]);
        this.release = Integer.parseInt(versionParts[2].substring(1));
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
    public boolean isEqualOrHigher(MinecraftVersion other) {
        return compareTo(other) >= 0;
    }

    /**
     * @param other The other version to compare against.
     * @return {@code true} if this version is lower or equal compared to the other version.
     */
    public boolean isEqualOrLower(MinecraftVersion other) {
        return compareTo(other) <= 0;
    }

    /**
     * @param other The other version to compare against.
     * @return {@code true} if this version is higher than the other version.
     */
    public boolean isHigher(MinecraftVersion other) {
        return compareTo(other) > 0;
    }

    /**
     * @param other The other version to compare against.
     * @return {@code true} if this version is lower than to the other version.
     */
    public boolean isLower(MinecraftVersion other) {
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
    public int compareTo(@NotNull MinecraftVersion other) {
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MinecraftVersion that = (MinecraftVersion) o;

        if (getMajor() != that.getMajor()) return false;
        if (getMinor() != that.getMinor()) return false;
        return getRelease() == that.getRelease();
    }

    /**
     * Determines the server version based on the package path, e.g. {@code org.bukkit.craftbukkit.v1_16_R3},
     * where v1_16_R3 is the resolved version.
     *
     * @return The package version.
     */
    private static String getPackageVersion() {
        String fullPackagePath = Bukkit.getServer().getClass().getPackage().getName();
        return fullPackagePath.substring(fullPackagePath.lastIndexOf('.') + 1);
    }

}