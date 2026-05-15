package top.miragedge.fwindemikocore.items.tools;

import top.miragedge.fwindemikocore.api.ItemModule;
import top.miragedge.fwindemikocore.util.ConfigHelper;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 胡萝卜镐 - 工具类物品模块。
 * <p>
 * 功能：玩家使用此 CraftEngine 自定义镐挖掘指定矿石时，有概率额外掉落胡萝卜。
 * <p>
 * <b>配置文件路径：</b> items/tools/carrot-pickaxe.yml
 */
public class CarrotPickAxe extends ItemModule {

    /** 线程安全的随机数生成器 */
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    /** 触发概率（0-100） */
    private int triggerChance;
    /** 最小额外掉落数量 */
    private int minDrops;
    /** 最大额外掉落数量 */
    private int maxDrops;
    /** 生效的方块类型集合 */
    private Set<Material> enabledBlocks;

    /** 模块独立配置 */
    private YamlConfiguration itemConfig;

    /**
     * 构造胡萝卜镐模块。
     *
     * @param plugin 插件主实例
     */
    public CarrotPickAxe(JavaPlugin plugin) {
        super(plugin, "胡萝卜镐", "items/tools/carrot-pickaxe.yml", "miragedge_items:carrot_pickaxe");
    }

    @Override
    public void loadConfig() {
        this.itemConfig = ConfigHelper.loadItemConfig(plugin, configFilePath);
        ConfigurationSection config = itemConfig;

        this.customItemId = ConfigHelper.getItemId(config, "item-id", defaultItemId, logger);
        this.triggerChance = ConfigHelper.getChance(config, "trigger-chance", 25);
        this.minDrops = ConfigHelper.getPositiveInt(config, "drops.min", 1);
        this.maxDrops = Math.max(minDrops, ConfigHelper.getPositiveInt(config, "drops.max", 3));

        this.enabledBlocks = ConfigHelper.getEnumSet(config, "enabled-blocks", Material.class);

        if (enabledBlocks.isEmpty()) {
            logger.warning("生效方块列表为空，功能已禁用！");
            enabled = false;
        }
    }

    /**
     * 监听方块破坏事件。
     * <p>
     * 当玩家使用胡萝卜镐挖掘配置的矿石时，按概率触发额外掉落胡萝卜。
     *
     * @param event 方块破坏事件
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled() || !enabled || !isCraftEngineLoaded() || triggerChance <= 0) return;

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Block block = event.getBlock();

        if (!isHoldingValidTool(tool)) return;
        if (!enabledBlocks.contains(block.getType())) return;
        if (random.nextInt(100) >= triggerChance) return;

        applyExtraDrops(event, tool);
    }

    /**
     * 应用额外掉落。取消原掉落，在原掉落物基础上增加胡萝卜。
     *
     * @param event 方块破坏事件
     * @param tool  玩家使用的工具
     */
    private void applyExtraDrops(BlockBreakEvent event, ItemStack tool) {
        List<ItemStack> drops = new ArrayList<>(event.getBlock().getDrops(tool));
        event.setDropItems(false);

        int amount = minDrops;
        if (maxDrops > minDrops) {
            amount += random.nextInt(maxDrops - minDrops + 1);
        }
        drops.add(new ItemStack(Material.CARROT, amount));

        drops.forEach(item ->
            event.getBlock().getWorld().dropItemNaturally(
                event.getBlock().getLocation().add(0.5, 0.5, 0.5), item
            )
        );
    }
}
