package moe.dituon.petpet.bot.qq;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.dituon.petpet.bot.BotSendEvent;
import moe.dituon.petpet.core.GlobalContext;
import moe.dituon.petpet.core.context.RequestContext;
import moe.dituon.petpet.core.utils.image.EncodedImage;
import moe.dituon.petpet.script.PetpetScriptModel;
import moe.dituon.petpet.script.event.EventManager;
import moe.dituon.petpet.template.Metadata;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;

/**
 * 实现了PetpetScriptModel接口的模板索引脚本模型类
 * 该类主要用于处理与模板索引相关的事件和元数据
 */
@Getter
@Slf4j
public class TemplateIndexScriptModel implements PetpetScriptModel {
    /**
     * 存储模板的元数据信息
     * API版本、模板ID等信息用于标识和管理模板
     */
    public final Metadata metadata = new Metadata(
            GlobalContext.API_VERSION, 0,
            Collections.emptyList(), Collections.emptyList(),
            "", "",
            true, false, 1,
            null
    );

    /**
     * 用于管理与模板相关的事件
     * 如发送消息、接收消息等事件的处理
     */
    public final EventManager eventManager = new EventManager();

    /**
     * 构造函数，根据配置的默认回复类型来设置事件的处理方式
     *
     * @param service QQBot服务实例，用于访问服务的相关配置和功能
     */
    public TemplateIndexScriptModel(QQBotService service) {
        // 根据服务的默认回复类型，设置当发送消息事件触发时的处理逻辑
        switch (service.config.getDefaultReplyType()) {
            case TEXT:
                // 当默认回复类型为文本时，监听bot发送消息的事件，并回复相应的字符串
                eventManager.on("bot_send", e -> {
                    ((BotSendEvent) e).response(service.getIndexString());
                });
                break;
            case TEMPLATE:
            case FORWARD_TEXT:
                // 当默认回复类型为模板或转发文本时，监听bot发送消息的事件，并以转发的方式回复相应的字符串
                eventManager.on("bot_send", e -> {
                    ((BotSendEvent) e).responseInForward(true);
                    ((BotSendEvent) e).response(service.getIndexString());
                });
                break;
        }
    }

    /**
     * 绘制模板的图片方法，本实现中未使用
     *
     * @param requestContext 请求上下文，包含绘制模板所需的信息
     * @return 返回绘制好的图片，此处返回null
     */
    @Override
    public EncodedImage draw(RequestContext requestContext) {
        return null;
    }

    /**
     * 设置模板的元数据信息，本实现中未使用
     *
     * @param metadata 元数据实例，包含模板的相关信息
     */
    @Override
    public void setMetadata(Metadata metadata) {
    }

    /**
     * 获取模板的预览图片文件，本实现中未使用
     *
     * @return 返回预览图片文件，此处返回null
     */
    @Override
    public @Nullable File getPreviewImage() {
        return null;
    }
}
