package top.miragedge.fwindemikocore.util;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * 模块专用日志工具。
 * <p>
 * 为每个功能模块提供带前缀的日志输出，便于在控制台区分不同模块的日志。
 * 支持 info / warning / severe 三级日志，以及常用的预定义日志模板。
 */
public final class ModuleLogger {

    private final JavaPlugin plugin;
    private final String moduleName;

    /**
     * 创建模块日志工具。
     *
     * @param plugin     插件主实例
     * @param moduleName 模块名称，会作为日志前缀
     */
    public ModuleLogger(@NotNull JavaPlugin plugin, @NotNull String moduleName) {
        this.plugin = plugin;
        this.moduleName = moduleName;
    }

    /** 输出 info 级别日志 */
    public void info(@NotNull String message) {
        plugin.getLogger().info("[" + moduleName + "] " + message);
    }

    /** 输出 warning 级别日志 */
    public void warning(@NotNull String message) {
        plugin.getLogger().warning("[" + moduleName + "] " + message);
    }

    /** 输出 severe 级别日志 */
    public void severe(@NotNull String message) {
        plugin.getLogger().severe("[" + moduleName + "] " + message);
    }

    /** 配置节缺失时的标准错误日志 */
    public void configMissing(@NotNull String sectionName) {
        severe("配置节 [" + sectionName + "] 不存在，模块已禁用！");
    }

    /** 事件监听器注册成功的标准日志 */
    public void eventRegistered() {
        info("事件监听已注册");
    }

    /** 事件监听器注销成功的标准日志 */
    public void eventUnregistered() {
        info("事件监听已注销");
    }

    /** CraftEngine 数据加载完成的标准日志 */
    public void craftEngineLoaded() {
        info("CraftEngine 数据加载完成");
    }

    /** 等待 CraftEngine 加载的标准日志 */
    public void craftEngineWaiting() {
        info("等待 CraftEngine 加载...");
    }
}
