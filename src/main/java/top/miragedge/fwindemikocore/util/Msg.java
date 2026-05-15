package top.miragedge.fwindemikocore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * MiniMessage 消息格式化工具类。
 * <p>
 * 支持两种颜色编码格式：
 * <ul>
 *   <li><b>传统 & 颜色码</b>：如 &c红色 &a绿色</li>
 *   <li><b>MiniMessage 标签</b>：如 <red>红色 <green>绿色 <gradient:red:blue>渐变色</li>
 * </ul>
 * <p>
 * 所有方法均为静态，无需实例化。典型使用：
 * <pre>
 * Msg.send(player, "&a成功！ &7冷却: &e" + seconds + "s");
 * Msg.send(player, "<gradient:gold:yellow>★ 传说武器 ★");
 * Msg.broadcast("&c全服公告: &f" + message);
 * </pre>
 */
public final class Msg {

    private Msg() {
        // 禁止实例化
    }

    /** MiniMessage 解析器实例 */
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * 将包含 & 颜色码或 MiniMessage 标签的字符串解析为 Adventure Component。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>先解析传统 & 颜色码（&0-&f, &k-&r）</li>
     *   <li>再解析 MiniMessage 标签（如 <red>, <gradient:...>）</li>
     * </ol>
     *
     * @param text 原始消息文本
     * @return 解析后的 Component
     */
    public static @NotNull Component parse(@NotNull String text) {
        // 步骤1：将传统 & 颜色码转换为 § 格式，再序列化为 Component
        Component legacyParsed = LegacyComponentSerializer.legacyAmpersand().deserialize(text);

        // 步骤2：将 Component 序列化为字符串（包含 § 颜色码），再交给 MiniMessage 解析
        // 这样可以同时支持 & 颜色码和 MiniMessage 标签
        String serialized = LegacyComponentSerializer.legacySection().serialize(legacyParsed);

        // 步骤3：使用 MiniMessage 解析（支持 <red>, <gradient:...> 等标签）
        return MINI_MESSAGE.deserialize(serialized);
    }

    /**
     * 仅解析 MiniMessage 标签（不处理 & 颜色码）。
     *
     * @param text 包含 MiniMessage 标签的文本
     * @return 解析后的 Component
     */
    public static @NotNull Component mini(@NotNull String text) {
        return MINI_MESSAGE.deserialize(text);
    }

    /**
     * 仅解析传统 & 颜色码（不处理 MiniMessage 标签）。
     *
     * @param text 包含 & 颜色码的文本
     * @return 解析后的 Component
     */
    public static @NotNull Component legacy(@NotNull String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    /**
     * 向玩家发送消息（支持 & 颜色码和 MiniMessage）。
     *
     * @param player  目标玩家
     * @param message 消息内容
     */
    public static void send(@NotNull Player player, @NotNull String message) {
        player.sendMessage(parse(message));
    }

    /**
     * 向 CommandSender 发送消息（支持 & 颜色码和 MiniMessage）。
     *
     * @param sender  命令发送者
     * @param message 消息内容
     */
    public static void send(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(parse(message));
    }

    /**
     * 向玩家发送 ActionBar 消息（支持 & 颜色码和 MiniMessage）。
     *
     * @param player  目标玩家
     * @param message 消息内容
     */
    public static void actionBar(@NotNull Player player, @NotNull String message) {
        player.sendActionBar(parse(message));
    }

    /**
     * 向所有在线玩家广播消息（支持 & 颜色码和 MiniMessage）。
     *
     * @param message 消息内容
     */
    public static void broadcast(@NotNull String message) {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            send(player, message);
        }
    }

    /**
     * 向所有在线玩家广播 ActionBar（支持 & 颜色码和 MiniMessage）。
     *
     * @param message 消息内容
     */
    public static void broadcastActionBar(@NotNull String message) {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            actionBar(player, message);
        }
    }

    /**
     * 发送带标题的消息（支持 & 颜色码和 MiniMessage）。
     *
     * @param player    目标玩家
     * @param title     标题
     * @param subtitle  副标题
     * @param fadeIn    淡入时间（tick）
     * @param stay      停留时间（tick）
     * @param fadeOut   淡出时间（tick）
     */
    public static void title(@NotNull Player player, @NotNull String title,
                              @NotNull String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(net.kyori.adventure.title.Title.title(
            parse(title),
            parse(subtitle),
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(fadeIn * 50L),
                java.time.Duration.ofMillis(stay * 50L),
                java.time.Duration.ofMillis(fadeOut * 50L)
            )
        ));
    }

    /**
     * 发送带标题的消息（使用默认时间：10, 70, 20 tick）。
     *
     * @param player    目标玩家
     * @param title     标题
     * @param subtitle  副标题
     */
    public static void title(@NotNull Player player, @NotNull String title, @NotNull String subtitle) {
        title(player, title, subtitle, 10, 70, 20);
    }
}
