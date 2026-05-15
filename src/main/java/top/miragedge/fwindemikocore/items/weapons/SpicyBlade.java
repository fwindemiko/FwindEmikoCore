package top.miragedge.fwindemikocore.items.weapons;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import top.miragedge.fwindemikocore.api.ItemModule;
import top.miragedge.fwindemikocore.modules.cooldown.CooldownManager;
import top.miragedge.fwindemikocore.modules.effects.EntityEffects;
import top.miragedge.fwindemikocore.modules.packet.PacketHelper;
import top.miragedge.fwindemikocore.util.ConfigHelper;
import top.miragedge.fwindemikocore.util.Msg;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 辛辣之刃 - 武器类物品模块。
 * <p>
 * 功能：
 * <ul>
 *   <li><b>攻击效果</b>：概率点燃目标并施加减速</li>
 *   <li><b>范围失明</b>：攻击时概率使周围敌人失明</li>
 *   <li><b>右键技能</b>：临时提升攻击速度（通过 ProtocolLib 监听 USE_ITEM 包实现）</li>
 * </ul>
 * <p>
 * <b>配置文件路径：</b> items/weapons/spicy-blade.yml
 */
public class SpicyBlade extends ItemModule {

    /** 线程安全的随机数生成器 */
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    // --- 攻击效果配置 ---
    /** 点燃概率（0-100） */
    private int igniteChance;
    /** 失明效果概率（0-100） */
    private int blindChance;
    /** 点燃持续时间（秒） */
    private int igniteDuration;
    /** 减速持续时间（秒） */
    private int speedReductionDuration;
    /** 失明效果半径（格） */
    private double blindRadius;
    /** 失明持续时间（秒） */
    private int blindDuration;

    // --- 攻速技能配置 ---
    /** 是否启用攻速提升技能 */
    private boolean attackSpeedSkillEnabled;
    /** 攻速提升百分比（0.4 = 40%） */
    private double attackSpeedIncrease;
    /** 攻速提升持续时间（秒） */
    private int attackSpeedDuration;
    /** 攻速技能冷却时间（秒） */
    private int attackSpeedCooldown;

    /** 技能冷却管理器 */
    private CooldownManager cooldownManager;
    /** ProtocolLib 包监听器（右键技能用） */
    private PacketAdapter useItemPacketAdapter;

    /** 模块独立配置 */
    private YamlConfiguration itemConfig;

    /**
     * 构造辛辣之刃模块。
     *
     * @param plugin 插件主实例
     */
    public SpicyBlade(JavaPlugin plugin) {
        super(plugin, "辛辣之刃", "items/weapons/spicy-blade.yml", "miragedge_items:spicy_blade");
    }

    @Override
    public void loadConfig() {
        this.itemConfig = ConfigHelper.loadItemConfig(plugin, configFilePath);
        ConfigurationSection config = itemConfig;

        this.customItemId = ConfigHelper.getItemId(config, "item-id", defaultItemId, logger);
        this.igniteChance = ConfigHelper.getChance(config, "ignite-chance", 80);
        this.blindChance = ConfigHelper.getChance(config, "blind-chance", 100);
        this.igniteDuration = ConfigHelper.getPositiveInt(config, "ignite-duration", 5);
        this.speedReductionDuration = ConfigHelper.getPositiveInt(config, "speed-reduction-duration", 5);
        this.blindRadius = ConfigHelper.getPositiveDouble(config, "blind-radius", 3.0);
        this.blindDuration = ConfigHelper.getPositiveInt(config, "blind-duration", 1);

        this.attackSpeedSkillEnabled = config.getBoolean("attack-speed-skill-enabled", false);
        this.attackSpeedIncrease = ConfigHelper.getClampedDouble(config, "attack-speed-increase-percent", 0.4, 0.0, 10.0);
        this.attackSpeedDuration = ConfigHelper.getPositiveInt(config, "attack-speed-duration", 10);
        this.attackSpeedCooldown = ConfigHelper.getPositiveInt(config, "attack-speed-cooldown", 18);

        this.cooldownManager = new CooldownManager(attackSpeedCooldown);
    }

    /**
     * 注册事件监听器。
     * <p>
     * 如果攻速技能启用且 ProtocolLib 可用，还会注册 USE_ITEM 包监听器。
     */
    @Override
    public void register() {
        super.register();
        if (enabled && attackSpeedSkillEnabled && PacketHelper.isAvailable()) {
            registerPacketListener();
        }
    }

