package com.syspilot.viewer.utility;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class PlatformPaths {

    private static final String OS = System.getProperty("os.name", "").toLowerCase();
    private static final String USER_HOME = System.getProperty("user.home");

    private PlatformPaths() {}

    public static boolean isWindows() { return OS.contains("win"); }
    public static boolean isLinux()   { return OS.contains("nux"); }
    public static boolean isMac()     { return OS.contains("mac"); }

    /**
     * SysPilot 基础目录，不同操作系统的前缀不同。
     * Linux:   ~/文档/SysPilot
     * Windows: ~/Desktop/SysPilot
     * macOS:   ~/Documents/SysPilot
     */
    public static Path getSysPilotDir() {
        if (isWindows()) {
            return Paths.get(USER_HOME, "Desktop", "SysPilot");
        } else if (isLinux()) {
            return Paths.get(USER_HOME, "文档", "SysPilot");
        } else {
            return Paths.get(USER_HOME, "Documents", "SysPilot");
        }
    }

    /**
     * trajectory_visible app 目录 = SysPilot/trajectory_visible
     */
    public static Path getAppDir() {
        return getSysPilotDir().resolve("trajectory_visible");
    }
}
