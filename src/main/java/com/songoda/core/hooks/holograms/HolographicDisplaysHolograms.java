package com.songoda.core.hooks.holograms;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HolographicDisplaysHolograms extends Holograms {

    private final Map<String, Hologram> holograms = new HashMap<>();
    private final String textLineFormat;

    public HolographicDisplaysHolograms(Plugin plugin) {
        super(plugin);
        String version = Bukkit.getPluginManager().getPlugin("HolographicDisplays").getDescription().getVersion();

        this.textLineFormat = version.startsWith("3") ? "TextLine{text=%s}" : "CraftTextLine [text=%s]";
    }

    @Override
    public String getName() {
        return "HolographicDisplays";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected double defaultHeightOffset() {
        return 1;
    }

    @Override
    public void createHologram(String id, Location location, List<String> lines) {
        createAt(id, location, lines);
    }

    @Override
    public void removeHologram(String id) {
        Hologram hologram = holograms.remove(id);
        if (hologram != null) {
            hologram.delete();
        }
    }

    @Override
    public void updateHologram(String id, List<String> lines) {
        bulkUpdateHolograms(Collections.singletonMap(id, lines));
    }

    @Override
    public void bulkUpdateHolograms(Map<String, List<String>> hologramData) {
        for (Map.Entry<String, List<String>> entry : hologramData.entrySet()) {
            String id = entry.getKey();
            List<String> lines = entry.getValue();

            Hologram hologram = holograms.get(id);

            // only update if there is a change to the text
            boolean isChanged = lines.size() != hologram.size();

            if (!isChanged) {
                // double-check the lines
                for (int i = 0; !isChanged && i < lines.size(); ++i) {
                    isChanged = !hologram.getLine(i).toString().equals(String.format(textLineFormat, lines.get(i)));
                }
            }

            if (isChanged) {
                hologram.clearLines();

                for (String line : lines) {
                    hologram.appendTextLine(line);
                }
            }
        }
    }

    private void createAt(String id, Location location, List<String> lines) {
        if (holograms.containsKey(id)) {
            return;
        }

        location = fixLocation(location);
        Hologram hologram = HologramsAPI.createHologram(plugin, location);

        for (String line : lines) {
            hologram.appendTextLine(line);
        }

        holograms.put(id, hologram);
    }

    @Override
    public void removeAllHolograms() {
        holograms.values().forEach(Hologram::delete);
    }

    @Override
    public boolean isHologramLoaded(String id) {
        return holograms.get(id) != null;
    }
}
