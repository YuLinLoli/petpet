package moe.dituon.petpet.bot.qq;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import moe.dituon.petpet.bot.BotService;
import moe.dituon.petpet.bot.TemplateExtraMetadata;
import moe.dituon.petpet.bot.qq.permission.ContactPermission;
import moe.dituon.petpet.bot.qq.permission.TimeParser;
import moe.dituon.petpet.core.element.ElementModel;
import moe.dituon.petpet.core.element.PetpetModel;
import moe.dituon.petpet.core.element.PetpetTemplateModel;
import moe.dituon.petpet.core.element.avatar.AvatarModel;
import moe.dituon.petpet.script.PetpetScriptModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 针对于 QQ 群聊的抽象 Bot 服务
 */
@Slf4j
public class QQBotService extends BotService {
    // 默认戳一戳概率
    public static final float DEFAULT_NUDGE_PROBABILITY = 0.3f;
    // 回复中用于戳一戳的关键字
    public static final String REPLY_NUDGE_KEYWORD = "[nudge]";

    // QQ机器人配置，用于存储和管理机器人各项设置
    @Getter
    protected final QQBotConfig config;
    // 命令与权限名称映射，用于管理不同命令所需的权限
    @Getter
    public final Map<String, Integer> commandPermissionNameMap;
    // 戳一戳概率，决定机器人戳一戳行为的频率
    @Getter
    protected final float nudgeProbability;
    // 图片缓存池，用于存储和管理图片资源
    @Getter
    protected final Map<Long, String> imageCachePool;
    // 默认群命令权限，用于新加入群的默认权限设置
    @Getter
    protected final int defaultGroupCommandPermission;
    // 默认群编辑权限，用于控制群成员编辑消息的权限
    @Getter
    protected final int defaultGroupEditPermission;
    // 群权限映射，用于存储和管理不同对象在群中的权限
    @Getter
    protected final Map<Object, ContactPermission> groupPermissionMap = new HashMap<>(256);
    // 权限配置路径，用于指定权限配置文件的保存位置
    @Setter
    @Getter
    protected Path permissionConfigPath = Path.of("./permissions");
    // 冷却回复戳一戳功能的开关，控制是否启用冷却时间
    @Getter
    public final boolean cooldownReplyNudge;
    // 时间解析器，用于解析和处理时间相关的操作
    @Getter
    public final TimeParser timeParser;
    // 模板期望尺寸缓存，用于存储和管理模板的期望尺寸，以优化性能
    protected final Map<PetpetModel, Map<String, Integer>> templateExpectedSizeCache = new HashMap<>(256);

    // 是否保存权限配置的标志，控制是否将权限配置保存到文件
    @Setter
    @Getter
    protected boolean savePermission = true;

    /**
     * 构造函数用于初始化QQ机器人服务
     *
     * @param config QQ机器人的配置信息，包含运行模式、概率设置、缓存大小及权限配置等
     */
    public QQBotService(QQBotConfig config) {
        // 根据配置决定是否以无头模式运行，即不需要图形界面
        if (config.getHeadless()) {
            System.setProperty("java.awt.headless", "true");
        }
        this.config = config;
        // 初始化消息推送概率
        this.nudgeProbability = config.getNudgeProbability();
        // 初始化图片缓存池，使用LinkedHashMap实现，自动移除最久未使用的图片以保持缓存大小在设定范围内
        this.imageCachePool = new LinkedHashMap<>(config.getImageCachePoolSize(), 0.75f, true) {
            @Override
            public boolean removeEldestEntry(Map.Entry eldest) {
                return size() > config.getImageCachePoolSize();
            }
        };

        // 初始化命令权限名称映射，结合默认权限和自定义权限配置
        this.commandPermissionNameMap = new HashMap<>(
                ContactPermission.COMMAND_PERMISSION_NAME_MAP.size()
                        + config.getCommandPermissionName().size()
        );
        initPermissionNameMap(config);
        // 将默认群组命令权限的字符串表示转换为实际的命令权限枚举
        this.defaultGroupCommandPermission = stringToCommandPermission(config.getDefaultGroupCommandPermission());
        // 将默认群组编辑权限的字符串表示转换为实际的编辑权限枚举
        this.defaultGroupEditPermission = stringToEditPermission(config.getDefaultGroupEditPermission());
        // 初始化时间解析器，用于处理时间相关的命令
        this.timeParser = new TimeParser(config.getTimeUnitName());
        // 根据配置判断在冷却期间是否回复戳一戳消息
        this.cooldownReplyNudge = REPLY_NUDGE_KEYWORD.equals(config.getInCoolDownMessage());

        // 添加JVM关闭钩子，确保程序退出时执行特定的清理操作
        Runtime.getRuntime().addShutdownHook(new Thread(this::onJvmExit));
    }

