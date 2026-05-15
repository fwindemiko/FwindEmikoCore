package top.miragedge.fwindemikocore.modules.scale;

import top.miragedge.fwindemikocore.api.ItemModule;
import top.miragedge.fwindemikocore.modules.effects.EntityEffects;
import top.miragedge.fwindemikocore.modules.packet.PlayerScalePacket;
import top.miragedge.fwindemikocore.util.ConfigHelper;
import top.miragedge.fwindemikocore.util.Msg;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家碰撞箱缩放功能模块。
 * <p>
 * 提供临时改变玩家尺寸（scale）的能力，同时影响：
 * <ul>
 *   <li><b>服务端碰撞箱</b>：通过 {@link Attribute#SCALE} 属性修饰符真实改变</li>
 *   <li><b>客户端显示</b>：通过 ProtocolLib 广播 ENTITY_METADATA 数据包同步</li>
 * </ul>
 * <p>
 * 此模块可以被其他物品模块调用，实现"使用某物品后变小/变大"的效果。
 * 也支持通过配置文件独立运行（绑定到特定 CraftEngine 物品）。
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * PlayerScaleModule scaleModule = new PlayerScaleModule(plugin);
 * scaleModule.loadConfig();
 * scaleModule.applyScale(player, 0.5f, 10); // 玩家缩小到50%，持续10秒
 * </pre>
 */
public class PlayerScaleModule extends ItemModule {

    /**
     * 记录每个玩家原始的 scale 基值，用于恢复。
     * Key: 玩家UUID, Value: 原始scale值
     */
    private final Map<UUID, Double> originalScales = new HashMap<>();

    /** 是否启用独立物品触发模式（从配置读取） */
    private boolean itemTriggerEnabled;
    /** 默认缩放比例（从配置读取） */
    private float defaultScale;
    /** 默认持续时间（从配置读取） */
    private int defaultDuration;
    /** 默认冷却时间（从配置读取） */
    private int defaultCooldown;

    /** 模块独立配置 */
    private YamlConfiguration itemConfig;

    /**
     * 构造玩家缩放模块。
     *
     * @param plugin 插件主实例
     */
    public PlayerScaleModule(JavaPlugin plugin) {
        super(plugin, "玩家缩放", "items/modules/player-scale.yml", "miragedge_items:scale_changer");
    }

    @Override
    public void loadConfig() {
        this.itemConfig = ConfigHelper.loadItemConfig(plugin, configFilePath);
        ConfigurationSection config = itemConfig;

        this.customItemId = ConfigHelper.getItemId(config, "item-id", defaultItemId, logger);
        this.itemTriggerEnabled = config.getBoolean("item-trigger-enabled", false);
        this.defaultScale = (float) ConfigHelper.getClampedDouble(config, "default-scale", 0.5, 0.1, 10.0);
        this.defaultDuration = ConfigHelper.getPositiveInt(config, "default-duration", 10);
        this.defaultCooldown = ConfigHelper.getPositiveInt(config, "default-cooldown", 30);
    }

    /**
     * 对指定玩家应用临时缩放效果。
     * <p>
     * 此方法会同时修改服务端属性（影响碰撞箱）和客户端显示。
     *
     * @param player   目标玩家
     * @param scale    缩放比例（1.0 = 正常，0.5 = 一半，2.0 = 两倍）
     * @param duration 持续时间（秒）
     */
    public void applyScale(@org.jetbrains.annotations.NotNull Player player, float scale, int duration) {
        // 保存原始值（如果尚未保存）
        originalScales.putIfAbsent(player.getUniqueId(), player.getAttribute(Attribute.SCALE).getBaseValue());

        // 计算相对于原始值的修饰量
        double modifierValue = scale - 1.0; // MULTIPLY_SCALAR_1 模式下: base * (1 + value)

        NamespacedKey key = new NamespacedKey(plugin, "fec_player_scale");

        // 应用服务端属性修饰符（真实改变碰撞箱）
        EntityEffects.applyAttributeModifier(player, Attribute.SCALE, key, modifierValue, AttributeModifier.Operation.MULTIPLY_SCALAR_1);

        // 广播客户端数据包同步显示
        PlayerScalePacket.broadcastScaleUpdate(player, scale);

        // 播放效果音效
        if (scale < 1.0f) {
            EntityEffects.playSound(player, "entity.illusioner.mirror_move", 0.6F, 1.2F);
        } else {
            EntityEffects.playSound(player, "entity.illusioner.cast_spell", 0.6F, 0.8F);
        }

        // 发送 ActionBar 提示（使用 MiniMessage 格式）
        int percent = (int) (scale * 100);
        Msg.actionBar(player, "<aqua>体型变化: <yellow>" + percent + "% <aqua>持续 <yellow>" + duration + "<aqua> 秒");

        // 定时恢复
        new BukkitRunnable() {
            @Override
            public void run() {
                restoreScale(player);
            }
        }.runTaskLater(plugin, duration * 20L);
    }

    /**
     * 恢复指定玩家的原始尺寸。
     *
     * @param player 目标玩家
     */
    public void restoreScale(@org.jetbrains.annotations.NotNull Player player) {
        NamespacedKey key = new NamespacedKey(plugin, "fec_player_scale");
        EntityEffects.removeAttributeModifier(player, Attribute.SCALE, key);

        // 恢复客户端显示
        double originalScale = originalScales.getOrDefault(player.getUniqueId(), 1.0);
        PlayerScalePacket.broadcastScaleUpdate(player, (float) originalScale);

        // 清理记录
        originalScales.remove(player.getUniqueId());

        EntityEffects.playSound(player, "entity.enderman.teleport", 0.4F, 1.0F);
        Msg.actionBar(player, "<green>体型已恢复");
    }

    /**
     * 判断玩家是否正在被缩放效果影响。
     *
     * @param player 目标玩家
     * @return true 如果玩家当前有活跃的缩放修饰符
     */
    public boolean isScaled(@org.jetbrains.annotations.NotNull Player player) {
        return originalScales.containsKey(player.getUniqueId());
    }

    /** 新玩家加入时，同步当前所有被缩放玩家的状态给新玩家 */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player newPlayer = event.getPlayer();
        for (Map.Entry<UUID, Double> entry : originalScales.entrySet()) {
            Player scaledPlayer = plugin.getServer().getPlayer(entry.getKey());
            if (scaledPlayer != null && scaledPlayer.isOnline()) {
                // 新玩家需要看到被缩放玩家的当前尺寸
                double currentScale = scaledPlayer.getAttribute(Attribute.SCALE).getValue();
                PlayerScalePacket.sendScaleUpdate(scaledPlayer, newPlayer, (float) currentScale);
            }
        }
    }

    /** 玩家退出时清理记录 */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (originalScales.containsKey(player.getUniqueId())) {
            // 强制移除修饰符，防止残留
            NamespacedKey key = new NamespacedKey(plugin, "fec_player_scale");
            EntityEffects.removeAttributeModifier(player, Attribute.SCALE, key);
            originalScales.remove(player.getUniqueId());
        }
    }

    /**
     * 获取默认缩放比例。
     *
     * @return 默认 scale 值
     */
    public float getDefaultScale() {
        return defaultScale;
    }

    /**
     * 获取默认持续时间。
     *
     * @return 默认持续时间（秒）
     */
    public int getDefaultDuration() {
        return defaultDuration;
    }

    /**
     * 获取默认冷却时间。
     *
     * @return 默认冷却时间（秒）
     */
    public int getDefaultCooldown() {
        return defaultCooldown;
    }
}
