package de.flori.mCJS;

/**
 * Version information for MC-JS plugin.
 * This class provides access to version information at runtime.
 */
public class Version {
    /**
     * Current version of MC-JS.
     * Format: MAJOR.MINOR.PATCH (Semantic Versioning)
     * 
     * MAJOR version when you make incompatible API changes
     * MINOR version when you add functionality in a backwards compatible manner
     * PATCH version when you make backwards compatible bug fixes
     */
    public static final String VERSION = "2.0.0";
    
    /**
     * Major version number
     */
    public static final int MAJOR = 2;
    
    /**
     * Minor version number
     */
    public static final int MINOR = 0;
    
    /**
     * Patch version number
     */
    public static final int PATCH = 0;
    
    /**
     * Full version string with build metadata (if available)
     */
    public static final String FULL_VERSION = VERSION;
    
    /**
     * Get the version string
     * @return Version string (e.g., "2.0.0")
     */
    public static String getVersion() {
        return VERSION;
    }
    
    /**
     * Get the major version number
     * @return Major version
     */
    public static int getMajor() {
        return MAJOR;
    }
    
    /**
     * Get the minor version number
     * @return Minor version
     */
    public static int getMinor() {
        return MINOR;
    }
    
    /**
     * Get the patch version number
     * @return Patch version
     */
    public static int getPatch() {
        return PATCH;
    }
    
    /**
     * Check if this version is newer than another version
     * @param otherVersion Version string to compare (e.g., "1.9.0")
     * @return true if this version is newer
     */
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
    
    /**
     * Get version info as formatted string
     * @return Formatted version string
     */
    public static String getVersionInfo() {
        return "MC-JS v" + VERSION + " (API v" + MAJOR + "." + MINOR + ")";
    }
}
