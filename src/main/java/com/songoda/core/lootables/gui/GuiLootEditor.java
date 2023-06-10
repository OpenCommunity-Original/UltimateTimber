package com.songoda.core.lootables.gui;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.lootables.loot.Loot;
import com.songoda.core.lootables.loot.LootManager;
import com.songoda.core.utils.TextUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GuiLootEditor extends Gui {
    private final LootManager lootManager;
    private final Loot loot;
    private final Gui returnGui;

    public GuiLootEditor(LootManager lootManager, Loot loot, Gui returnGui) {
        super(6, returnGui);

        this.lootManager = lootManager;
        this.loot = loot;
        this.returnGui = returnGui;

        setDefaultItem(null);
        setTitle("Loot Editor");

        paint();

        setOnClose((event) ->
                lootManager.saveLootables(false));
    }

    public void paint() {
        if (inventory != null) {
            inventory.clear();
        }

        setActionForRange(0, 0, 5, 9, null);

        setButton(8, GuiUtils.createButtonItem(CompatibleMaterial.OAK_DOOR, TextUtils.formatText("&cBack")),
                (event) -> guiManager.showGUI(event.player, returnGui));

        setButton(9, GuiUtils.createButtonItem(loot.getMaterial() == null ? CompatibleMaterial.BARRIER : loot.getMaterial(),
                TextUtils.formatText("&7Current Material: &6" + (loot.getMaterial() != null
                        ? loot.getMaterial().name() : "None")), TextUtils.formatText(
                        Arrays.asList("",
                                "&8Click to set the material to",
                                "&8the material in your hand.")
                )), (event) -> {
            ItemStack stack = event.player.getInventory().getItemInMainHand();
            loot.setMaterial(CompatibleMaterial.getMaterial(stack));

            paint();
        });

        setButton(11, GuiUtils.createButtonItem(CompatibleMaterial.WRITABLE_BOOK,
                        TextUtils.formatText("&7Lore Override:"),
                        TextUtils.formatText(loot.getLore() == null ? Collections.singletonList("&6None set") : loot.getLore())),
                (event) -> guiManager.showGUI(event.player, new GuiLoreEditor(loot, this)));

        List<String> enchantments = new ArrayList<>();

        if (loot.getEnchants() != null) {
            for (Map.Entry<String, Integer> entry : loot.getEnchants().entrySet()) {
                enchantments.add("&6" + entry.getKey() + " " + entry.getValue());
            }
        }

        setButton(12, GuiUtils.createButtonItem(CompatibleMaterial.ENCHANTED_BOOK,
                        TextUtils.formatText("&7Enchantments:"),
                        TextUtils.formatText(enchantments.isEmpty() ? Collections.singletonList("&6None set") : enchantments)),
                (event) -> guiManager.showGUI(event.player, new GuiEnchantEditor(loot, this)));

        setButton(13, GuiUtils.createButtonItem(
                        loot.getBurnedMaterial() == null
                                ? CompatibleMaterial.FIRE_CHARGE
                                : loot.getBurnedMaterial(),
                        TextUtils.formatText("&7Current Burned Material: &6"
                                + (loot.getBurnedMaterial() == null
                                ? "None"
                                : loot.getBurnedMaterial().name())), TextUtils.formatText(
                                Arrays.asList("",
                                        "&8Click to set the burned material to",
                                        "&8the material in your hand.")
                        )),
                (event) -> {
                    ItemStack stack = event.player.getInventory().getItemInMainHand();
                    loot.setBurnedMaterial(CompatibleMaterial.getMaterial(stack));

                    paint();
                });

        setButton(19, GuiUtils.createButtonItem(CompatibleMaterial.CHEST,
                        TextUtils.formatText("&7Allow Looting Enchantment?: &6" + loot.isAllowLootingEnchant())),
                (event) -> {
                    loot.setAllowLootingEnchant(!loot.isAllowLootingEnchant());

                    paint();
                    event.player.closeInventory();
                });

        List<String> entities = new ArrayList<>();

        if (loot.getOnlyDropFor() != null) {
            for (EntityType entity : loot.getOnlyDropFor()) {
                entities.add("&6" + entity.name());
            }
        }

        setButton(22, GuiUtils.createButtonItem(CompatibleMaterial.ENCHANTED_BOOK,
                        TextUtils.formatText("&7Only Drop For:"),
                        TextUtils.formatText(entities)),
                (event) -> guiManager.showGUI(event.player, new GuiEntityEditor(loot, this)));

        int i = 9 * 5;
        for (Loot loot : loot.getChildLoot()) {
            ItemStack item = loot.getMaterial() == null
                    ? CompatibleMaterial.BARRIER.getItem()
                    : GuiUtils.createButtonItem(loot.getMaterial(), null,
                    TextUtils.formatText("&6Left click &7to edit"),
                    TextUtils.formatText("&6Right click &7to destroy"));

            setButton(i, item,
                    (event) -> {
                        if (event.clickType == ClickType.RIGHT) {
                            this.loot.removeChildLoot(loot);
                            paint();
                        } else if (event.clickType == ClickType.LEFT) {
                            guiManager.showGUI(event.player, new GuiLootEditor(lootManager, loot, this));
                        }
                    });

            ++i;
        }
    }
}
