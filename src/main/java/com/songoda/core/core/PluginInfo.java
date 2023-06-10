package com.songoda.core.core;

import com.songoda.core.compatibility.CompatibleMaterial;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class PluginInfo {
    private final JavaPlugin javaPlugin;
    private final int songodaId;
    private final List<PluginInfoModule> modules = new ArrayList<>();

    public PluginInfo(JavaPlugin javaPlugin, int songodaId, String icon, String coreLibraryVersion) {
        this.javaPlugin = javaPlugin;
        this.songodaId = songodaId;
    }

    public PluginInfoModule addModule(PluginInfoModule module) {
        modules.add(module);

        return module;
    }

    public JavaPlugin getJavaPlugin() {
        return javaPlugin;
    }

}
