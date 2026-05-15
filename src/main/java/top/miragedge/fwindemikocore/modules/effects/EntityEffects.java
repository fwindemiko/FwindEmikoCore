package top.miragedge.fwindemikocore.modules.effects;

import top.miragedge.fwindemikocore.util.Msg;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * 实体效果应用的静态工具类。
 * <p>
 * 提供药水效果、属性修饰符、音效、ActionBar 等常用实体操作的封装方法。
 * 所有方法均为静态，无需实例化。
 * <p>
 * 典型使用场景：
 * <ul>
 *   <li>武器攻击时给目标施加点燃、减速、失明效果</li>
 *   <li>技能触发时临时提升玩家属性（攻速、移速、尺寸等）</li>
 *   <li>播放音效和发送 ActionBar 提示（支持 MiniMessage 格式）</li>
 * </ul>
 */
public final class EntityEffects {

    private EntityEffects() {
        // 禁止实例化
    }

    /**
     * 点燃目标实体。
     *
     * @param target         目标生物
     * @param durationSeconds 燃烧持续时间（秒）
     */
    public static void ignite(@NotNull LivingEntity target, int durationSeconds) {
        target.setFireTicks(durationSeconds * 20);
    }

    /**
     * 给目标施加缓慢效果。
     *
     * @param target          目标生物
     * @param durationSeconds 持续时间（秒）
     * @param amplifier       效果等级（0=I, 1=II, ...）
     */
    public static void applySlowness(@NotNull LivingEntity target, int durationSeconds, int amplifier) {
        PotionEffect effect = new PotionEffect(PotionEffectType.SLOWNESS, durationSeconds * 20, amplifier, false, false);
        target.addPotionEffect(effect);
    }

    /**
     * 给目标施加失明效果。
     *
     * @param target          目标生物
     * @param durationSeconds 持续时间（秒）
     */
    public static void applyBlindness(@NotNull LivingEntity target, int durationSeconds) {
        PotionEffect effect = new PotionEffect(PotionEffectType.BLINDNESS, durationSeconds * 20, 0, false, false);
        target.addPotionEffect(effect);
    }

    /**
     * 给目标施加任意药水效果。
     *
     * @param target          目标生物
     * @param type            药水效果类型
     * @param durationSeconds 持续时间（秒）
     * @param amplifier       效果等级
     */
    public static void applyPotionEffect(@NotNull LivingEntity target, @NotNull PotionEffectType type,
                                          int durationSeconds, int amplifier) {
        PotionEffect effect = new PotionEffect(type, durationSeconds * 20, amplifier, false, false);
        target.addPotionEffect(effect);
    }

    /**
     * 给实体应用属性修饰符。如果已存在同名修饰符，会先移除旧的。
     *
     * @param entity    目标实体
     * @param attribute 要修改的属性
     * @param key       修饰符的命名空间键（用于唯一标识）
     * @param value     修饰值
     * @param operation 运算模式
     * @return true 如果应用成功
     */
    public static boolean applyAttributeModifier(@NotNull LivingEntity entity, @NotNull Attribute attribute,
                                                  @NotNull NamespacedKey key, double value,
                                                  @NotNull AttributeModifier.Operation operation) {
        AttributeInstance attr = entity.getAttribute(attribute);
        if (attr == null) {
            return false;
        }

        removeAttributeModifier(entity, attribute, key);

        AttributeModifier modifier = new AttributeModifier(key, value, operation);
        attr.addModifier(modifier);
        return true;
    }

    /**
     * 移除实体上指定命名空间键的属性修饰符。
     *
     * @param entity    目标实体
     * @param attribute 属性类型
     * @param key       修饰符的命名空间键
     */
    public static void removeAttributeModifier(@NotNull LivingEntity entity, @NotNull Attribute attribute,
                                                @NotNull NamespacedKey key) {
        AttributeInstance attr = entity.getAttribute(attribute);
        if (attr == null) {
            return;
        }
        attr.getModifiers().stream()
                .filter(modifier -> key.equals(modifier.getKey()))
                .forEach(attr::removeModifier);
    }

    /**
     * 通过键名（不含命名空间）移除属性修饰符。
     *
     * @param entity    目标实体
     * @param attribute 属性类型
     * @param keyKey    修饰符键名
     */
    public static void removeAttributeModifierByKey(@NotNull LivingEntity entity, @NotNull Attribute attribute,
                                                     @NotNull String keyKey) {
        AttributeInstance attr = entity.getAttribute(attribute);
        if (attr == null) {
            return;
        }
        attr.getModifiers().stream()
                .filter(modifier -> keyKey.equals(modifier.getKey().getKey()))
                .forEach(attr::removeModifier);
    }

    /**
     * 给实体应用临时属性修饰符，在指定时间后自动移除。
     *
     * @param plugin          插件实例（用于调度移除任务）
     * @param entity          目标实体
     * @param attribute       要修改的属性
     * @param key             修饰符的命名空间键
     * @param value           修饰值
     * @param operation       运算模式
     * @param durationSeconds 持续时间（秒）
     * @param onExpire        效果到期后的回调（可为空操作）
     */
    public static void temporaryAttributeModifier(@NotNull JavaPlugin plugin, @NotNull LivingEntity entity,
                                                   @NotNull Attribute attribute, @NotNull NamespacedKey key,
                                                   double value, @NotNull AttributeModifier.Operation operation,
                                                   int durationSeconds,
                                                   @NotNull Runnable onExpire) {
        if (!applyAttributeModifier(entity, attribute, key, value, operation)) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                removeAttributeModifier(entity, attribute, key);
                onExpire.run();
            }
        }.runTaskLater(plugin, durationSeconds * 20L);
    }

    /**
     * 给实体应用临时属性修饰符（无到期回调）。
     *
     * @see #temporaryAttributeModifier(JavaPlugin, LivingEntity, Attribute, NamespacedKey, double, AttributeModifier.Operation, int, Runnable)
     */
    public static void temporaryAttributeModifier(@NotNull JavaPlugin plugin, @NotNull LivingEntity entity,
                                                   @NotNull Attribute attribute, @NotNull NamespacedKey key,
                                                   double value, @NotNull AttributeModifier.Operation operation,
                                                   int durationSeconds) {
        temporaryAttributeModifier(plugin, entity, attribute, key, value, operation, durationSeconds, () -> {});
    }

    /**
     * 给玩家播放音效。
     *
     * @param player 目标玩家
     * @param sound  音效名称
     * @param volume 音量
     * @param pitch  音调
     */
    public static void playSound(@NotNull Player player, @NotNull String sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * 给玩家发送 ActionBar 消息（支持 & 颜色码和 MiniMessage）。
     *
     * @param player  目标玩家
     * @param message 消息内容（支持 & 颜色码和 MiniMessage 标签）
     */
    public static void sendActionBar(@NotNull Player player, @NotNull String message) {
        Msg.actionBar(player, message);
    }
}
