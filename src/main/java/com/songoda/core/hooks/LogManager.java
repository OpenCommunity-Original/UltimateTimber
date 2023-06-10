package com.songoda.core.hooks;

import com.songoda.core.hooks.log.Log;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;

/**
 * A convenience class for static access to a Log HookManager
 */
public class LogManager {
    private static final HookManager<Log> manager = new HookManager(Log.class);

    /**
     * Load all supported log plugins. <br />
     * Note: This method should be called in your plugin's onEnable() section
     */
    public static void load() {
        manager.load();
    }

    /**
     * Log the removal of a block. <br />
     * NOTE: using a default log assumes that this library is shaded
     *
     * @param player player to commit actionremvedplaced
     */
    public static void logRemoval(OfflinePlayer player, Block block) {
        if (manager.isEnabled()) {
            manager.getCurrentHook().logRemoval(player, block);
        }
    }

}
