package com.boydti.fawe;

/** An internal FAWE class not meant for public use. **/
public class FaweVersion {
    public final int year;
    public final int month;
    public final int day;
    public final int hash;
    public final int build;

    public FaweVersion(int year, int month, int day, int hash, int build) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hash = hash;
        this.build = build;
    }

    public FaweVersion(String version, String commit, String date) {
        String[] split = version.substring(version.indexOf('=') + 1).split("-");
        this.build = Integer.parseInt(split[1]);
        this.hash = Integer.parseInt(commit.substring(commit.indexOf('=') + 1), 16);
        String[] split1 = date.substring(date.indexOf('=') + 1).split("\\.");
        this.year = Integer.parseInt(split1[0]);
        this.month = Integer.parseInt(split1[1]);
        this.day = Integer.parseInt(split1[2]);
    }

    public static FaweVersion tryParse(String version, String commit, String date) {
        try {
            return new FaweVersion(version, commit, date);
        } catch (Exception exception) {
            exception.printStackTrace();
            return new FaweVersion(0, 0, 0, 0, 0);
        }
    }

    @Override public String toString() {
        if (hash == 0 && build == 0) {
            return "FastAsyncWorldEdit-1.16-NoVer-SNAPSHOT";
        } else {
            return "FastAsyncWorldEdit-1.16" + build;
        }
    }

    public boolean isNewer(FaweVersion other) {
        return other.build < this.build;
    }
}
