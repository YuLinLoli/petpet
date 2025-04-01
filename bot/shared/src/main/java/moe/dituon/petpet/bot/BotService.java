package moe.dituon.petpet.bot;

import lombok.Getter;
import lombok.Setter;
import moe.dituon.petpet.core.element.PetpetModel;
import moe.dituon.petpet.service.BaseService;
import moe.dituon.petpet.service.ObservableBaseService;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 对于通用社交媒体的抽象 Bot 服务
 */
/**
 * BotService 类继承自 ObservableBaseService，旨在为机器人提供服务功能
 * 它管理着与机器人加载事件和模板相关的属性和方法
 */
public class BotService extends ObservableBaseService {
    // 定义机器人加载事件的键，用于标识机器人加载事件
    public static final String BOT_LOAD_EVENT_KEY = "bot_load";
    // 定义当没有加载任何 Petpet 模板时显示的消息
    public static final String NO_TEMPLATE_MESSAGE = "暂未加载任何 Petpet 模板...";

    // 存储随机 ID 列表，用于处理随机事件或选择
    @Getter
    protected List<String> randomIdList = new ArrayList<>(256);
    // 记录上一次的版本号，用于检测更新
    protected int previousVersion = super.updateVersion;
    // 缓存首页字符串，减少重复生成首页内容的开销
    protected String indexString = null;
    // 默认的 Petpet 模板，可能为空
    @Getter
    @Nullable
    protected PetpetModel defaultTemplate = null;
    // 默认模板的 ID，可能为空
    @Getter
    @Nullable
    protected String defaultTemplateId = null;
    // 标记默认模板是否被锁定，防止修改或删除
    protected boolean defaultTemplateLock = false;

    // 存储模板的额外元数据，可能为空
    protected Map<String, TemplateExtraMetadata> extraMetadataMap = null;


    /**
     * 获取额外的模板元数据映射
     * 如果extraMetadataMap为空，则通过调用initCustomTemplateMetadata方法进行初始化
     *
     * @return 包含模板额外元数据的映射
     */
    protected Map<String, TemplateExtraMetadata> getExtraMetadataMap() {
        // 检查extraMetadataMap是否未被初始化
        if (extraMetadataMap == null) {
            // 初始化extraMetadataMap
            extraMetadataMap = initCustomTemplateMetadata();
        }
        // 返回初始化后的extraMetadataMap
        return extraMetadataMap;
    }

    /**
     * 初始化自定义模板的额外元数据映射
     * 此方法应由子类覆盖以提供具体的实现
     * 当前默认返回一个空映射
     *
     * @return 一个空的映射，表示没有额外的模板元数据
     */
    protected Map<String, TemplateExtraMetadata> initCustomTemplateMetadata() {
        // 返回一个不可变的空映射
        return Collections.emptyMap();
    }
    /**
     * 重写添加模板的方法
     * 此方法用于向系统中添加一个新的宠物模板，同时处理与默认模板和随机列表相关的逻辑
     *
     * @param id 模板的唯一标识符
     * @param model 要添加的宠物模板模型
     * @return 返回之前已存在的相同ID的宠物模板模型，如果没有则返回null
     */
    @Override
    public PetpetModel addTemplate(String id, PetpetModel model) {
        // 获取额外元数据映射中的自定义元数据
        var customMetadata = getExtraMetadataMap().get(id);
        // 如果存在自定义元数据，则将其应用到模型中
        if (customMetadata != null) {
            model.setMetadata(customMetadata.toMetadata());
        }
        // 调用父类方法添加模板
        var prev = super.addTemplate(id, model);
        // 如果模型的元数据为空，则返回之前已存在的模板
        if (model.getMetadata() == null) {
            return prev;
        }
        // 如果模型在随机列表中，则将其ID添加到随机ID列表
        if (isModelInRandomList(id, model)) {
            randomIdList.add(id);
        }
        // 如果默认模板未锁定，且当前模型的默认模板权重不为0，并且满足以下条件之一：
        // 1. 当前默认模板为空
        // 2. 当前模型的默认模板权重大于已有的默认模板权重
        // 则将当前模型设置为新的默认模板
        if (!defaultTemplateLock && model.getMetadata().getDefaultTemplateWeight() != 0 && (this.defaultTemplate == null
                || (model.getMetadata().getDefaultTemplateWeight() > this.defaultTemplate.getMetadata().getDefaultTemplateWeight())
        )) {
            this.defaultTemplate = model;
            this.defaultTemplateId = id;
        }
        // 返回之前已存在的相同ID的模板
        return prev;
    }

