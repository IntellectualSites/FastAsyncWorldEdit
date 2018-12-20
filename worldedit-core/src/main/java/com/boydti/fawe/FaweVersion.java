package com.boydti.fawe;

public class FaweVersion {
    public final int year, month, day, hash, build, major, minor, patch;

    public FaweVersion(String version) {
        String[] split = version.substring(version.indexOf('=') + 1).split("-");
        if (split[0].equals("unknown")) {
            this.year = month = day = hash = build = major = minor = patch = 0;
            return;
        }
        String[] date = split[0].split("\\.");
        this.year = Integer.parseInt(date[0]);
        this.month = Integer.parseInt(date[1]);
        this.day = Integer.parseInt(date[2]);
        this.hash = Integer.parseInt(split[1], 16);
        this.build = Integer.parseInt(split[2]);
        String[] semver = split[3].split("\\.");
        this.major = Integer.parseInt(semver[0]);
        this.minor = Integer.parseInt(semver[1]);
        this.patch = Integer.parseInt(semver[2]);
    }

    @Override
    public String toString() {
        return "FastAsyncWorldEdit-" + year + "." + month + "." + day + "-" + Integer.toHexString(hash) + "-" + build;
    }

    public boolean isNewer(FaweVersion other) {
        return other.build < this.build && (this.major > other.major || (this.major == other.major && this.minor > other.minor) || (this.major == other.major && this.minor == other.minor && this.patch > other.patch));
    }
}