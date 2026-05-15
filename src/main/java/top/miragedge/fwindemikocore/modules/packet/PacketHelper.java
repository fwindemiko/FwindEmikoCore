package top.miragedge.fwindemikocore.modules.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ProtocolLib 数据包操作的静态工具类。
 * <p>
 * 封装了 ProtocolLib 的常用操作：初始化、创建数据包、发送数据包、
 * 注册/注销包监听器等。所有方法均为静态，使用前必须先调用 {@link #init()}。
 * <p>
 * 如果服务器未安装 ProtocolLib，所有方法会安全地空操作，不会抛出异常。
 */
public final class PacketHelper {

    private PacketHelper() {
        // 禁止实例化
    }

    /** ProtocolLib 的协议管理器实例 */
    private static ProtocolManager protocolManager;

    /**
     * 初始化 PacketHelper，获取 ProtocolManager 实例。
     * 在插件 onEnable 时调用一次即可。
     */
    public static void init() {
        protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /**
     * 判断 ProtocolLib 是否可用。
     *
     * @return true 如果 ProtocolLib 已加载且初始化成功
     */
    public static boolean isAvailable() {
        return protocolManager != null;
    }

    /**
     * 获取 ProtocolManager 实例。
     *
     * @return ProtocolManager，或 null 如果未初始化
     */
    public static @Nullable ProtocolManager getManager() {
        return protocolManager;
    }

    /**
     * 向指定玩家发送服务器端数据包。
     *
     * @param player 目标玩家
     * @param packet 要发送的数据包
     */
    public static void sendPacket(@NotNull Player player, @NotNull PacketContainer packet) {
        if (protocolManager == null) {
            return;
        }
        try {
            protocolManager.sendServerPacket(player, packet);
        } catch (Exception e) {
            // 静默处理发送失败，避免影响主流程
        }
    }

    /**
     * 向所有在线玩家广播数据包。
     *
     * @param packet 要广播的数据包
     */
    public static void broadcastPacket(@NotNull PacketContainer packet) {
        if (protocolManager == null) {
            return;
        }
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            sendPacket(player, packet);
        }
    }

    /**
     * 创建指定类型的空数据包。
     *
     * @param type 数据包类型
     * @return 新的 PacketContainer 实例
     */
    public static @NotNull PacketContainer createPacket(@NotNull PacketType type) {
        return protocolManager != null
                ? protocolManager.createPacket(type)
                : new PacketContainer(type);
    }

    /**
     * 创建一个包监听器适配器。
     *
     * @param plugin  插件实例
     * @param type    要监听的包类型
     * @param handler 包处理回调
     * @return PacketAdapter 实例
     */
    public static @NotNull PacketAdapter createListener(@NotNull JavaPlugin plugin,
                                                         @NotNull PacketType type,
                                                         @NotNull PacketHandler handler) {
        return new PacketAdapter(plugin, type) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handler.onPacketReceiving(event);
            }

            @Override
            public void onPacketSending(PacketEvent event) {
                handler.onPacketSending(event);
            }
        };
    }

    /**
     * 注册一个包监听器。
     *
     * @param adapter 要注册的监听器
     */
    public static void registerListener(@NotNull PacketAdapter adapter) {
        if (protocolManager != null) {
            protocolManager.addPacketListener(adapter);
        }
    }

    /**
     * 注销一个包监听器。
     *
     * @param adapter 要注销的监听器
     */
    public static void removeListener(@NotNull PacketAdapter adapter) {
        if (protocolManager != null) {
            protocolManager.removePacketListener(adapter);
        }
    }

    /**
     * 包处理回调接口。
     * <p>
     * 提供默认空实现，使用者只需重写需要的方法。
     */
    public interface PacketHandler {
        /** 接收到客户端发来的包时触发 */
        default void onPacketReceiving(PacketEvent event) {}
        /** 向客户端发送包时触发 */
        default void onPacketSending(PacketEvent event) {}
    }
}
