package top.miragedge.fwindemikocore.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 配置读取的静态工具类。
 * <p>
 * 提供类型安全、带默认值和范围限制的配置读取方法，
 * 避免每个模块重复编写配置解析和校验逻辑。
 * <p>
 * 支持从独立 YAML 文件加载配置（items/ 文件夹下的分类配置）。
 * 所有方法均为静态，无需实例化。
 */
public final class ConfigHelper {

    private ConfigHelper() {
        // 禁止实例化
    }

    /**
     * 从独立 YAML 文件加载配置。
     * <p>
     * 文件路径为插件数据文件夹下的相对路径，如 "items/tools/carrot-pickaxe.yml"。
     * 如果文件不存在，会尝试从插件资源中复制默认文件。
     *
     * @param plugin   插件实例
     * @param filePath 相对文件路径（如 "items/tools/carrot-pickaxe.yml"）
     * @return 加载的 YamlConfiguration，如果加载失败返回空配置
     */
    public static @NotNull YamlConfiguration loadItemConfig(@NotNull JavaPlugin plugin, @NotNull String filePath) {
        File file = new File(plugin.getDataFolder(), filePath);
        if (!file.exists()) {
            if (plugin.getResource(filePath) != null) {
                plugin.saveResource(filePath, false);
            } else {
                plugin.getLogger().warning("[配置] 找不到配置文件: " + filePath);
                return new YamlConfiguration();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * 读取并验证物品命名空间ID。
     *
     * @param section    配置节
     * @param path       配置键路径
     * @param defaultId  默认ID（格式错误时使用）
     * @param logger     模块日志工具
     * @return 有效的物品ID
     */
    public static @NotNull String getItemId(@NotNull ConfigurationSection section,
                                             @NotNull String path,
                                             @NotNull String defaultId,
                                             @NotNull ModuleLogger logger) {
        String itemId = section.getString(path, defaultId);
        if (!CraftEngineHelper.validateItemIdFormat(itemId)) {
            logger.severe("物品ID格式错误: " + itemId + "，使用默认值: " + defaultId);
            return defaultId;
        }
        return itemId;
    }

    /**
     * 读取概率值（0-100），自动限制在有效范围内。
     *
     * @param section      配置节
     * @param path         配置键路径
     * @param defaultValue 默认值
     * @return 限制在 [0, 100] 范围内的整数值
     */
    public static int getChance(@NotNull ConfigurationSection section, @NotNull String path, int defaultValue) {
        return Math.clamp(section.getInt(path, defaultValue), 0, 100);
    }

    /**
     * 读取非负整数。
     *
     * @param section      配置节
     * @param path         配置键路径
     * @param defaultValue 默认值
     * @return 大于等于 0 的整数值
     */
    public static int getPositiveInt(@NotNull ConfigurationSection section, @NotNull String path, int defaultValue) {
        return Math.max(0, section.getInt(path, defaultValue));
    }

    /**
     * 读取被限制在指定范围内的 double 值。
     *
     * @param section      配置节
     * @param path         配置键路径
     * @param defaultValue 默认值
     * @param min          最小值
     * @param max          最大值
     * @return 限制在 [min, max] 范围内的 double 值
     */
    public static double getClampedDouble(@NotNull ConfigurationSection section, @NotNull String path,
                                           double defaultValue, double min, double max) {
        return Math.clamp(section.getDouble(path, defaultValue), min, max);
    }

    /**
     * 读取非负 double 值。
     *
     * @param section      配置节
     * @param path         配置键路径
     * @param defaultValue 默认值
     * @return 大于等于 0.0 的 double 值
     */
    public static double getPositiveDouble(@NotNull ConfigurationSection section, @NotNull String path, double defaultValue) {
        return Math.max(0.0, section.getDouble(path, defaultValue));
    }

    /**
     * 读取字符串列表并转换为枚举集合。
     * <p>
     * 列表中无法解析为枚举值的字符串会被自动忽略。
     *
     * @param section    配置节
     * @param path       配置键路径
     * @param enumClass  枚举类
     * @param <T>        枚举类型
     * @return 枚举值集合，不会为 null
     */
    public static <T extends Enum<T>> @NotNull Set<T> getEnumSet(@NotNull ConfigurationSection section,
                                                                  @NotNull String path,
                                                                  @NotNull Class<T> enumClass) {
        return section.getStringList(path).stream()
                .map(name -> {
                    try {
                        return Enum.valueOf(enumClass, name.trim().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
