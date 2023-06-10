package com.songoda.core.compatibility;

import org.bukkit.Bukkit;

public enum ServerVersion {
    UNKNOWN, V1_7, V1_8, V1_9, V1_10, V1_11, V1_12, V1_13, V1_14, V1_15, V1_16, V1_17, V1_18, V1_19, V1_20, V1_21;

    private final static String serverPackagePath;
    private final static String serverPackageVersion;
    private final static ServerVersion serverVersion;
    private final static boolean isMocked;

    static {
        String srvPackage = Bukkit.getServer().getClass().getPackage().getName();
        isMocked = srvPackage.equals("be.seeseemelk.mockbukkit");

        if (isMocked) {
            String packageVersionShard = "v" + Bukkit.getServer().getBukkitVersion().replace('.', '_') + "_R0";

            serverPackagePath = "org.bukkit.craftbukkit." + packageVersionShard;
            serverPackageVersion = packageVersionShard;
        } else {
            serverPackagePath = srvPackage;
            serverPackageVersion = serverPackagePath.substring(serverPackagePath.lastIndexOf('.') + 1);
        }

        serverVersion = getVersion();
    }

    private static ServerVersion getVersion() {
        for (ServerVersion version : values()) {
            if (serverPackageVersion.toUpperCase().startsWith(version.name())) {
                return version;
            }
        }

        return UNKNOWN;
    }

    public static String getServerVersionString() {
        return serverPackageVersion;
    }

    public static ServerVersion getServerVersion() {
        return serverVersion;
    }

    public static boolean isServerVersion(ServerVersion version) {
        return serverVersion == version;
    }

    public static boolean isServerVersionAtLeast(ServerVersion version) {
        return serverVersion.ordinal() >= version.ordinal();
    }

    public static boolean isServerVersionAtOrBelow(ServerVersion version) {
        return serverVersion.ordinal() <= version.ordinal();
    }

    public static boolean isServerVersionBelow(ServerVersion version) {
        return serverVersion.ordinal() < version.ordinal();
    }

    public boolean isAtLeast(ServerVersion other) {
        return this.ordinal() >= other.ordinal();
    }
}