    /**
     * 重写添加模板方法，以支持设置默认模板
     * 当指定的模板ID与配置中的默认模板ID相匹配时，将该模板设置为默认模板
     *
     * @param id 模板ID，用于唯一标识模板
     * @param model 模板对象，包含模板的具体信息
     * @return 返回添加后的模板对象
     */
    @Override
    public PetpetModel addTemplate(String id, PetpetModel model) {
        // 检查当前模板ID是否与配置中定义的默认模板ID相同
        if (id.equals(config.getDefaultTemplate())) {
            // 如果是默认模板，则锁定默认模板位置，并设置默认模板和其ID
            super.defaultTemplateLock = true;
            super.defaultTemplate = model;
            this.defaultTemplateId = id;
        }
        // 调用父类方法，实际添加模板
        return super.addTemplate(id, model);
    }

    /**
     * 根据ID获取联系人权限
     * 如果指定ID的权限不存在，则创建一个新的ContactPermission对象并添加到groupPermissionMap中
     *
     * @param id 联系人或群组的唯一标识符，用于获取权限信息
     * @return 返回与给定ID关联的ContactPermission对象如果之前不存在，则新创建一个
     */
    public ContactPermission getPermission(Object id) {
        return groupPermissionMap.computeIfAbsent(id, k -> new ContactPermission(this, k));
    }

/**
 * 获取默认的表情模板
 *
 * 此方法用于提供一个默认的表情模板，当没有明确指定模板时使用。
 * 通过检查默认模板（defaultTemplate）是否为空，来决定是否需要创建一个新的模板实例。
 * 这样做可以确保每个实例只创建并使用一个默认模板，节省资源。
 *
 * @return PetpetModel 返回默认的表情模板，不会返回null
 */
@Override
public @NotNull PetpetModel getDefaultTemplate() {
    // 如果默认模板尚未初始化，则创建一个新的模板实例
    if (defaultTemplate == null) {
        defaultTemplate = new TemplateIndexScriptModel(this);
    }
    // 返回初始化后的默认模板
    return defaultTemplate;
}


    /**
     * 判断模型是否在随机列表中
     *
     * 本方法用于确定一个模型是否应该出现在随机列表中，通过检查模型的ID是否在配置的禁用模板列表中，
     * 或者模型的元数据是否标记为在随机列表中
     *
     * @param id 模型的唯一标识符，用于与配置中的禁用模板ID进行比较
     * @param model PetpetModel对象，包含模型的数据和元数据
     * @return boolean 如果模型的ID在禁用模板列表中，或者模型的元数据指明它应该在随机列表中，则返回true；否则返回false
     */
    @Override
    protected boolean isModelInRandomList(String id, PetpetModel model) {
        // 检查模型ID是否在配置的禁用模板列表中，或者模型是否被标记为在随机列表中
        return config.getDisabledTemplates().contains(id) || model.getMetadata().getInRandomList()  ;
    }

    /**
     * 初始化命令权限名称映射
     * 该方法首先将预定义的命令权限名称映射全部加入到bot的权限名称映射中，
     * 然后根据配置文件中的命令权限设置，更新或添加新的权限映射
     *
     * @param config QQBot配置对象，包含命令权限名称等配置信息
     */
    private void initPermissionNameMap(QQBotConfig config) {
        // 将预定义的命令权限名称映射加入到bot的权限名称映射中
        this.commandPermissionNameMap.putAll(ContactPermission.COMMAND_PERMISSION_NAME_MAP);

        // 遍历配置文件中的命令权限名称设置
        for (Map.Entry<String, String> entry : config.getCommandPermissionName().entrySet()) {
            // 将权限名称字符串转换为对应的命令权限代码
            int i = stringToCommandPermission(entry.getValue());
            // 如果转换结果为0，则跳过当前设置，不进行映射
            if (i == 0) continue;
            // 将新的权限映射加入到bot的命令权限名称映射中
            this.commandPermissionNameMap.put(entry.getKey(), i);
        }
    }


    /**
     * 将字符串形式的命令权限转换为对应的整型权限值
     *
     * @param permissions 权限字符串，代表特定的命令权限
     * @return 整型权限值，用于表示转换后的权限
     */
    public int stringToCommandPermission(String permissions) {
        return stringToPermission(permissions, commandPermissionNameMap);
    }

    /**
     * 将字符串形式的编辑权限转换为对应的整型权限值
     * 这个方法是静态的，因此它不需要实例化类就可以被调用
     *
     * @param permissions 权限字符串，代表特定的编辑权限
     * @return 整型权限值，用于表示转换后的编辑权限
     */
    public static int stringToEditPermission(String permissions) {
        return stringToPermission(permissions, ContactPermission.EDIT_PERMISSION_NAME_MAP);
    }

    /**
     * 将权限字符串转换为权限代码
     *
     * @param permissions 权限字符串，可以包含多个权限名称，用空格或 '&' 分隔
     * @param nameMap 权限名称与权限代码的映射关系
     * @return 转换后的权限代码，如果权限字符串为空或包含未知权限名称，则返回-1
     */
    protected static int stringToPermission(String permissions, Map<String, Integer> nameMap) {
        // 检查权限字符串是否为空，如果为空则返回特殊值-1
        if (permissions.isBlank()) return -1;

        // 将权限字符串去除前后空格，并按空格或 '&' 分割成数组
        var tokens = permissions.trim().split("[\\s|&]+");

        // 初始化权限合并变量
        int i = 0;

        // 遍历每个权限名称
        for (String token : tokens) {
            // 尝试从map中获取对应的权限值
            Integer p = nameMap.get(token);

            // 如果权限名称不存在，则记录警告日志并跳过当前循环
            if (p == null) {
                log.warn("Unknown permission name: {}", token);
                continue;
            }

            // 将当前权限值与已有的权限合并变量进行位或操作，合并权限
            i |= p;
        }

        // 返回合并后的权限值
        return i;
    }

