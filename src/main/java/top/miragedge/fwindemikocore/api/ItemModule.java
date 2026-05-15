package top.miragedge.fwindemikocore.api;

import top.miragedge.fwindemikocore.util.CraftEngineHelper;
import top.miragedge.fwindemikocore.util.ModuleLogger;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * 物品功能模块的抽象基类。
 * <p>
 * 所有自定义物品功能模块都应继承此类。它提供了标准化的生命周期管理：
 * <ul>
 *   <li>{@link #loadConfig()} - 从独立 YAML 文件加载模块配置</li>
 *   <li>{@link #register()} - 注册 Bukkit 事件监听器</li>
 *   <li>{@link #unregister()} - 注销事件监听器，清理资源</li>
 *   <li>{@link #reload()} - 热重载：注销→重新加载配置→重新注册</li>
 * </ul>
 * <p>
 * 基类还封装了通用的 CraftEngine 物品检查逻辑，子类只需关注业务功能实现。
 */
public abstract class ItemModule implements Listener {

    /** 插件主实例，用于调度任务、获取配置等 */
    protected final JavaPlugin plugin;
    /** 模块专用日志工具，自动带模块名前缀 */
    protected final ModuleLogger logger;
    /** 模块显示名称，用于日志输出 */
    protected final String moduleName;
    /** 配置文件相对路径（如 "items/tools/carrot-pickaxe.yml"） */
    protected final String configFilePath;
    /** 默认的 CraftEngine 物品命名空间ID */
    protected final String defaultItemId;

    /** 从配置中读取到的实际物品ID */
    protected String customItemId;
    /** 事件监听器是否已注册 */
    protected boolean registered = false;
    /** 模块是否启用（配置缺失或错误时会被禁用） */
    protected boolean enabled = true;

    /**
     * 构造一个物品功能模块。
     *
     * @param plugin        插件主实例
     * @param moduleName    模块显示名称（用于日志）
     * @param configFilePath 配置文件相对路径（如 "items/tools/carrot-pickaxe.yml"）
     * @param defaultItemId 默认 CraftEngine 物品ID（格式: namespace:id）
     */
    protected ItemModule(@NotNull JavaPlugin plugin, @NotNull String moduleName,
                         @NotNull String configFilePath, @NotNull String defaultItemId) {
        this.plugin = plugin;
        this.moduleName = moduleName;
        this.configFilePath = configFilePath;
        this.defaultItemId = defaultItemId;
        this.logger = new ModuleLogger(plugin, moduleName);
    }

    /**
     * 加载模块配置。子类必须实现此方法，从独立 YAML 文件读取所需参数。
     * <p>
     * 如果配置缺失或无效，应将 {@link #enabled} 设为 false，
     * 模块将不会注册事件监听器。
     */
    public abstract void loadConfig();

    /**
     * 注册 Bukkit 事件监听器。仅在模块启用且未注册时执行。
     * 子类如需注册额外监听器（如 ProtocolLib 包监听器），应重写此方法并在最后调用 super.register()。
     */
    public void register() {
        if (!registered && enabled) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            registered = true;
            logger.eventRegistered();
        }
    }

    /**
     * 注销所有事件监听器。插件禁用或重载时调用。
     * 子类如有额外监听器，应重写此方法并先执行自定义清理，再调用 super.unregister()。
     */
    public void unregister() {
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
            logger.eventUnregistered();
        }
    }

    /**
     * 热重载模块。依次执行：注销 → 重新加载配置 → 重新注册。
     * 由主类的 reloadPlugin() 统一调用。
     */
    public void reload() {
        unregister();
        loadConfig();
        register();
    }

    /**
     * 检查 CraftEngine 是否已加载。
     * 由主类在插件启用时调用，仅输出状态日志，不做物品存在性验证。
     * <p>
     * 物品存在性改为在运行时通过 {@link #isHoldingValidTool(ItemStack)} 动态检测，
     * 避免 CraftEngine 版本差异导致的 NoSuchMethodError。
     */
    public void checkCraftEngineLoaded() {
        if (!CraftEngineHelper.isAvailable()) {
            logger.craftEngineWaiting();
            return;
        }
        logger.craftEngineLoaded();
        logger.info("自定义物品将在运行时动态检测: " + customItemId);
    }

    /**
     * 判断 CraftEngine 插件是否可用。
     *
     * @return true 如果 CraftEngine 插件已加载
     */
    public boolean isCraftEngineLoaded() {
        return CraftEngineHelper.isAvailable();
    }

    /**
     * 检查玩家主手是否持有本模块对应的自定义物品。
     * <p>
     * 这是运行时检测方法，在玩家触发事件时调用。
     * 使用 try-catch 包裹，兼容不同版本的 CraftEngine API。
     *
     * @param itemStack 玩家主手的物品
     * @return true 如果持有的是本模块配置的 CraftEngine 自定义物品
     */
    public boolean isHoldingValidTool(@NotNull ItemStack itemStack) {
        if (itemStack.getType() == org.bukkit.Material.AIR) {
            return false;
        }
        return CraftEngineHelper.isCustomItem(itemStack, customItemId);
    }

    /**
     * 判断模块是否处于启用状态。
     *
     * @return true 如果模块配置正确且已启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取模块显示名称。
     *
     * @return 模块名称
     */
    public @NotNull String getModuleName() {
        return moduleName;
    }
}
