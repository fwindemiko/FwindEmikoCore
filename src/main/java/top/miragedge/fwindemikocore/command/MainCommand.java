package top.miragedge.fwindemikocore.command;

import top.miragedge.fwindemikocore.FwindEmikoCore;
import top.miragedge.fwindemikocore.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件主命令处理器。
 * <p>
 * 命令：/fwindemikocore 或 /fec
 * <p>
 * 支持的子命令：
 * <ul>
 *   <li><b>info</b> - 显示插件信息</li>
 *   <li><b>reload</b> - 重新加载所有配置文件（需要 op 权限）</li>
 * </ul>
 */
public class MainCommand implements CommandExecutor, TabCompleter {

    /** 插件主实例 */
    private final FwindEmikoCore plugin;

    /**
     * 构造命令处理器。
     *
     * @param plugin 插件主实例
     */
    public MainCommand(FwindEmikoCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 处理命令执行。
     *
     * @param sender  命令发送者
     * @param command 命令对象
     * @param label   使用的命令别名
     * @param args    命令参数
     * @return true 如果命令被正确处理
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "<red>用法: /fec <info|reload>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                sendInfo(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            default:
                Msg.send(sender, "<red>未知子命令。用法: /fec <info|reload>");
        }
        return true;
    }

    /**
     * 发送插件信息。
     *
     * @param sender 命令发送者
     */
    private void sendInfo(CommandSender sender) {
        Msg.send(sender, "<gold>===== FwindEmikoCore 插件信息 =====");
        Msg.send(sender, "<yellow>版本: <white>" + plugin.getDescription().getVersion());
        Msg.send(sender, "<yellow>作者: <white>" + plugin.getDescription().getAuthors());
        Msg.send(sender, "<yellow>已加载模块数: <white>" + plugin.getModules().size());
        Msg.send(sender, "<gold>==================================");
    }

    /**
     * 处理重载命令。
     *
     * @param sender 命令发送者
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("fwindemikocore.admin")) {
            Msg.send(sender, "<red>你没有权限执行此命令！");
            return;
        }
        plugin.reloadPlugin();
        Msg.send(sender, "<green>配置已重新加载！");
    }

    /**
     * 提供命令补全。
     *
     * @param sender  命令发送者
     * @param command 命令对象
     * @param alias   使用的命令别名
     * @param args    当前输入的参数
     * @return 补全建议列表
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("info");
            if (sender.hasPermission("fwindemikocore.admin")) {
                completions.add("reload");
            }
        }
        return completions;
    }
}
