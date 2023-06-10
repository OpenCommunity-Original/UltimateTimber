package com.songoda.core.utils;

import com.songoda.core.compatibility.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class uses some Minecraft code and also Paper API
 */
public class ItemUtils {
    static Class<?> cb_ItemStack = ClassMapping.CRAFT_ITEM_STACK.getClazz();
    static Class<?> mc_ItemStack = ClassMapping.ITEM_STACK.getClazz();
    static Class<?> mc_NBTTagCompound = ClassMapping.NBT_TAG_COMPOUND.getClazz();
    static Class<?> mc_NBTTagList = ClassMapping.NBT_TAG_LIST.getClazz();
    static Method mc_ItemStack_getTag;
    static Method mc_ItemStack_setTag;
    static Method mc_NBTTagCompound_set;
    static Method mc_NBTTagCompound_remove;
    static Method cb_CraftItemStack_asNMSCopy;
    static Method cb_CraftItemStack_asCraftMirror;
    static Class cb_CraftPlayer = NMSUtils.getCraftClass("entity.CraftPlayer");
    private static Method methodAsBukkitCopy, methodAsNMSCopy, methodA;

    static {
        try {
            ItemStack.class.getMethod("getI18NDisplayName");
        } catch (NoSuchMethodException | SecurityException ex) {
        }
    }

    static {
        try {
            Class<?> clazzEnchantmentManager = ClassMapping.ENCHANTMENT_MANAGER.getClazz();
            Class<?> clazzItemStack = ClassMapping.ITEM_STACK.getClazz();
            Class<?> clazzCraftItemStack = ClassMapping.CRAFT_ITEM_STACK.getClazz();

            methodAsBukkitCopy = clazzCraftItemStack.getMethod("asBukkitCopy", clazzItemStack);
            methodAsNMSCopy = clazzCraftItemStack.getMethod("asNMSCopy", ItemStack.class);

            if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_19)) {
                Class<?> clazzRandomSource = ClassMapping.RANDOM_SOURCE.getClazz();
                methodA = clazzEnchantmentManager.getMethod("a", clazzRandomSource.getMethod("c").getReturnType(), clazzItemStack, int.class, boolean.class);
            } else if (ServerVersion.isServerVersion(ServerVersion.V1_8)) {
                methodA = clazzEnchantmentManager.getMethod("a", Random.class, clazzItemStack, int.class);
            } else {
                methodA = clazzEnchantmentManager.getMethod("a", Random.class, clazzItemStack, int.class, boolean.class);
            }
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    static {
        if (cb_ItemStack != null) {
            try {
                mc_ItemStack_getTag = MethodMapping.MC_ITEM_STACK__GET_TAG.getMethod(mc_ItemStack);
                mc_ItemStack_setTag = MethodMapping.MC_ITEM_STACK__SET_TAG.getMethod(mc_ItemStack);
                mc_NBTTagCompound_set = MethodMapping.MC_NBT_TAG_COMPOUND__SET.getMethod(mc_NBTTagCompound);
                mc_NBTTagCompound_remove = MethodMapping.MC_NBT_TAG_COMPOUND__REMOVE.getMethod(mc_NBTTagCompound);
//                mc_NBTTagCompound_setShort = MethodMapping.MC_NBT_TAG_COMPOUND__SET_SHORT.getMethod(mc_NBTTagCompound);
//                mc_NBTTagCompound_setString = MethodMapping.MC_NBT_TAG_COMPOUND__SET_STRING.getMethod(mc_NBTTagCompound);
                cb_CraftItemStack_asNMSCopy = MethodMapping.CB_ITEM_STACK__AS_NMS_COPY.getMethod(cb_ItemStack);
                cb_CraftItemStack_asCraftMirror = MethodMapping.CB_ITEM_STACK__AS_CRAFT_MIRROR.getMethod(cb_ItemStack);
//                mc_NBTTagList_add = MethodMapping.MC_NBT_TAG_LIST__ADD.getMethod(mc_NBTTagList);
            } catch (Exception ex) {
                Logger.getLogger(ItemUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static ItemStack applyRandomEnchants(ItemStack item, int level) {
        try {
            Object nmsItemStack = methodAsNMSCopy.invoke(null, item);

            if (ServerVersion.isServerVersion(ServerVersion.V1_8)) {
                nmsItemStack = methodA.invoke(null, new Random(), nmsItemStack, level);
            } else {
                nmsItemStack = methodA.invoke(null, new Random(), nmsItemStack, level, false);
            }

            item = (ItemStack) methodAsBukkitCopy.invoke(null, nmsItemStack);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
        }

        return item;
    }

    public static boolean hasEnoughDurability(ItemStack tool, int requiredAmount) {
        if (tool.getType().getMaxDurability() <= 1) {
            return true;
        }

        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)) {
            if (!tool.hasItemMeta() || !(tool.getItemMeta() instanceof Damageable damageable)) {
                return true;
            }

            int durabilityRemaining = tool.getType().getMaxDurability() - damageable.getDamage();

            return durabilityRemaining > requiredAmount;
        }

        return tool.getDurability() + requiredAmount <= tool.getType().getMaxDurability();
    }

    /**
     * Make an item glow as if it contained an enchantment. <br>⁄
     * Tested working 1.8-1.14
     *
     * @param item itemstack to create a glowing copy of
     * @return copy of item with a blank enchantment nbt tag
     */
    public static ItemStack addGlow(ItemStack item) {
        // from 1.11 up, fake enchantments don't work without more steps
        // creating a new Enchantment involves some very involved reflection,
        // as the namespace is the same but until 1.12 requires an int, but versions after require a String
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_11)) {
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            // you can at least hide the enchantment, though
            ItemMeta m = item.getItemMeta();
            m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(m);

            return item;
        }

        // hack a fake enchant onto the item
        // Confirmed works on 1.8, 1.9, 1.10
        // Does not work 1.11+ (minecraft ignores the glitched enchantment)
        if (item != null && item.getType() != Material.AIR && cb_CraftItemStack_asCraftMirror != null) {
            try {
                Object nmsStack = cb_CraftItemStack_asNMSCopy.invoke(null, item);
                Object tag = mc_ItemStack_getTag.invoke(nmsStack);
                if (tag == null) {
                    tag = mc_NBTTagCompound.newInstance();
                }
                // set to have a fake enchantment
                Object enchantmentList = mc_NBTTagList.newInstance();
                /*
                if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)) {
                    // Servers from 1.13 and up change the id to a string
                    Object fakeEnchantment = mc_NBTTagCompound.newInstance();
                    mc_NBTTagCompound_setString.invoke(fakeEnchantment, "id", "glow:glow");
                    mc_NBTTagCompound_setShort.invoke(fakeEnchantment, "lvl", (short) 0);
                    mc_NBTTagList_add.invoke(enchantmentList, fakeEnchantment);
                } else if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_11)) {
                    // Servers from 1.11 and up require *something* in the enchantment field
                    Object fakeEnchantment = mc_NBTTagCompound.newInstance();
                    mc_NBTTagCompound_setShort.invoke(fakeEnchantment, "id", (short) 245);
                    mc_NBTTagCompound_setShort.invoke(fakeEnchantment, "lvl", (short) 1);
                    mc_NBTTagList_add.invoke(enchantmentList, fakeEnchantment);
                }//*/
                mc_NBTTagCompound_set.invoke(tag, "ench", enchantmentList);
                mc_ItemStack_setTag.invoke(nmsStack, tag);
                item = (ItemStack) cb_CraftItemStack_asCraftMirror.invoke(null, nmsStack);
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to set glow enchantment on item: " + item, ex);
            }
        }

        return item;
    }

