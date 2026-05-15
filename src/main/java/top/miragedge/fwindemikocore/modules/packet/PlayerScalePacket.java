package top.miragedge.fwindemikocore.modules.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家尺寸（Scale）数据包工具。
 * <p>
 * 通过 ProtocolLib 发送 ENTITY_METADATA 数据包，修改客户端看到的玩家 scale 值。
 * Scale 索引在 Minecraft 1.21+ 中为 5，类型为 Float。
 * <p>
 * <b>注意：</b>此方法仅修改客户端显示效果。如需真实改变碰撞箱，
 * 应使用 {@link org.bukkit.attribute.Attribute#SCALE} 属性修饰符。
 * 推荐两者结合使用：服务端修改属性（影响碰撞），再广播数据包同步显示。
 */
public final class PlayerScalePacket {

    private PlayerScalePacket() {
        // 禁止实例化
    }

    /** Minecraft 实体元数据中 scale 字段的索引 */
    private static final int SCALE_INDEX = 5;

    /**
     * 向指定观察者发送目标玩家的 scale 更新数据包。
     *
     * @param target  被修改尺寸的目标玩家
     * @param viewer  接收数据包的观察者玩家
     * @param scale   新的 scale 值（1.0 = 正常尺寸）
     */
    @SuppressWarnings("removal")
    public static void sendScaleUpdate(@NotNull Player target, @NotNull Player viewer, float scale) {
        if (!PacketHelper.isAvailable()) {
            return;
        }

        PacketContainer packet = PacketHelper.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, target.getEntityId());

        List<WrappedDataValue> dataValues = new ArrayList<>();

        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Float.class);
        WrappedDataValue scaleValue = new WrappedDataValue(
                SCALE_INDEX,
                serializer,
                scale
        );
        dataValues.add(scaleValue);

        packet.getDataValueCollectionModifier().write(0, dataValues);

        PacketHelper.sendPacket(viewer, packet);
    }

    /**
     * 向所有在线玩家广播目标玩家的 scale 更新（包括目标玩家自己）。
     *
     * @param target 被修改尺寸的目标玩家
     * @param scale  新的 scale 值
     */
    public static void broadcastScaleUpdate(@NotNull Player target, float scale) {
        for (Player viewer : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target)) {
                sendScaleUpdate(target, viewer, scale);
            }
        }
        sendScaleUpdate(target, target, scale);
    }

    /**
     * 同步目标玩家当前的 scale 属性值给指定观察者。
     * 通常在新玩家加入或需要同步状态时使用。
     *
     * @param target 目标玩家
     * @param viewer 观察者玩家
     */
    public static void updateScaleForViewer(@NotNull Player target, @NotNull Player viewer) {
        AttributeInstance attr = target.getAttribute(Attribute.SCALE);
        float currentScale = attr != null ? (float) attr.getValue() : 1.0f;
        sendScaleUpdate(target, viewer, currentScale);
    }
}
