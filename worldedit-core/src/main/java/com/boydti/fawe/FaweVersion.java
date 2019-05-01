package com.boydti.fawe;

public class FaweVersion {
    public final int year, month, day, hash, build;

    public FaweVersion(int year, int month, int day, int hash, int build) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hash = hash;
        this.build = build;
    }

    public FaweVersion(String version, String commit, String date) {
        String[] split = version.substring(version.indexOf('=') + 1).split("\\.");
        this.build = Integer.parseInt(split[2]);
        this.hash = Integer.parseInt(commit.substring(commit.indexOf('=') + 1), 16);
        String[] split1 = date.substring(date.indexOf('=') + 1).split("\\.");
        this.year = Integer.parseInt(split1[0]);
        this.month = Integer.parseInt(split1[1]);
        this.day = Integer.parseInt(split1[2]);
    }

    public static FaweVersion tryParse(String version, String commit, String date) {
        try {
            return new FaweVersion(version, commit, date);
        } catch (Exception ignore) {
            ignore.printStackTrace();
            return new FaweVersion(0, 0, 0, 0, 0);
        }
    }

    @Override
    public String toString() {
        return "FastAsyncWorldEdit-" + year + "." + month + "." + day + "-" + Integer.toHexString(hash) + "-" + build;
    }

    public boolean isNewer(FaweVersion other) {
        return other.build < this.build;
    }
}