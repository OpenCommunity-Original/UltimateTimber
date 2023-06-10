package com.songoda.core.hooks.worldguard;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class WorldGuardRegionHandler {
    static boolean wgPlugin;
    static Object worldGuardPlugin;
    static boolean legacy_v60 = false;
    static boolean legacy_v62 = false;
    static boolean legacy_v5 = false;
    static Method legacy_getRegionManager = null;
    static Method legacy_getApplicableRegions_Region = null;
    static Constructor legacy_newProtectedCuboidRegion;
    static Class legacy_blockVectorClazz;
    static Constructor legacy_newblockVector;
    static Class legacy_VectorClazz;
    static Constructor legacy_newVectorClazz;
    static Method legacy_getApplicableRegions_Vector = null;

    static void init() {
        if (wgPlugin = (worldGuardPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard")) != null) {
            // a number of flags were introduced in 7.x that aren't in 5 or 6
            try {
                // if this class exists, we're on 7.x
                Class.forName("com.sk89q.worldguard.protection.flags.WeatherTypeFlag");
            } catch (ClassNotFoundException ex) {
                try {
                    // if this class exists, we're on 6.2
                    Class.forName("com.sk89q.worldguard.protection.flags.registry.SimpleFlagRegistry");
                    legacy_v62 = true;
                } catch (ClassNotFoundException ex2) {
                    try {
                        // if this class exists, we're on 6.0
                        Class.forName("com.sk89q.worldguard.protection.flags.BuildFlag");
                        legacy_v60 = true;
                    } catch (ClassNotFoundException ex3) {
                        try {
                            // if this class exists, we're on 5.x
                            Class.forName("com.sk89q.worldguard.protection.flags.DefaultFlag");
                            legacy_v5 = true;
                        } catch (ClassNotFoundException ex4) {
                            // ¯\_(ツ)_/¯
                            wgPlugin = false;
                        }
                    }
                }
            }
        }
        if (wgPlugin && (legacy_v62 || legacy_v60 || legacy_v5)) {
            try {
                // cache reflection methods
                if (legacy_getRegionManager == null) {
                    legacy_getRegionManager = worldGuardPlugin.getClass()
                            .getDeclaredMethod("getRegionManager", org.bukkit.World.class);
                    legacy_getApplicableRegions_Region = RegionManager.class.getDeclaredMethod("getApplicableRegions",
                            Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion"));
                    legacy_blockVectorClazz = Class.forName("com.sk89q.worldedit.BlockVector");
                    legacy_newblockVector = legacy_blockVectorClazz.getConstructor(int.class, int.class, int.class);
                    legacy_newProtectedCuboidRegion = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion")
                            .getConstructor(String.class, legacy_blockVectorClazz, legacy_blockVectorClazz);
                    legacy_VectorClazz = Class.forName("com.sk89q.worldedit.Vector");
                    legacy_newVectorClazz = legacy_VectorClazz.getConstructor(int.class, int.class, int.class);
                    legacy_getApplicableRegions_Vector = RegionManager.class.getDeclaredMethod("getApplicableRegions", legacy_VectorClazz);
                }
            } catch (Exception ex) {
                //Bukkit.getServer().getLogger().log(Level.WARNING, "Failed to set legacy WorldGuard Flags", ex);
                Bukkit.getServer().getLogger().log(Level.WARNING, "Could not load WorldGuard methods for " + (legacy_v62 ? "6.2" : (legacy_v60 ? "6.0" : "5")));
                wgPlugin = false;
            }
        }
    }

    private static List<String> getRegionNamesLegacy(Chunk c) {
        try {
            // grab the applicable manager for this world
            Object worldManager = legacy_getRegionManager.invoke(worldGuardPlugin, c.getWorld());

            if (worldManager == null) {
                return null;
            }

            // Create a legacy ProtectedCuboidRegion
            Object chunkRegion = legacy_newProtectedCuboidRegion.newInstance("__TEST__",
                    legacy_newblockVector.newInstance(c.getX() << 4, c.getWorld().getMaxHeight(), c.getZ() << 4),
                    legacy_newblockVector.newInstance((c.getX() << 4) + 15, 0, (c.getZ() << 4) + 15));

            // now look for any intersecting regions
            // ApplicableRegionSet's prototype is different from v5 to v6, but they're both Iterable
            Iterable<ProtectedRegion> set = (Iterable<ProtectedRegion>) legacy_getApplicableRegions_Region.invoke(worldManager, chunkRegion);

            List<String> regions = new ArrayList<>();
            List<String> parentNames = new ArrayList<>();

            for (ProtectedRegion region : set) {
                String id = region.getId();

                regions.add(id);

                ProtectedRegion parent = region.getParent();

                while (parent != null) {
                    parentNames.add(parent.getId());
                    parent = parent.getParent();
                }
            }

            regions.removeAll(parentNames);

            return regions;
        } catch (Exception ex) {
            Bukkit.getServer().getLogger().log(Level.WARNING, "Could not grab regions from WorldGuard", ex);
        }

        return Collections.emptyList();
    }

    private static List<String> getRegionNamesLegacy(Location loc) {
        try {
            // grab the applicable manager for this world
            Object worldManager = legacy_getRegionManager.invoke(worldGuardPlugin, loc.getWorld());
            if (worldManager == null) {
                return Collections.emptyList();
            }

            // create a vector object
            Object vec = legacy_newVectorClazz.newInstance(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            // now look for any intersecting regions
            // ApplicableRegionSet's prototype is different from v5 to v6, but they're both Iterable
            Iterable<ProtectedRegion> set = (Iterable<ProtectedRegion>) legacy_getApplicableRegions_Vector.invoke(worldManager, legacy_VectorClazz.cast(vec));

            List<String> regions = new ArrayList<>();
            List<String> parentNames = new ArrayList<>();

            for (ProtectedRegion region : set) {
                String id = region.getId();

                regions.add(id);

                ProtectedRegion parent = region.getParent();

                while (parent != null) {
                    parentNames.add(parent.getId());
                    parent = parent.getParent();
                }
            }

            regions.removeAll(parentNames);

            return regions;
        } catch (Exception ex) {
            Bukkit.getServer().getLogger().log(Level.WARNING, "Could not grab regions from WorldGuard", ex);
        }

        return Collections.emptyList();
    }
}