    /**
     * 注销所有监听器。
     * <p>
     * 包括 Bukkit 事件监听器和 ProtocolLib 包监听器。
     */
    @Override
    public void unregister() {
        super.unregister();
        unregisterPacketListener();
    }

    /** 注册 ProtocolLib USE_ITEM 包监听器，用于捕获右键使用物品事件 */
    private void registerPacketListener() {
        useItemPacketAdapter = PacketHelper.createListener(plugin, PacketType.Play.Client.USE_ITEM, new PacketHelper.PacketHandler() {
            @Override
            public void onPacketReceiving(com.comphenix.protocol.events.PacketEvent event) {
                onUseItemPacket(event);
            }
        });
        PacketHelper.registerListener(useItemPacketAdapter);
    }

    /** 注销 ProtocolLib 包监听器 */
    private void unregisterPacketListener() {
        if (useItemPacketAdapter != null) {
            PacketHelper.removeListener(useItemPacketAdapter);
            useItemPacketAdapter = null;
        }
    }

    /**
     * 处理 USE_ITEM 包事件（右键技能触发）。
     *
     * @param event ProtocolLib 包事件
     */
    private void onUseItemPacket(com.comphenix.protocol.events.PacketEvent event) {
        if (event.isServerPacket()) return;

        Player player = event.getPlayer();
        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (!isHoldingValidTool(weapon)) return;
        if (!attackSpeedSkillEnabled) return;

        if (cooldownManager.isOnCooldown(player)) {
            int secondsLeft = cooldownManager.getRemainingSeconds(player);
            Msg.actionBar(player, "<red>技能冷却中: <yellow>" + secondsLeft + "s");
            return;
        }

        applyAttackSpeedBoost(player);
        cooldownManager.setCooldown(player);
        Msg.actionBar(player, "<green>攻速提升 <yellow>" + (int)(attackSpeedIncrease * 100) + "% <green>持续 <yellow>" + attackSpeedDuration + "<green> 秒");
    }

    /** Bukkit 右键交互事件占位（ProtocolLib 版本优先处理） */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 右键技能由 ProtocolLib 包监听器处理，此方法保留用于未来扩展
    }

    /**
     * 监听实体被攻击事件。
     * <p>
     * 当玩家使用辛辣之刃攻击时，触发点燃和范围失明效果。
     *
     * @param event 实体伤害事件
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (event.isCancelled()) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        Player player = (Player) event.getDamager();
        Entity target = event.getEntity();
        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (!isHoldingValidTool(weapon)) return;

        if (random.nextInt(100) < igniteChance) {
            igniteTarget(target);
        }

        applyBlindEffect(player, target);
    }

    /** 玩家退出时清理其攻速修饰符 */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        EntityEffects.removeAttributeModifierByKey(event.getPlayer(), Attribute.ATTACK_SPEED, "spicy_blade_attack_speed");
    }

    /** 应用攻速提升效果 */
    private void applyAttackSpeedBoost(Player player) {
        NamespacedKey key = new NamespacedKey(plugin, "spicy_blade_attack_speed");
        EntityEffects.temporaryAttributeModifier(
            plugin, player, Attribute.ATTACK_SPEED, key,
            attackSpeedIncrease, AttributeModifier.Operation.MULTIPLY_SCALAR_1,
            attackSpeedDuration,
            () -> EntityEffects.playSound(player, "entity.enderman.teleport", 0.4F, 0.7F)
        );
        EntityEffects.playSound(player, "entity.arrow.shoot", 0.5F, 1.5F);
    }

    /** 点燃目标并施加减速 */
    private void igniteTarget(Entity target) {
        if (target instanceof LivingEntity) {
            LivingEntity livingTarget = (LivingEntity) target;
            EntityEffects.ignite(livingTarget, igniteDuration);
            EntityEffects.applySlowness(livingTarget, speedReductionDuration, 1);
        }
    }

    /** 对主目标周围的其他敌人施加失明效果 */
    private void applyBlindEffect(Player player, Entity primaryTarget) {
        if (random.nextInt(100) >= blindChance) return;

        List<Entity> nearbyEntities = player.getNearbyEntities(blindRadius, blindRadius, blindRadius);
        for (Entity entity : nearbyEntities) {
            if (entity.equals(player) || entity.equals(primaryTarget)) continue;
            if (entity instanceof LivingEntity) {
                EntityEffects.applyBlindness((LivingEntity) entity, blindDuration);
            }
        }
    }
}
