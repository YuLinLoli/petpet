package moe.dituon.petpet.bot;

import moe.dituon.petpet.service.event.ScriptLoadEvent;

/**
 * ScriptBotLoadEvent 类表示一个与机器人脚本加载相关的事件，继承自 ScriptLoadEvent。
 * 该类主要用于在机器人服务中记录或传递脚本加载时的相关信息，例如默认模板的标识符。
 */
public class ScriptBotLoadEvent extends ScriptLoadEvent {
    // 默认模板的标识符，用于指示机器人服务中使用的默认模板
    public final String defaultTemplate;

    /**
     * 构造函数，用于初始化 ScriptBotLoadEvent 对象。
     *
     * @param botService 机器人服务对象，提供脚本加载所需的服务和配置。
     */
    public ScriptBotLoadEvent(BotService botService) {
        super(botService); // 调用父类构造函数，传递机器人服务对象
        defaultTemplate = botService.defaultTemplateId; // 初始化默认模板标识符
    }
}

