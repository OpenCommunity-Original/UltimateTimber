package com.songoda.core.lootables.gui;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.lootables.loot.Loot;
import com.songoda.core.utils.TextUtils;

import java.util.*;

public class GuiEnchantEditor extends Gui {
    private final Gui returnGui;
    private final Loot loot;

    public GuiEnchantEditor(Loot loot, Gui returnGui) {
        super(1, returnGui);

        this.returnGui = returnGui;
        this.loot = loot;

        setDefaultItem(null);
        setTitle("Enchantment Editor");

        paint();
    }

    public void paint() {
        Map<String, Integer> lore = loot.getEnchants() == null ? new HashMap<>() : new HashMap<>(loot.getEnchants());

        setButton(2, GuiUtils.createButtonItem(CompatibleMaterial.OAK_FENCE_GATE,
                        TextUtils.formatText("&cBack")),
                (event) -> {
                    guiManager.showGUI(event.player, returnGui);
                    ((GuiLootEditor) returnGui).paint();
                });
        setButton(6, GuiUtils.createButtonItem(CompatibleMaterial.OAK_FENCE_GATE,
                        TextUtils.formatText("&cBack")),
                (event) -> {
                    guiManager.showGUI(event.player, returnGui);
                    ((GuiLootEditor) returnGui).paint();
                });

        List<String> enchantments = new ArrayList<>();

        String last = null;

        if (!lore.isEmpty()) {
            for (Map.Entry<String, Integer> entry : lore.entrySet()) {
                last = entry.getKey();
                enchantments.add("&6" + entry.getKey() + " " + entry.getValue());
            }
        }

        setItem(4, GuiUtils.createButtonItem(CompatibleMaterial.WRITABLE_BOOK,
                TextUtils.formatText("&7Enchant Override:"),
                lore.isEmpty()
                        ? TextUtils.formatText(Collections.singletonList("&cNo enchantments set..."))
                        : TextUtils.formatText(enchantments)));

        String lastFinal = last;
        setButton(5, GuiUtils.createButtonItem(CompatibleMaterial.ARROW,
                        TextUtils.formatText("&cRemove the last line")),
                (event -> {
                    lore.remove(lastFinal);
                    loot.setEnchants(lore);
                    paint();
                }));
    }
}