    /**
     * Remove all enchantments, including hidden enchantments
     *
     * @param item item to clear enchants from
     * @return copy of the item without any enchantment tag
     */
    public static ItemStack removeGlow(ItemStack item) {
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_11)) {
            item.removeEnchantment(Enchantment.DURABILITY);

            return item;
        } else {
            if (item != null && item.getType() != Material.AIR && cb_CraftItemStack_asCraftMirror != null) {
                try {
                    Object nmsStack = cb_CraftItemStack_asNMSCopy.invoke(null, item);
                    Object tag = mc_ItemStack_getTag.invoke(nmsStack);

                    if (tag != null) {
                        // remove enchantment list
                        mc_NBTTagCompound_remove.invoke(tag, "ench");
                        mc_ItemStack_setTag.invoke(nmsStack, tag);
                        item = (ItemStack) cb_CraftItemStack_asCraftMirror.invoke(null, nmsStack);
                    }
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "Failed to set glow enchantment on item: " + item, ex);
                }
            }
        }

        return item;
    }

    /**
     * Use up whatever item the player is holding in their main hand
     *
     * @param player player to grab item from
     * @param hand   the hand to take the item from.
     */
    @Deprecated
    public static void takeActiveItem(Player player, CompatibleHand hand) {
        takeActiveItem(player, hand, 1);
    }

    /**
     * Use up whatever item the player is holding in their main hand
     *
     * @param player player to grab item from
     * @param hand   the hand to take the item from.
     * @param amount number of items to use up
     */
    @Deprecated
    public static void takeActiveItem(Player player, CompatibleHand hand, int amount) {
        hand.takeItem(player, amount);
    }

    /**
     * Add an item to this inventory, but only if it can be added completely.
     *
     * @param item            item to add
     * @param amount          how many of this item should be added
     * @param inventory       a list that represents the inventory
     * @param containerSize   maximum number of different items this container can
     *                        hold
     * @param reserved        slot to reserve - will not fill this slot
     * @param inventorySource Material of the container
     * @return true if the item was added
     */
    public static boolean addItem(ItemStack item, int amount, List<ItemStack> inventory, int containerSize, int reserved, Material inventorySource) {
        if (inventory == null || item == null || amount <= 0 || inventorySource == null) {
            return false;
        }

        boolean[] check = null;

        if (inventorySource != Material.AIR) {
            // Don't transfer shulker boxes into other shulker boxes, that's a bad idea.
            if (inventorySource.name().contains("SHULKER_BOX") && item.getType().name().contains("SHULKER_BOX")) {
                return false;
            }

            // some destination containers have special conditions
            switch (inventorySource.name()) {
                case "BREWING_STAND": {
                    // first compile a list of what slots to check
                    check = new boolean[5];
                    String typeStr = item.getType().name().toUpperCase();

                    if (typeStr.contains("POTION") || typeStr.contains("BOTTLE")) {
                        // potion bottles are the first three slots
                        check[0] = check[1] = check[2] = true;
                    }

                    // fuel in 5th position, input in 4th
                    if (item.getType() == Material.BLAZE_POWDER) {
                        check[4] = true;
                    } else {
                        check[3] = true;
                    }
                }
                case "SMOKER":
                case "BLAST_FURNACE":
                case "BURNING_FURNACE":
                case "FURNACE": {
                    check = new boolean[3];

                    boolean isFuel = !item.getType().name().contains("LOG") && CompatibleMaterial.getMaterial(item.getType()).isFuel();

                    // fuel is 2nd slot, input is first
                    if (isFuel) {
                        check[1] = true;
                    } else {
                        check[0] = true;
                    }
                }
            }
        }

        // grab the amount to move and the max item stack size
        int toAdd = item.getAmount();
        final int maxStack = item.getMaxStackSize();

        // we can reduce calls to ItemStack.isSimilar() by caching what cells to look at
        if (check == null) {
            check = new boolean[containerSize];
            for (int i = 0; toAdd > 0 && i < check.length; ++i) {
                check[i] = true;
            }
        }

        if (reserved >= 0 && check.length < reserved) {
            check[reserved] = false;
        }

        // first verify that we can add this item
        for (int i = 0; toAdd > 0 && i < containerSize; ++i) {
            if (check[i]) {
                final ItemStack cacheItem = i >= inventory.size() ? null : inventory.get(i);

                if (cacheItem == null || cacheItem.getAmount() == 0) {
                    // free slot!
                    toAdd -= Math.min(maxStack, toAdd);
                    check[i] = true;
                } else if (maxStack > cacheItem.getAmount() && item.isSimilar(cacheItem)) {
                    // free space!
                    toAdd -= Math.min(maxStack - cacheItem.getAmount(), toAdd);
                    check[i] = true;
                } else {
                    check[i] = false;
                }
            }
        }

        if (toAdd <= 0) {
            // all good to add!
            toAdd = item.getAmount();

            for (int i = 0; toAdd > 0 && i < containerSize; i++) {
                if (!check[i]) {
                    continue;
                }

                final ItemStack cacheItem = i >= inventory.size() ? null : inventory.get(i);

                if (cacheItem == null || cacheItem.getAmount() == 0) {
                    // free slot!
                    int adding = Math.min(maxStack, toAdd);
                    ItemStack item2 = item.clone();
                    item2.setAmount(adding);

                    if (i >= inventory.size()) {
                        inventory.add(item2);
                    } else {
                        inventory.set(i, item2);
                    }

                    toAdd -= adding;
                } else if (maxStack > cacheItem.getAmount()) {
                    // free space!
                    // (no need to check item.isSimilar(cacheItem), since we have that cached in check[])
                    int adding = Math.min(maxStack - cacheItem.getAmount(), toAdd);

                    inventory.get(i).setAmount(adding + cacheItem.getAmount());
                    toAdd -= adding;
                }
            }

            return true;
        }

        return false;
    }

}
