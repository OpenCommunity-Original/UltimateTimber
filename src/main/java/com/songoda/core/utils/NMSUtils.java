package com.songoda.core.utils;

import com.songoda.core.compatibility.ClassMapping;
import com.songoda.core.compatibility.ServerVersion;
import org.bukkit.entity.Player;

public class NMSUtils {
    /**
     * @deprecated Use {@link ClassMapping} instead
     */
    @Deprecated
    public static Class<?> getCraftClass(String className) {
        try {
            String fullName = "org.bukkit.craftbukkit." + ServerVersion.getServerVersionString() + "." + className;
            return Class.forName(fullName);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    // FIXME: Remove this method on next major release

    /**
     * @deprecated Doesn't work cross version, use NMSManager#getPlayer() instead
     */
    @Deprecated
    public static void sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField(ServerVersion.isServerVersionAtLeast(ServerVersion.V1_17) ? "b" : "playerConnection").get(handle);

            playerConnection.getClass().getMethod("sendPacket", ClassMapping.PACKET.getClazz()).invoke(playerConnection, packet);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
