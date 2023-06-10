package com.songoda.core.hooks;

import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HookManager<T extends Hook> {
    private final Class typeClass;
    private final Map<PluginHook, T> registeredHooks = new HashMap<>();
    private T defaultHook = null;
    private boolean loaded = false;

    public HookManager(Class typeClass) {
        this.typeClass = typeClass;
    }

    /**
     * Load all supported plugins.
     */
    public void load() {
        load(null);
    }

    /**
     * Load all supported plugins.
     *
     * @param hookingPlugin plugin to pass to the hook handler
     */
    public void load(Plugin hookingPlugin) {
        if (!loaded) {
            registeredHooks.putAll(PluginHook.loadHooks(typeClass, hookingPlugin).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> (T) e.getValue())));

            if (!registeredHooks.isEmpty()) {
                defaultHook = registeredHooks.values().iterator().next();
            }

            loaded = true;
        }
    }

    /**
     * Get the currently selected plugin hook. <br>
     * If none were set, then the first one found is used.
     *
     * @return The instance of T that was created, or null if none available.
     */
    public T getCurrentHook() {
        return defaultHook;
    }

    /**
     * Check to see if there is a default hook loaded.
     *
     * @return returns false if there are no supported plugins loaded
     */
    public boolean isEnabled() {
        return defaultHook != null;
    }

}