    /**
     * 判断模型是否在随机列表中
     *
     * @param id 宠物模型的ID，用于唯一标识一个宠物模型
     * @param model 宠物模型实例，包含宠物的各类信息
     * @return 如果模型在随机列表中，则返回true；否则返回false
     */
    protected boolean isModelInRandomList(String id, PetpetModel model) {
        return model.getMetadata().getInRandomList();
    }

    /**
     * 随机选择一个ID
     *
     * @return 返回随机选择的ID字符串
     * @throws IllegalStateException 如果随机ID列表为空时抛出此异常
     */
    public String randomId() {
        if (randomIdList.isEmpty()) {
            throw new IllegalStateException("No template in random list!");
        }
        return randomIdList.get(BaseService.RANDOM.nextInt(randomIdList.size()));
    }

    /**
     * 随机选择一个pet表情作为模板
     *
     * @return 返回随机选择的pet表情
     */
    public PetpetModel randomTemplate() {
        return staticModelMap.get(randomId());
    }

    /**
     * 获取索引字符串
     *
     * @return 返回当前的索引字符串
     */
    public String getIndexString() {
        // 如果索引字符串已生成且版本未更新，则直接返回缓存的索引字符串
        if (indexString != null && previousVersion == updateVersion) {
            return indexString;
        }
        // 重新生成索引字符串并更新版本号
        indexString = buildIndexString();
        previousVersion = updateVersion;
        return indexString;
    }

    /**
     * 构建索引字符串的方法
     *
     * 此方法用于生成一个包含所有非隐藏模板的索引字符串每个模板的名称后面是其别名（如果有）
     * 如果没有可用的模板，则返回一个表示没有模板的消息
     *
     * @return 生成的索引字符串，如果没有模板可用则返回NO_TEMPLATE_MESSAGE
     */
    protected String buildIndexString() {
        StringBuilder sb = new StringBuilder();
        // 检查staticModelMap是否为空如果为空，则返回没有模板的消息
        if (staticModelMap.isEmpty()) {
            return NO_TEMPLATE_MESSAGE;
        }
        // 遍历staticModelMap中的每个条目
        for (Map.Entry<String, PetpetModel> entry : staticModelMap.entrySet()) {
            var model = entry.getValue();
            // 如果模型的元数据为null或被标记为隐藏，则跳过当前循环迭代
            if (model.getMetadata() == null || model.getMetadata().getHidden()) {
                continue;
            }
            // 获取模型的别名列表
            var alias = model.getMetadata().getAlias();
            // 如果别名列表为空，则仅追加模板名称
            if (alias.isEmpty()) {
                sb.append(entry.getKey()).append('\n');
                continue;
            }
            // 如果别名列表不为空，则追加模板名称后跟其别名
            sb.append(entry.getKey()).append("  ( ").append(String.join(", ", alias)).append(" )\n");
        }
        // 删除最后多余的换行符
        sb.deleteCharAt(sb.length() - 1);
        // 返回构建的索引字符串
        return sb.toString();
    }

    /**
     * 当全部模板加载完后调用以发布事件

     * 更新脚本服务此方法主要用于在特定时机（如配置更新、系统初始化等）重新加载或初始化所有脚本模型的事件处理
     * 如果当前没有脚本模型注册，则直接返回，不做任何操作
     * 对于每个已注册的脚本模型，根据其事件管理器中是否存在特定的事件键（BOT_LOAD_EVENT_KEY）来决定触发哪个事件
     * 如果存在BOT_LOAD_EVENT_KEY，则触发该事件，否则触发LOAD_EVENT_KEY对应的事件
     * 这种设计允许脚本模型根据自身需要灵活地响应加载事件
     */
    @Override
    public void updateScriptService() {
        // 检查是否有脚本模型注册，如果没有则直接返回
        if (scriptModelMap.isEmpty()) return;

        // 创建一个脚本加载事件实例，用于通知脚本模型已经加载
        var loadEvent = new ScriptBotLoadEvent(this);

        // 遍历所有已注册的脚本模型
        for (var model : scriptModelMap.values()) {
            // 检查模型的事件管理器中是否有BOT_LOAD_EVENT_KEY对应的事件
            if (model.getEventManager().has(BOT_LOAD_EVENT_KEY)) {
                // 如果有，则触发该事件
                model.getEventManager().trigger(BOT_LOAD_EVENT_KEY, loadEvent);
            } else {
                // 如果没有，则触发LOAD_EVENT_KEY对应的事件
                model.getEventManager().trigger(LOAD_EVENT_KEY, loadEvent);
            }
        }
    }
}
