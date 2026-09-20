package de.flori.mCJS;

public class Version {

    public static final String VERSION = "2.1.0";

    public static final int MAJOR = 2;

    public static final int MINOR = 1;

    public static final int PATCH = 0;

    public static final String FULL_VERSION = VERSION;

    public static String getVersion() {
        return VERSION;
    }

    public static int getMajor() {
        return MAJOR;
    }

    public static int getMinor() {
        return MINOR;
    }

    public static int getPatch() {
        return PATCH;
    }

    public static boolean isNewerThan(String otherVersion) {
        try {
            String[] parts = otherVersion.split("\\.");
            int otherMajor = Integer.parseInt(parts[0]);
            int otherMinor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int otherPatch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            if (MAJOR > otherMajor) return true;
            if (MAJOR < otherMajor) return false;
            if (MINOR > otherMinor) return true;
            if (MINOR < otherMinor) return false;
            return PATCH > otherPatch;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getVersionInfo() {
        return "MC-JS v" + VERSION + " (API v" + MAJOR + "." + MINOR + ")";
    }
}
