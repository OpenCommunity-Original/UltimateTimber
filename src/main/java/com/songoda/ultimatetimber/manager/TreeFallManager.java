package com.songoda.ultimatetimber.manager;

import com.songoda.core.compatibility.CompatibleHand;
import com.songoda.core.hooks.LogManager;
import com.songoda.core.hooks.WorldGuardHook;
import com.songoda.core.utils.ItemUtils;
import com.songoda.core.world.SItemStack;
import com.songoda.ultimatetimber.UltimateTimber;
import com.songoda.ultimatetimber.events.TreeFallEvent;
import com.songoda.ultimatetimber.events.TreeFellEvent;
import com.songoda.ultimatetimber.misc.OnlyToppleWhile;
import com.songoda.ultimatetimber.tree.DetectedTree;
import com.songoda.ultimatetimber.tree.ITreeBlock;
import com.songoda.ultimatetimber.tree.TreeBlockSet;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class TreeFallManager extends Manager implements Listener {

    private int maxLogBlocksAllowed;
    private boolean disInWorldGuard;

    public TreeFallManager(UltimateTimber ultimateTimber) {
        super(ultimateTimber);
        Bukkit.getPluginManager().registerEvents(this, ultimateTimber);
    }

    @Override
    public void reload() {
        this.maxLogBlocksAllowed = ConfigurationManager.Setting.MAX_LOGS_PER_CHOP.getInt();
        this.disInWorldGuard = ConfigurationManager.Setting.DISABLE_IN_WORLDGUARD_REGIONS.getBoolean();
    }

    @Override
    public void disable() {

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        TreeDefinitionManager treeDefinitionManager = this.plugin.getTreeDefinitionManager();
        TreeDetectionManager treeDetectionManager = this.plugin.getTreeDetectionManager();
        TreeAnimationManager treeAnimationManager = this.plugin.getTreeAnimationManager();
        ChoppingManager choppingManager = this.plugin.getChoppingManager();
        SaplingManager saplingManager = this.plugin.getSaplingManager();

        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack tool = CompatibleHand.getHand(event).getItem(player);

        // Protect saplings
        if (saplingManager.isSaplingProtected(block)) {
            event.setCancelled(true);
            return;
        }

        // Condition checks
        boolean isValid = !ConfigurationManager.Setting.DISABLED_WORLDS.getStringList().contains(player.getWorld().getName());

        if (!ConfigurationManager.Setting.ALLOW_CREATIVE_MODE.getBoolean() && player.getGameMode().equals(GameMode.CREATIVE))
            isValid = false;

        if (!this.checkToppleWhile(player))
            isValid = false;

        if (ConfigurationManager.Setting.REQUIRE_CHOP_PERMISSION.getBoolean() && !player.hasPermission("ultimatetimber.chop"))
            isValid = false;

        if (!choppingManager.isChopping(player))
            isValid = false;

        // WorldGuard check
        if (this.disInWorldGuard) {
            Boolean flag;
            Chunk centerChunk = player.getLocation().getChunk();
            if ((flag = WorldGuardHook.getBooleanFlag(centerChunk, "allow-claims")) != null && !flag) {
                isValid = false;
            }
        }

        if (choppingManager.isInCooldown(player))
            isValid = false;

        if (treeAnimationManager.isBlockInAnimation(block)) {
            isValid = false;
            event.setCancelled(true);
        }

        if (!treeDefinitionManager.isToolValidForAnyTreeDefinition(tool))
            isValid = false;

        boolean alwaysReplantSapling = ConfigurationManager.Setting.ALWAYS_REPLANT_SAPLING.getBoolean();
        if (!isValid && !alwaysReplantSapling)
            return;

        DetectedTree detectedTree = treeDetectionManager.detectTree(block);
        if (detectedTree == null)
            return;

        if (alwaysReplantSapling) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () ->
                    saplingManager.replantSapling(detectedTree.getTreeDefinition(), detectedTree.getDetectedTreeBlocks().getInitialLogBlock()));

            if (!isValid)
                return;
        }

        if (!treeDefinitionManager.isToolValidForTreeDefinition(detectedTree.getTreeDefinition(), tool))
            return;

        short toolDamage = this.getToolDamage(detectedTree.getDetectedTreeBlocks(), tool.containsEnchantment(Enchantment.SILK_TOUCH));
        if (ConfigurationManager.Setting.PROTECT_TOOL.getBoolean() && !ItemUtils.hasEnoughDurability(tool, toolDamage)) {
            return;
        }

        // Trigger fall event
        TreeFallEvent treeFallEvent = new TreeFallEvent(player, detectedTree);
        Bukkit.getPluginManager().callEvent(treeFallEvent);
        if (treeFallEvent.isCancelled())
            return;

        // Valid tree and meets all conditions past this point
        event.setCancelled(true);

        detectedTree.getDetectedTreeBlocks().sortAndLimit(maxLogBlocksAllowed);

        choppingManager.cooldownPlayer(player);

        // Destroy initiated block if enabled
        if (ConfigurationManager.Setting.DESTROY_INITIATED_BLOCK.getBoolean()) {
            detectedTree.getDetectedTreeBlocks().getInitialLogBlock().getBlock().setType(Material.AIR);
            detectedTree.getDetectedTreeBlocks().remove(detectedTree.getDetectedTreeBlocks().getInitialLogBlock());
        }

        boolean isCreative = player.getGameMode().equals(GameMode.CREATIVE);

        if (!isCreative) {
            new SItemStack(tool).addDamage(player, toolDamage, true);
        }

        for (ITreeBlock<Block> treeBlock : detectedTree.getDetectedTreeBlocks().getAllTreeBlocks())
            LogManager.logRemoval(player, treeBlock.getBlock());

        treeAnimationManager.runAnimation(detectedTree, player);
        treeDefinitionManager.dropTreeLoot(detectedTree.getTreeDefinition(), detectedTree.getDetectedTreeBlocks().getInitialLogBlock(), player, false, true);

        // Trigger fell event
        TreeFellEvent treeFellEvent = new TreeFellEvent(player, detectedTree);
        Bukkit.getPluginManager().callEvent(treeFellEvent);
    }

    /**
     * Checks if a player is doing a certain action required to topple a tree
     *
     * @param player The player to check
     * @return True if the check passes, otherwise false
     */
    private boolean checkToppleWhile(Player player) {
        switch (OnlyToppleWhile.fromString(ConfigurationManager.Setting.ONLY_TOPPLE_WHILE.getString())) {
            case SNEAKING:
                return player.isSneaking();
            case NOT_SNEAKING:
                return !player.isSneaking();
            default:
                return true;
        }
    }

    private short getToolDamage(TreeBlockSet<Block> treeBlocks, boolean hasSilkTouch) {
        if (!ConfigurationManager.Setting.REALISTIC_TOOL_DAMAGE.getBoolean())
            return 1;

        if (ConfigurationManager.Setting.APPLY_SILK_TOUCH_TOOL_DAMAGE.getBoolean() && hasSilkTouch) {
            return (short) treeBlocks.size();
        } else {
            return (short) treeBlocks.getLogBlocks().size();
        }
    }
}
