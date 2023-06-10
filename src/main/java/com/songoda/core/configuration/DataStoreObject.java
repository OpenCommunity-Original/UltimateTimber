package com.songoda.core.configuration;

import org.bukkit.configuration.ConfigurationSection;

public interface DataStoreObject<T> {

    /**
     * @return a unique identifier for saving this value with
     */
    String getConfigKey();

    /**
     * Save this data to a ConfigurationSection
     */
    void saveToSection(ConfigurationSection sec);

    /**
     * Mark this data as needing a save or not
     */
    void setChanged(boolean isChanged);
}
