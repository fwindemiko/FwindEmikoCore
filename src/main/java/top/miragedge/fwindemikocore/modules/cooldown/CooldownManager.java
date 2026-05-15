package top.miragedge.fwindemikocore.modules.cooldown;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家技能冷却管理器。
 * <p>
 * 为每个玩家独立追踪技能使用时间，支持查询剩余冷却时间。
 * 使用 {@link ConcurrentHashMap} 保证线程安全。
 * <p>
 * 典型使用场景：武器右键技能、主动道具等需要冷却时间的功能。
 */
public final class CooldownManager {

    /** 玩家UUID到上次使用时间的映射 */
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    /** 冷却时间（秒），小于等于0表示无冷却 */
    private final int cooldownSeconds;

    /**
     * 创建冷却管理器。
     *
     * @param cooldownSeconds 冷却时间（秒）
     */
    public CooldownManager(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    /**
     * 判断玩家是否处于冷却中。
     *
     * @param player 目标玩家
     * @return true 如果还在冷却时间内
     */
    public boolean isOnCooldown(@NotNull Player player) {
        return getRemainingTime(player) > 0;
    }

    /**
     * 获取玩家剩余的冷却时间（毫秒）。
     *
     * @param player 目标玩家
     * @return 剩余毫秒数，0 表示不在冷却中
     */
    public long getRemainingTime(@NotNull Player player) {
        if (cooldownSeconds <= 0) {
            return 0;
        }
        long lastUse = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long elapsed = System.currentTimeMillis() - lastUse;
        long remaining = (cooldownSeconds * 1000L) - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * 获取玩家剩余的冷却时间（秒），向上取整。
     *
     * @param player 目标玩家
     * @return 剩余秒数
     */
    public int getRemainingSeconds(@NotNull Player player) {
        return (int) Math.ceil(getRemainingTime(player) / 1000.0);
    }

    /**
     * 设置玩家进入冷却状态（记录当前时间为上次使用时间）。
     *
     * @param player 目标玩家
     */
    public void setCooldown(@NotNull Player player) {
        if (cooldownSeconds > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    /**
     * 重置玩家的冷却时间（使其可以立即再次使用）。
     *
     * @param player 目标玩家
     */
    public void resetCooldown(@NotNull Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    /** 清除所有玩家的冷却记录。通常在插件禁用时调用。 */
    public void clearAll() {
        cooldowns.clear();
    }

    /**
     * 获取配置的冷却时间。
     *
     * @return 冷却时间（秒）
     */
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }
}
