package top.miragedge.fwindemikocore.util;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CraftEngine 插件的静态工具类。
 * <p>
 * 提供统一的自定义物品查询、ID格式验证、物品存在性检查等功能。
 * 所有方法均为静态，使用前必须先调用 {@link #init(JavaPlugin)} 初始化。
 * <p>
 * 设计为单例静态工具类，避免每个模块重复实例化相同的辅助对象。
 */
public final class CraftEngineHelper {

    private CraftEngineHelper() {
        // 禁止实例化
    }

    /** 插件主实例，用于获取服务器信息和输出日志 */
    private static JavaPlugin plugin;
    /** CraftEngine 是否已加载的缓存标志 */
    private static boolean craftEngineAvailable = false;

    /**
     * 初始化 CraftEngineHelper。在插件 onEnable 时调用一次即可。
     *
     * @param pluginInstance 插件主实例
     */
    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        checkAvailability();
    }

    /**
     * 检查 CraftEngine 插件是否已在服务器中加载。
     * 主类的定时检查任务会周期性调用此方法更新状态。
     */
    public static void checkAvailability() {
        craftEngineAvailable = plugin.getServer().getPluginManager().getPlugin("CraftEngine") != null;
    }

    /**
     * 判断 CraftEngine 是否可用。
     *
     * @return true 如果 CraftEngine 已加载
     */
    public static boolean isAvailable() {
        return craftEngineAvailable;
    }

    /**
     * 通过物品堆栈获取 CraftEngine 自定义物品信息。
     *
     * @param itemStack Bukkit 物品堆栈
     * @return CustomItem 对象，如果不是自定义物品或 CraftEngine 未加载则返回 null
     */
    public static @Nullable CustomItem<ItemStack> getCustomItem(@NotNull ItemStack itemStack) {
        if (!craftEngineAvailable || itemStack == null) {
            return null;
        }
        try {
            return CraftEngineItems.byItemStack(itemStack);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过物品命名空间ID获取 CraftEngine 自定义物品信息。
     *
     * @param itemId 物品命名空间ID，如 "miragedge_items:carrot_pickaxe"
     * @return CustomItem 对象，如果未找到或 CraftEngine 未加载则返回 null
     */
    public static @Nullable CustomItem<ItemStack> getCustomItemById(@NotNull String itemId) {
        if (!craftEngineAvailable) {
            return null;
        }
        try {
            return CraftEngineItems.byId(Key.of(itemId));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断物品堆栈是否是指定的 CraftEngine 自定义物品。
     *
     * @param itemStack  物品堆栈
     * @param expectedId 期望的物品命名空间ID
     * @return true 如果匹配
     */
    public static boolean isCustomItem(@NotNull ItemStack itemStack, @NotNull String expectedId) {
        CustomItem<ItemStack> customItem = getCustomItem(itemStack);
        if (customItem == null) {
            return false;
        }
        return expectedId.equals(customItem.id().toString());
    }

    /**
     * 验证物品命名空间ID格式是否正确。
     * <p>
     * 正确格式为：namespace:id（小写字母、数字、下划线）
     *
     * @param itemId 待验证的ID
     * @return true 如果格式正确
     */
    public static boolean validateItemIdFormat(@NotNull String itemId) {
        return itemId.matches("^[a-z0-9_]+:[a-z0-9_]+$");
    }

    /**
     * 安全地获取物品ID，如果格式错误则返回默认值并输出错误日志。
     *
     * @param itemId    配置中读取的ID
     * @param defaultId 默认ID
     * @return 有效的物品ID
     */
    public static @NotNull String sanitizeItemId(@NotNull String itemId, @NotNull String defaultId) {
        if (validateItemIdFormat(itemId)) {
            return itemId;
        }
        plugin.getLogger().severe("[CraftEngine] 物品ID格式错误: " + itemId + "，使用默认值: " + defaultId);
        return defaultId;
    }

    /**
     * 验证自定义物品是否在 CraftEngine 中已注册，未找到时输出错误日志。
     *
     * @param moduleName 模块名称（用于日志前缀）
     * @param itemId     要验证的物品ID
     */
    public static void logItemValidation(@NotNull String moduleName, @NotNull String itemId) {
        CustomItem<ItemStack> item = getCustomItemById(itemId);
        if (item == null) {
            plugin.getLogger().severe("[" + moduleName + "] 自定义物品加载失败: " + itemId);
        }
    }
}
