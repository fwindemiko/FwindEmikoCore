package top.miragedge.fwindemikocore;

import top.miragedge.fwindemikocore.api.ItemModule;
import top.miragedge.fwindemikocore.command.MainCommand;
import top.miragedge.fwindemikocore.items.tools.CarrotPickAxe;
import top.miragedge.fwindemikocore.items.weapons.SpicyBlade;
import top.miragedge.fwindemikocore.modules.packet.PacketHelper;
import top.miragedge.fwindemikocore.modules.scale.PlayerScaleModule;
import top.miragedge.fwindemikocore.util.CraftEngineHelper;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * FwindEmikoCore 插件主类。
 * <p>
 * 负责插件生命周期管理、模块注册、CraftEngine 兼容性检查。
 * <p>
 * <b>模块管理：</b>
 * <ul>
 *   <li>所有功能模块继承 {@link ItemModule}，通过 {@link #registerModule(ItemModule)} 注册</li>
 *   <li>支持热重载：/fec reload 会重新加载所有模块配置</li>
 *   <li>CraftEngine 延迟加载：如果 CraftEngine 尚未加载，会启动定时检查任务</li>
 * </ul>
 * <p>
 * <b>当前包含的模块：</b>
 * <ul>
 *   <li>{@link CarrotPickAxe} - 胡萝卜镐（工具）</li>
 *   <li>{@link SpicyBlade} - 辛辣之刃（武器）</li>
 *   <li>{@link PlayerScaleModule} - 玩家缩放（可被其他模块调用）</li>
 * </ul>
 */
public class FwindEmikoCore extends JavaPlugin {

    /** 已注册的功能模块列表 */
    private final List<ItemModule> modules = new ArrayList<>();

    /** 玩家缩放模块实例（供其他模块调用） */
    private PlayerScaleModule scaleModule;

    /**
     * 插件启用时调用。
     * <p>
     * 执行顺序：保存默认配置 → 初始化工具类 → 注册命令 → 加载模块 → 检查 CraftEngine
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        CraftEngineHelper.init(this);
        PacketHelper.init();

        MainCommand mainCommand = new MainCommand(this);
        getCommand("fwindemikocore").setExecutor(mainCommand);
        getCommand("fwindemikocore").setTabCompleter(mainCommand);

        initializeModules();
        scheduleCraftEngineCheck();

        getLogger().info("[核心] 插件已启用！");
    }

    /**
     * 插件禁用时调用。
     * <p>
     * 注销所有模块的事件监听器，清理资源。
     */
    @Override
    public void onDisable() {
        shutdownModules();
        getLogger().info("[核心] 插件已关闭！");
    }

    /**
     * 初始化所有功能模块。
     * <p>
     * 在此方法中添加新的模块注册。
     */
    private void initializeModules() {
        modules.clear();

        // 注册工具类模块
        registerModule(new CarrotPickAxe(this));

        // 注册武器类模块
        registerModule(new SpicyBlade(this));

        // 注册玩家缩放模块（可被其他模块调用）
        this.scaleModule = new PlayerScaleModule(this);
        registerModule(scaleModule);

        // 加载配置并注册事件
        for (ItemModule module : modules) {
            module.loadConfig();
            module.register();
        }
    }

    /**
     * 注册一个功能模块到插件。
     *
     * @param module 要注册的物品模块
     */
    private void registerModule(ItemModule module) {
        modules.add(module);
    }

    /**
     * 关闭所有模块。
     * <p>
     * 依次调用每个模块的 unregister() 方法，然后清空模块列表。
     */
    private void shutdownModules() {
        for (ItemModule module : modules) {
            module.unregister();
        }
        modules.clear();
        this.scaleModule = null;
    }

    /**
     * 调度 CraftEngine 加载检查任务。
     * <p>
     * 启动定时任务，每2秒检查一次 CraftEngine 是否已加载，
     * 加载完成后通知所有模块，然后取消任务。
     */
    private void scheduleCraftEngineCheck() {
        for (ItemModule module : modules) {
            getLogger().info("[" + module.getModuleName() + "] 等待CraftEngine加载...");
        }

        Bukkit.getScheduler().runTaskTimer(this, task -> {
            CraftEngineHelper.checkAvailability();
            if (!CraftEngineHelper.isAvailable()) {
                return;
            }

            // CraftEngine 已加载，通知所有模块
            for (ItemModule module : modules) {
                module.checkCraftEngineLoaded();
            }

            getLogger().info("[核心] CraftEngine 已加载，所有模块状态已更新");
            task.cancel();
        }, 20L, 40L);
    }

    /**
     * 热重载插件配置。
     * <p>
     * 由 /fec reload 命令调用。依次执行：
     * <ol>
     *   <li>重新加载 config.yml</li>
     *   <li>注销所有模块</li>
     *   <li>重新初始化所有模块</li>
     *   <li>重新检查 CraftEngine</li>
     * </ol>
     */
    public void reloadPlugin() {
        reloadConfig();
        shutdownModules();
        initializeModules();
        scheduleCraftEngineCheck();
        getLogger().info("[核心] 配置重载完成！");
    }

    /**
     * 获取已注册的模块列表副本。
     *
     * @return 模块列表
     */
    public List<ItemModule> getModules() {
        return new ArrayList<>(modules);
    }

    /**
     * 获取玩家缩放模块实例。
     * <p>
     * 其他模块可以通过此方法调用缩放功能。
     *
     * @return PlayerScaleModule 实例，或 null 如果未初始化
     */
    public PlayerScaleModule getScaleModule() {
        return scaleModule;
    }
}
