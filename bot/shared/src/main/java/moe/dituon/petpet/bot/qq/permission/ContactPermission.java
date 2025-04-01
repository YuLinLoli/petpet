package moe.dituon.petpet.bot.qq.permission;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import moe.dituon.petpet.bot.qq.QQBotService;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ContactPermission {
    /**
     * 通过指令生成模板 (pet id)
     */
    public static final int COMMAND = 0b00001;
    /**
     * 通过戳一戳生成模板
     */
    public static final int NUDGE = 0b00010;
    /**
     * 通过 At 用户生成模板
     */
    public static final int AT = 0b00100;
    /**
     * 通过 发送/回复 图像生成模板
     */
    public static final int IMAGE = 0b01000;
    /**
     * 通过 head + id 生成模板 (#id)
     */
    public static final int COMMAND_HEAD = 0b10000;
    public static final int COMMAND_ALL = COMMAND | NUDGE | AT | IMAGE | COMMAND_HEAD;

    public static final Map<String, Integer> COMMAND_PERMISSION_NAME_MAP = Map.of(
            "all", COMMAND_ALL,
            "command", COMMAND,
            "nudge", NUDGE,
            "at", AT,
            "image", IMAGE,
            "command_head", COMMAND_HEAD
    );

//    public static final Map<String>

    public static final int EDIT_COMMAND_PERMISSION = 0b0001;
    public static final int EDIT_DISABLE_TEMPLATE_LIST = 0b0010;
    public static final int EDIT_NUDGE_PROBABILITY = 0b0100;
    public static final int EDIT_COOLDOWN_TIME = 0b1000;
    public static final int EDIT_ALL = EDIT_COMMAND_PERMISSION | EDIT_DISABLE_TEMPLATE_LIST | EDIT_NUDGE_PROBABILITY | EDIT_COOLDOWN_TIME;

    public static final Map<String, Integer> EDIT_PERMISSION_NAME_MAP = Map.of(
            "all", EDIT_ALL,
            "command_permission", EDIT_COMMAND_PERMISSION,
            "disable_template", EDIT_DISABLE_TEMPLATE_LIST,
            "nudge_probability", EDIT_NUDGE_PROBABILITY,
            "cooldown_time", EDIT_COOLDOWN_TIME
    );

    public static final String ENABLE_MESSAGE = "已启用 %s";
    public static final String DISABLE_MESSAGE = "已禁用 %s";
    public static final String ENABLE_TEMPLATE_MESSAGE = "已启用模板 %s";
    public static final String DISABLE_TEMPLATE_MESSAGE = "已禁用模板 %s";
    public static final String PROBABILITY_MESSAGE = "戳一戳触发概率更新为 %.2f%%";
    public static final String COOLDOWN_MESSAGE = "冷却时间更新为 %s";

    public final Object id;
    public final QQBotService service;
    @Setter
    protected int commandPermission = -1;
    @Setter
    protected int editPermission = -1;
    /**
     * 戳一戳生成概率
     */
    protected float nudgeProbability = -1f;
    protected Set<String> disabledTemplateIds = null;
    /**
     * 冷却时间 (ms)
     */
    protected long cooldownTime = -1L;

    protected final Path configPath;

    public ContactPermission(QQBotService service, Object id) {
        this.id = id;
        this.service = service;
        this.configPath = getConfigFile(id);
        init();
    }

    protected void init() {
        if (!Files.exists(configPath)) {
            return;
        }
        try {
            var config = GroupPermissionConfig.fromJsonString(Files.readString(configPath));
            if (config.getCommandPermission() != null) {
                this.commandPermission = this.service.stringToCommandPermission(config.getCommandPermission());
            }

            if (config.getEditPermission() != null) {
                this.editPermission = this.service.stringToCommandPermission(config.getEditPermission());
            }

            if (config.getNudgeProbability() != null) {
                this.nudgeProbability = config.getNudgeProbability();
            }

            if (config.getDisabledTemplates() != null) {
                this.disabledTemplateIds = config.getDisabledTemplates();
            }

            if (config.getCooldownTime() != null) {
                this.cooldownTime = config.getCooldownTime();
            }
        } catch (IOException e) {
            log.error("Can not load permission config: {}", configPath.toAbsolutePath());
        }
    }

    public int getCommandPermission() {
        if (commandPermission == -1) return service.getDefaultGroupCommandPermission();
        return commandPermission;
    }

    public int getEditPermission() {
        if (editPermission == -1) return service.getDefaultGroupEditPermission();
        return editPermission;
    }

    public long getCooldownTime() {
        if (cooldownTime == -1) return service.getConfig().getGroupCooldownTime();
        return cooldownTime;
    }

    public float getNudgeProbability() {
        if (nudgeProbability == -1) return service.getNudgeProbability();
        return nudgeProbability;
    }

    public Set<String> getDisabledTemplateIds() {
        if (disabledTemplateIds == null) {
            var defaultDisabledTemplates = service.getConfig().getDisabledTemplates();
            if ((getEditPermission() & EDIT_DISABLE_TEMPLATE_LIST) == 0) {
                return defaultDisabledTemplates;
            }
            disabledTemplateIds = new HashSet<>(defaultDisabledTemplates);
        }
        return disabledTemplateIds;
    }


    /**
     * 将命令权限转换为字符串表示形式
     * 此方法基于当前对象的命令权限属性，使用预定义的映射将权限值转换为对应的字符串
     * 主要用于UI展示或其他需要字符串表示权限的场景
     *
     * @return 权限的字符串表示，如果映射中没有找到对应的权限，则返回null
     */
    public @Nullable String commandPermissionToString() {
        return permissionToString(commandPermission, COMMAND_PERMISSION_NAME_MAP);
    }

    /**
     * 将指定的命令权限转换为字符串表示形式
     * 此方法是静态版本的权限转换方法，允许直接通过类名调用，不需要实例化对象
     * 主要用于外部需要将命令权限转换为字符串的场景
     *
     * @param permission 需要转换的命令权限值
     * @return 权限的字符串表示，如果映射中没有找到对应的权限，则返回null
     */
    public static @Nullable String commandPermissionToString(int permission) {
        return permissionToString(permission, COMMAND_PERMISSION_NAME_MAP);
    }

    /**
     * 将编辑权限转换为字符串表示形式
     * 此方法基于当前对象的编辑权限属性，使用预定义的映射将权限值转换为对应的字符串
     * 主要用于UI展示或其他需要字符串表示权限的场景
     *
     * @return 权限的字符串表示，如果映射中没有找到对应的权限，则返回null
     */
    public @Nullable String editPermissionToString() {
        return permissionToString(editPermission, EDIT_PERMISSION_NAME_MAP);
    }

    /**
     * 将指定的编辑权限转换为字符串表示形式
     * 此方法是静态版本的权限转换方法，允许直接通过类名调用，不需要实例化对象
     * 主要用于外部需要将编辑权限转换为字符串的场景
     *
     * @param permission 需要转换的编辑权限值
     * @return 权限的字符串表示，如果映射中没有找到对应的权限，则返回null
     */
    public static @Nullable String editPermissionToString(int permission) {
        return permissionToString(permission, EDIT_PERMISSION_NAME_MAP);
    }

    /**
     * 将给定的权限值转换为对应的字符串表示
     * 此方法根据提供的权限值和名称映射，生成一个描述这些权限的字符串
     * 主要用于日志记录或用户界面显示，以便更直观地展示权限信息
     *
     * @param permission 权限值，表示一组权限的位掩码如果为-1，则表示无有效权限可转换
     * @param nameMap 包含权限名称和对应权限值的映射表，用于查找权限名称
     * @return 描述权限的字符串，如果没有有效权限或无法转换，则返回null
     */
    protected static @Nullable String permissionToString(int permission, Map<String, Integer> nameMap) {
        // 检查权限值是否为无效的-1
        if (permission == -1) return null;

        // 创建一个列表，用于存储匹配到的权限名称
        var tokens = new ArrayList<String>(nameMap.size());

        // 遍历名称映射，查找匹配的权限并添加到列表中
        for (var entry : nameMap.entrySet()) {
            // 检查当前权限位是否在给定的权限值中设置
            if ((permission & entry.getValue()) != 0) {
                var name = entry.getKey();

                // 忽略名称为"all"的权限，因为它通常代表所有权限的集合，不单独列出
                if ("all".equals(name)) {
                    continue;
                }

                // 将权限名称添加到列表中
                tokens.add(name);
            }
        }

        // 使用空格连接所有权限名称，并返回结果字符串
        return String.join(" ", tokens);
    }

    /**
     * 保存配置信息到文件中
     * 此方法首先检查是否有任何配置信息被修改，如果所有配置信息都未被修改，则不执行任何操作
     * 如果有配置信息被修改，則将当前配置信息构建为一个GroupPermissionConfig对象，并尝试将其保存到配置路径中
     * 如果配置文件不存在，此方法将尝试创建必要的目录结构和文件
     * 在保存过程中，如果遇到IO异常，将记录错误日志
     */
    public void saveConfig() {
        // 检查是否所有配置信息都未被修改，如果未被修改，则不执行任何操作
        if (
                this.commandPermission == -1
                        && this.editPermission == -1
                        && this.nudgeProbability == -1
                        && this.disabledTemplateIds == null
                        && this.cooldownTime == -1
        ) {
            return;
        }
        // 构建GroupPermissionConfig对象
        var config = GroupPermissionConfig.builder()
                .commandPermission(commandPermissionToString())
                .editPermission(editPermissionToString())
                .nudgeProbability(nudgeProbability)
                .disabledTemplates(disabledTemplateIds)
                .cooldownTime(cooldownTime)
                .build();
        try {
            // 确保配置文件的路径存在，如果不存在则创建
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.getParent());
            }
            // 将配置信息保存到文件中
            Files.writeString(configPath, config.toJsonString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            // 记录保存配置信息时遇到的IO异常
            log.error("Can not save permission config: {}", configPath.toAbsolutePath(), e);
        }
    }

    /**
     * 根据ID获取配置文件路径
     *
     * @param id 配置文件对应的ID
     * @return 配置文件的路径
     */
    protected Path getConfigFile(Object id) {
        return service.getPermissionConfigPath().resolve(id.toString() + ".json");
    }

    /**
     * 开启命令权限
     *
     * @param permissions 权限字符串，如果为空则使用默认权限
     * @return 解析后的权限值
     */
    public int turnOnCommandPermission(String permissions) {
        // 根据权限字符串获取对应的权限值，如果字符串为空则获取默认权限
        int permission = permissions.isBlank() ?
                service.getDefaultGroupCommandPermission() : service.stringToCommandPermission(permissions);
        // 将当前命令权限与新权限进行位或操作，以开启新权限
        commandPermission = getCommandPermission() | permission;
        return permission;
    }

    /**
     * 关闭命令权限
     *
     * @param permissions 权限字符串，如果为空则使用默认权限
     * @return 解析后的权限值
     */
    public int turnOffCommandPermission(String permissions) {
        // 根据权限字符串获取对应的权限值，如果字符串为空则获取默认权限
        int permission = permissions.isBlank() ?
                service.getDefaultGroupCommandPermission() : service.stringToCommandPermission(permissions);
        // 将当前命令权限与新权限进行位与非操作，以关闭新权限
        commandPermission = getCommandPermission() & ~permission;
        return permission;
    }

    /**
     * 检查编辑权限
     *
     * @param permission 要检查的权限
     * @throws IllegalStateException 如果没有相应的编辑权限，则抛出异常
     */
    protected void checkEditPermission(int permission) {
        // 检查当前编辑权限是否包含指定的权限，如果不包含则抛出异常
        if ((getEditPermission() & permission) == 0) {
            //TODO i18n message
            throw new IllegalStateException("Permission denied");
        }
    }

    /**
     * 设置接触提示概率
     * 该方法用于调整用户在接触时出现提示的概率可以通过传递一个百分比字符串来设置新的概率值
     *
     * @param probability 一个表示概率的字符串，可以是一个百分比（如"50%"）或一个可转换为浮点数的字符串
     * @return 返回设置后的提示概率如果输入为空或无效，则返回当前的提示概率
     */
    public float setContactNudgeProbability(String probability) {
        // 检查是否具有修改提示概率的权限
        checkEditPermission(ContactPermission.EDIT_NUDGE_PROBABILITY);

        // 如果概率字符串为空或只包含空白字符，则不进行设置，返回当前的概率值
        if (probability.isBlank()) {
            return getNudgeProbability();
        }

        // 如果概率字符串以百分号结尾，移除百分号以进行数值解析
        if (probability.endsWith("%")) {
            probability = probability.substring(0, probability.length() - 1);
        }

        // 解析概率字符串为浮点数，并转换为0到1之间的概率值
        float p = Float.parseFloat(probability) / 100;

        // 确保概率值在0到1的范围内
        p = Math.min(Math.max(p, 0), 1);

        // 更新当前的提示概率值
        this.nudgeProbability = p;

        // 返回设置后的概率值
        return p;
    }

    /**
     * 设置联系人的冷却时间
     *
     * 此方法首先检查调用者是否具有编辑冷却时间的权限如果提供的时间字符串为空或空白，
     * 则返回当前的冷却时间否则，解析提供的时间字符串并将其设置为新的冷却时间
     *
     * @param time 表示冷却时间的字符串，需要解析并设置
     * @return 返回当前（可能是新设置的）冷却时间
     */
    public long setContactCooldownTime(String time) {
        // 检查编辑冷却时间的权限
        checkEditPermission(ContactPermission.EDIT_COOLDOWN_TIME);

        // 如果时间字符串为空或空白，返回当前的冷却时间
        if (time.isBlank()) {
            return getCooldownTime();
        }

        // 解析并设置新的冷却时间
        this.cooldownTime = service.timeParser.parse(time.trim());

        // 返回新的冷却时间
        return cooldownTime;
    }

    /**
     * 设置禁用的联系人模板ID列表
     * 此方法首先检查用户是否具有编辑禁用模板列表的权限，然后将输入的模板ID字符串
     * 转换为一个Set集合，以便在系统中使用空格分隔的模板ID字符串来更新禁用模板列表
     *
     * @param templateIds 一个以空格分隔的模板ID字符串，表示要禁用的模板ID列表
     */
    public void setContactDisabledTemplateIds(String templateIds) {
        // 检查用户是否具有编辑禁用模板列表的权限
        checkEditPermission(ContactPermission.EDIT_DISABLE_TEMPLATE_LIST);

        // 将输入的模板ID字符串按空格分隔，转换为模板ID的Set集合
        this.disabledTemplateIds = Arrays.stream(templateIds.trim().split(" +"))
                .map(service::getTemplateId)
                .collect(Collectors.toSet());
    }

    /**
     * @param permissionOrId 传入权限列表或模板 ID
     * @param flag           为 true 时启用模板，false 时禁用模板
     */
    protected @Nullable String editCommandPermissionOrTemplate(String permissionOrId, boolean flag) {
        if (permissionOrId.isBlank()) {
            int p = service.getDefaultGroupCommandPermission();
            if (flag) {
                commandPermission = getCommandPermission() | p;
            } else {
                commandPermission = getCommandPermission() & ~p;
            }
            return String.format(
                    flag ? ENABLE_MESSAGE : DISABLE_MESSAGE,
                    commandPermissionToString(commandPermission)
            );
        }

        var tokens = permissionOrId.trim().split("[\\s|&]+");
        var editedTemplates = new ArrayList<String>(tokens.length);
        boolean canEditTemplateList = (getEditPermission() & EDIT_DISABLE_TEMPLATE_LIST) != 0;
        int i = 0;
        for (String token : tokens) {
            Integer p = service.commandPermissionNameMap.get(token);
            if (p == null) {
                var templateIds = service.getTemplateIds(token);
                if (templateIds.length != 0 && canEditTemplateList) {
                    var thisDisabledTemplateIds = getDisabledTemplateIds();
                    for (String templateId : templateIds) {
                        if (flag) {
                            thisDisabledTemplateIds.remove(templateId);
                        } else {
                            thisDisabledTemplateIds.add(templateId);
                        }
                        editedTemplates.add(templateId);
                    }
                }
                continue;
            }
            i |= p;
        }
        String templateMsg = editedTemplates.isEmpty() ? null : String.format(
                flag ? ENABLE_TEMPLATE_MESSAGE : DISABLE_TEMPLATE_MESSAGE,
                String.join(", ", editedTemplates)
        );
        if (i == 0) {
            if (editedTemplates.isEmpty()) {
                return null;
            }
            return templateMsg;
        }
        commandPermission = flag ? getCommandPermission() | i : getCommandPermission() & ~i;
        var permissionMsg = String.format(
                flag ? ENABLE_MESSAGE : DISABLE_MESSAGE,
                commandPermissionToString(i)
        );
        if (editedTemplates.isEmpty()) {
            return permissionMsg;
        }
        return permissionMsg + "\n" + templateMsg;
    }

    public @Nullable String turnOnCommandPermissionOrTemplate(String permissionOrId) {
        return editCommandPermissionOrTemplate(permissionOrId, true);
    }

    public @Nullable String turnOffCommandPermissionOrTemplate(String permissionOrId) {
        return editCommandPermissionOrTemplate(permissionOrId, false);
    }

    /**
     * @return null if edit permission not success
     */
    public @Nullable String handleEditCommand(String command) {
        command = command.trim();
        int spaceIndex = command.indexOf(' ');
        String operation = spaceIndex == -1 ? command : command.substring(0, spaceIndex);
        operation = service.getConfig().getCommandOperationName().getOrDefault(operation, operation);
        String parameter = spaceIndex == -1 ? "" : command.substring(spaceIndex + 1);
        switch (operation) {
            case "on":
            case "enable": {
                return turnOnCommandPermissionOrTemplate(parameter);
            }
            case "off":
            case "disable": {
                return turnOffCommandPermissionOrTemplate(parameter);
            }
            case "nudge_probability":
                float probability = this.setContactNudgeProbability(parameter);
                return String.format(PROBABILITY_MESSAGE, probability * 100);
            case "cooldown_time":
                this.setContactCooldownTime(parameter);
                return String.format(COOLDOWN_MESSAGE, parameter);
            default:
                return null;
        }
    }
}
