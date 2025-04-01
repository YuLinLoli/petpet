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
    /**
     * 定义一个常量COMMAND_ALL，用于表示所有可能的命令类型
     * 这个常量是通过将COMMAND、NUDGE、AT、IMAGE和COMMAND_HEAD的值进行位或操作得到的
     * 位或操作的结果是一个包含了上述所有命令类型的新值
     * 这样做是为了提供一个单一的值，可以代表所有的命令类型，便于在后续的代码中进行统一处理
     */
    public static final int COMMAND_ALL = COMMAND | NUDGE | AT | IMAGE | COMMAND_HEAD;
/**
 * 定义了一个不可变的映射表，用于将命令名称字符串与对应的权限值进行映射。
 * 该映射表主要用于快速查找特定命令名称所对应的权限值。
 *
 * 映射关系如下：
 * - "all" 对应所有权限的集合 COMMAND_ALL
 * - "command" 对应 COMMAND 权限
 * - "nudge" 对应 NUDGE 权限
 * - "at" 对应 AT 权限
 * - "image" 对应 IMAGE 权限
 * - "command_head" 对应 COMMAND_HEAD 权限
 */
public static final Map<String, Integer> COMMAND_PERMISSION_NAME_MAP = Map.of(
        "all", COMMAND_ALL,
        "command", COMMAND,
        "nudge", NUDGE,
        "at", AT,
        "image", IMAGE,
        "command_head", COMMAND_HEAD
);