    /**
     * 根据模板模型获取预期的尺寸信息
     *
     * @param model 模板模型对象
     * @return 包含每个关键帧预期尺寸的映射
     */
    public Map<String, Integer> getTemplateExpectedSize(PetpetModel model) {
        // 使用computeIfAbsent方法来避免重复计算，仅当model对应的值不存在时才进行计算
        return templateExpectedSizeCache.computeIfAbsent(model, k -> {
            // 初始化一个HashMap来存储模板预期尺寸信息
            var map = new HashMap<String, Integer>(4);

            // 如果model是PetpetScriptModel实例，则不进行计算，直接返回一个空映射
            if (model instanceof PetpetScriptModel) {
                return Collections.emptyMap();
            }

            // 遍历模板中的所有元素，寻找类型为AVATAR的元素
            for (ElementModel ele : ((PetpetTemplateModel) model).getElementList()) {
                if (ele.getElementType() != ElementModel.Type.AVATAR) {
                    continue;
                }

                // 将元素转换为AvatarModel类型，并遍历其模板的键
                var avatarEle = (AvatarModel) ele;
                for (String key : avatarEle.template.getKey()) {
                    // 计算预期的尺寸，并将其与已有的值进行比较，取较大者
                    int size = Math.max(avatarEle.getExpectedWidth(), avatarEle.getExpectedHeight());
                    map.merge(key, size, Math::max);
                }
            }

            // 返回计算得到的模板预期尺寸信息映射
            return map;
        });
    }

    /**
     * 更新默认字体设置
     * 此方法用于重新设置或更新系统中的默认字体
     * 它调用了 setDefaultFontFamily 方法，并传入当前配置中指定的默认字体族
     *
     * @return 字符串，表示设置默认字体家族后的结果
     */
    public String updateDefaultFont() {
        return setDefaultFontFamily(config.getDefaultFontFamily());
    }

    /**
     * 构建并返回保存的模板元数据映射
     * 此方法用于构建一个包含所有模板额外元数据的映射
     * 它首先创建一个临时映射，然后将那些不在当前额外元数据映射中且元数据不为空的条目从 staticModelMap 中复制过来
     * 如果临时映射为空，则返回一个空映射
     * 否则，将当前的额外元数据映射中的所有条目添加到临时映射中，并返回这个合并后的映射
     *
     * @return Map<String, TemplateExtraMetadata> 包含所有模板额外元数据的映射
     */
    protected Map<String, TemplateExtraMetadata> buildSavedMetadataMap() {
        // 创建一个临时HashMap，用于存储筛选后的额外元数据
        var tempMap = new HashMap<String, TemplateExtraMetadata>(staticModelMap.size());

        // 筛选staticModelMap中的条目，条件是键不在extraMetadataMap中且值的元数据不为空
        staticModelMap.entrySet().stream()
                .filter(e -> !getExtraMetadataMap().containsKey(e.getKey()) && e.getValue().getMetadata() != null)
                // 将筛选后的条目转换并放入tempMap中
                .forEach(e -> tempMap.put(e.getKey(), TemplateExtraMetadata.fromMetadata(e.getValue().getMetadata())));

        // 如果tempMap为空，则返回一个空的不可变Map
        if (tempMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // 将extraMetadataMap中的所有条目添加到tempMap中
        tempMap.putAll(getExtraMetadataMap());

        // 返回tempMap作为结果
        return tempMap;
    }

    /**
     * 重写更新脚本服务方法
     * 检查默认模板是否存在，如果不存在，则记录警告信息
     */
    @Override
    public void updateScriptService() {
        // 更新脚本服务，这是一个常规的维护任务，以确保服务的最新状态
        super.updateScriptService();

        // 检查默认模板是否未被找到，这是为了确保回复方案的正确性
        if (defaultTemplate == null) {
            // 如果默认模板缺失，记录警告信息，说明将采用预定义的回复文本作为回复方案
            log.warn("无法找到默认模板, 将使用默认 forward_text 回复方案");
        }
    }

    /**
     * 在JVM退出时保存权限配置
     * 遍历所有群组权限并保存配置，如果已保存配置，则记录相关信息
     */
    protected void onJvmExit() {
        // 如果保存权限的条件不满足，则直接返回，不进行后续操作
        if (!savePermission) return;

        // 初始化计数器，用于统计保存权限配置的数量
        int i = 0;

        // 遍历权限配置映射，保存每个权限的配置
        for (ContactPermission permission : groupPermissionMap.values()) {
            permission.saveConfig();
            i++;
        }

        // 如果有权限配置被保存，则输出日志信息
        if (i != 0) {
            log.info("Saved {} permission config", i);
        }
    }
}