//    public static final Map<String>

    // 定义编辑命令权限的常量，二进制表示为0001
    public static final int EDIT_COMMAND_PERMISSION = 0b0001;

    // 定义编辑禁用模板列表的常量，二进制表示为0010
    public static final int EDIT_DISABLE_TEMPLATE_LIST = 0b0010;

    // 定义编辑提示概率的常量，二进制表示为0100
    public static final int EDIT_NUDGE_PROBABILITY = 0b0100;

    // 定义编辑冷却时间的常量，二进制表示为1000
    public static final int EDIT_COOLDOWN_TIME = 0b1000;

    // 定义编辑所有设置的常量，通过位运算|合并上述所有设置的值
    public static final int EDIT_ALL = EDIT_COMMAND_PERMISSION | EDIT_DISABLE_TEMPLATE_LIST | EDIT_NUDGE_PROBABILITY | EDIT_COOLDOWN_TIME;
    /**
     * 编辑权限名称映射表
     *
     * 该映射表用于将字符串形式的编辑权限名称映射到其对应的整型值
     * 这种设计便于通过名称快速查找和管理不同的编辑权限
     */
    public static final Map<String, Integer> EDIT_PERMISSION_NAME_MAP = Map.of(
            "all", EDIT_ALL,
            "command_permission", EDIT_COMMAND_PERMISSION,
            "disable_template", EDIT_DISABLE_TEMPLATE_LIST,
            "nudge_probability", EDIT_NUDGE_PROBABILITY,
            "cooldown_time", EDIT_COOLDOWN_TIME
    );

    // 定义消息常量，用于表示各项功能的启用状态
    public static final String ENABLE_MESSAGE = "已启用 %s";
    // 定义消息常量，用于表示各项功能的禁用状态
    public static final String DISABLE_MESSAGE = "已禁用 %s";
    // 定义消息常量，用于表示模板的启用状态
    public static final String ENABLE_TEMPLATE_MESSAGE = "已启用模板 %s";
    // 定义消息常量，用于表示模板的禁用状态
    public static final String DISABLE_TEMPLATE_MESSAGE = "已禁用模板 %s";
    // 定义消息常量，用于表示戳一戳触发概率的更新
    public static final String PROBABILITY_MESSAGE = "戳一戳触发概率更新为 %.2f%%";
    // 定义消息常量，用于表示冷却时间的更新
    public static final String COOLDOWN_MESSAGE = "冷却时间更新为 %s";

    // 对象ID，用于唯一标识一个对象
    public final Object id;
    // QQ机器人服务实例，提供与QQ机器人的交互功能
    public final QQBotService service;
    // 命令权限，-1表示未设置，具体值代表不同的权限等级
    @Setter
    protected int commandPermission = -1;
    // 编辑权限，-1表示未设置，具体值代表不同的权限等级
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

    // 定义一个最终变量，用于存储配置文件的路径
    protected final Path configPath;

    /**
     * 构造函数，用于初始化ContactPermission对象
     *
     * @param service QQBotService实例，用于处理QQ机器人服务
     * @param id 联系人或群组的ID，用于确定配置文件和权限设置
     */
    public ContactPermission(QQBotService service, Object id) {
        // 将传入的id和service赋值给实例变量
        this.id = id;
        this.service = service;
        // 根据传入的id获取配置文件路径，并将其赋值给configPath变量
        this.configPath = getConfigFile(id);
        // 调用初始化方法，用于加载配置文件和设置权限
        init();
    }
    /**
     * 初始化方法，用于加载组权限配置
     * 此方法首先检查配置文件路径是否存在，如果不存在则直接返回
     * 如果存在，则尝试从该路径读取配置信息，并根据配置信息更新内部状态
     */
    protected void init() {
        // 检查配置文件路径是否存在，不存在则直接返回
        if (!Files.exists(configPath)) {
            return;
        }
        try {
            // 从配置文件路径读取并解析配置信息
            var config = GroupPermissionConfig.fromJsonString(Files.readString(configPath));

            // 如果配置了命令权限，则更新内部命令权限状态
            if (config.getCommandPermission() != null) {
                this.commandPermission = this.service.stringToCommandPermission(config.getCommandPermission());
            }

            // 如果配置了编辑权限，则更新内部编辑权限状态
            if (config.getEditPermission() != null) {
                this.editPermission = this.service.stringToCommandPermission(config.getEditPermission());
            }

            // 如果配置了戳一戳概率，则更新内部戳一戳概率状态
            if (config.getNudgeProbability() != null) {
                this.nudgeProbability = config.getNudgeProbability();
            }

            // 如果配置了禁用的模板ID列表，则更新内部禁用模板ID列表状态
            if (config.getDisabledTemplates() != null) {
                this.disabledTemplateIds = config.getDisabledTemplates();
            }

            // 如果配置了冷却时间，则更新内部冷却时间状态
            if (config.getCooldownTime() != null) {
                this.cooldownTime = config.getCooldownTime();
            }
        } catch (IOException e) {
            // 如果在读取配置过程中发生IO异常，则记录错误日志
            log.error("Can not load permission config: {}", configPath.toAbsolutePath());
        }
    }

    /**
     * 获取命令权限级别
     * 如果未设置特定的命令权限级别，则返回默认的组命令权限级别
     * @return 当前的命令权限级别，如果未设置，则返回默认值
     */
    public int getCommandPermission() {
        if (commandPermission == -1) return service.getDefaultGroupCommandPermission();
        return commandPermission;
    }

    /**
     * 获取编辑权限级别
     * 如果未设置特定的编辑权限级别，则返回默认的组编辑权限级别
     * @return 当前的编辑权限级别，如果未设置，则返回默认值
     */
    public int getEditPermission() {
        if (editPermission == -1) return service.getDefaultGroupEditPermission();
        return editPermission;
    }

    /**
     * 获取冷却时间
     * 如果未设置特定的冷却时间，则从配置中获取默认的组冷却时间
     * @return 当前的冷却时间，如果未设置，则返回默认值
     */
    public long getCooldownTime() {
        if (cooldownTime == -1) return service.getConfig().getGroupCooldownTime();
        return cooldownTime;
    }

    /**
     * 获取戳一戳概率
     * 如果未设置特定的戳一戳概率，则返回默认的戳一戳概率
     * @return 当前的戳一戳概率，如果未设置，则返回默认值
     */
    public float getNudgeProbability() {
        if (nudgeProbability == -1) return service.getNudgeProbability();
        return nudgeProbability;
    }
    /**
     * 获取禁用的模板ID集合
     *
     * 此方法用于返回当前禁用的模板ID集合如果尚未初始化，它会先检查编辑权限，
     * 并根据权限情况决定是否基于默认配置初始化这个集合
     *
     * @return Set<String> 禁用的模板ID集合
     */
    public Set<String> getDisabledTemplateIds() {
        // 检查disabledTemplateIds是否已初始化
        if (disabledTemplateIds == null) {
            // 获取默认的禁用模板集合
            var defaultDisabledTemplates = service.getConfig().getDisabledTemplates();
            // 检查是否具有编辑禁用模板列表的权限
            if ((getEditPermission() & EDIT_DISABLE_TEMPLATE_LIST) == 0) {
                // 如果没有权限，直接返回默认的禁用模板集合
                return defaultDisabledTemplates;
            }
            // 如果有权限，基于默认禁用模板集合初始化disabledTemplateIds，并进行后续操作
            disabledTemplateIds = new HashSet<>(defaultDisabledTemplates);
        }
        // 返回初始化后的禁用模板ID集合
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
     * 编辑命令权限或模板
     *
     * 此方法用于根据输入的权限列表或模板ID来启用或禁用相应的权限或模板
     * 它首先检查输入是否为空，如果是，则根据默认组命令权限进行修改；
     * 否则，它会解析输入的权限或模板ID，并根据当前的编辑权限进行相应的修改
     *
     * @param permissionOrId 传入权限列表或模板 ID
     * @param flag           为 true 时启用模板，false 时禁用模板
     * @return 返回修改后的权限信息或模板信息，如果未进行任何修改则返回 null
     */
    protected @Nullable String editCommandPermissionOrTemplate(String permissionOrId, boolean flag) {
        // 检查输入是否为空，如果为空，则根据默认组命令权限修改当前权限
        if (permissionOrId.isBlank()) {
            // 获取默认组命令权限
            int p = service.getDefaultGroupCommandPermission();
            // 根据flag值决定是增加权限还是减少权限
            if (flag) {
                // 如果flag为真，增加权限
                commandPermission = getCommandPermission() | p;
            } else {
                // 如果flag为假，减少权限
                commandPermission = getCommandPermission() & ~p;
            }
            // 根据修改后的权限生成并返回相应的消息
            return String.format(
                    flag ? ENABLE_MESSAGE : DISABLE_MESSAGE,
                    commandPermissionToString(commandPermission)
            );
        }

        // 解析输入的权限或模板ID，并进行相应的修改
        var tokens = permissionOrId.trim().split("[\\s|&]+");
        // 初始化一个列表，用于存储即将修改的模板ID
        var editedTemplates = new ArrayList<String>(tokens.length);
        // 检查是否具有修改禁用模板列表的权限
        boolean canEditTemplateList = (getEditPermission() & EDIT_DISABLE_TEMPLATE_LIST) != 0;
        int i = 0;
        for (String token : tokens) {
            // 尝试从命令权限名称映射中获取权限值
            Integer p = service.commandPermissionNameMap.get(token);
            // 如果未找到对应的权限值，则认为token是一个模板ID
            if (p == null) {
                // 获取与模板ID相关联的所有模板ID（可能涉及某种映射或解析机制）
                var templateIds = service.getTemplateIds(token);
                // 如果模板ID列表非空且用户有权限修改禁用模板列表，则进行修改
                if (templateIds.length != 0 && canEditTemplateList) {
                    // 获取当前禁用的模板ID列表
                    var thisDisabledTemplateIds = getDisabledTemplateIds();
                    // 根据flag的值，决定是添加还是移除模板ID到禁用列表
                    for (String templateId : templateIds) {
                        if (flag) {
                            thisDisabledTemplateIds.remove(templateId);
                        } else {
                            thisDisabledTemplateIds.add(templateId);
                        }
                        // 将修改的模板ID添加到编辑列表中
                        editedTemplates.add(templateId);
                    }
                }
                // 继续处理下一个token
                continue;
            }
            // 如果找到了对应的权限值，则将其与i进行按位或操作，累积权限
            i |= p;
        }
        // 根据编辑过的模板列表和标志位来构造消息字符串
        String templateMsg = editedTemplates.isEmpty() ? null : String.format(
                // 根据flag选择启用或禁用的模板消息格式
                flag ? ENABLE_TEMPLATE_MESSAGE : DISABLE_TEMPLATE_MESSAGE,
                // 将编辑过的模板列表用逗号连接成字符串
                String.join(", ", editedTemplates)
        );

        // 如果i为0，根据是否有编辑过的模板来决定是否返回null或仅返回模板消息
        if (i == 0) {
            if (editedTemplates.isEmpty()) {
                return null;
            }
            return templateMsg;
        }

        // 根据flag和i的值来更新命令权限
        commandPermission = flag ? getCommandPermission() | i : getCommandPermission() & ~i;

        // 构造权限更新的消息字符串
        var permissionMsg = String.format(
                // 根据flag选择启用或禁用的权限消息格式
                flag ? ENABLE_MESSAGE : DISABLE_MESSAGE,
                // 将权限值转换为字符串
                commandPermissionToString(i)
        );

        // 如果没有编辑过的模板，仅返回权限更新的消息
        if (editedTemplates.isEmpty()) {
            return permissionMsg;
        }

        // 返回既包含权限更新又包含模板消息的字符串，两者之间用换行符分隔
        return permissionMsg + "\n" + templateMsg;
    }

    /**
     * 开启指定的命令权限或模板
     *
     * 此方法用于激活或启用给定的权限或模板ID所对应的命令权限或模板
     * 它通过调用editCommandPermissionOrTemplate方法，并将enable参数设为true来实现
     *
     * @param permissionOrId 权限或模板的标识符
     * @return 返回执行结果的描述，如果执行失败可能返回null
     */
    public @Nullable String turnOnCommandPermissionOrTemplate(String permissionOrId) {
        return editCommandPermissionOrTemplate(permissionOrId, true);
    }

    /**
     * 关闭指定的命令权限或模板
     *
     * 此方法用于停用或禁用给定的权限或模板ID所对应的命令权限或模板
     * 它通过调用editCommandPermissionOrTemplate方法，并将enable参数设为false来实现
     *
     * @param permissionOrId 权限或模板的标识符
     * @return 返回执行结果的描述，如果执行失败可能返回null
     */
    public @Nullable String turnOffCommandPermissionOrTemplate(String permissionOrId) {
        return editCommandPermissionOrTemplate(permissionOrId, false);
    }

    /**
 * 处理编辑命令并执行相应的操作。
 *
 * @param command 包含操作和参数的命令字符串。格式为 "操作 参数"，例如 "on permission1" 或 "nudge_probability 0.5"。
 *                如果命令中没有空格，则整个字符串被视为操作，参数为空字符串。
 * @return 如果操作成功，则返回描述操作结果的字符串；如果编辑权限不足或操作无效，则返回 null。
 */
public @Nullable String handleEditCommand(String command) {
    // 去除命令字符串两端的空白字符
    command = command.trim();

    // 查找命令中的第一个空格位置，用于分割操作和参数
    int spaceIndex = command.indexOf(' ');

    // 提取操作部分，如果没有空格，则整个命令字符串为操作
    String operation = spaceIndex == -1 ? command : command.substring(0, spaceIndex);

    // 根据配置文件中的映射关系，将操作名称替换为标准名称（如果存在）
    operation = service.getConfig().getCommandOperationName().getOrDefault(operation, operation);

    // 提取参数部分，如果没有空格，则参数为空字符串
    String parameter = spaceIndex == -1 ? "" : command.substring(spaceIndex + 1);

    // 根据操作类型执行不同的逻辑
    switch (operation) {
        case "on":
        case "enable": {  // 启用权限或模板
            return turnOnCommandPermissionOrTemplate(parameter);
        }
        case "off":
        case "disable": {  // 禁用权限或模板
            return turnOffCommandPermissionOrTemplate(parameter);
        }
        case "nudge_probability": {  // 设置戳一戳触发概率
            float probability = this.setContactNudgeProbability(parameter);  // 调用方法设置概率
            return String.format(PROBABILITY_MESSAGE, probability * 100);  // 返回格式化的消息
        }
        case "cooldown_time": {  // 设置冷却时间
            this.setContactCooldownTime(parameter);  // 调用方法设置冷却时间
            return String.format(COOLDOWN_MESSAGE, parameter);  // 返回格式化的消息
        }
        default: {  // 如果操作类型不匹配，则返回 null
            return null;
        }
    }
}

}
