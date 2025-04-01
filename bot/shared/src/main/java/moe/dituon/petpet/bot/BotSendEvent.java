package moe.dituon.petpet.bot;

import lombok.Getter;
import moe.dituon.petpet.bot.qq.handler.QQMessageChain;
import moe.dituon.petpet.core.context.RequestContext;
import moe.dituon.petpet.core.utils.image.EncodedImage;
import moe.dituon.petpet.script.event.ScriptSendEvent;
import org.jetbrains.annotations.Nullable;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.io.File;


/**
 * BotSendEvent类是ScriptSendEvent的一个抽象子类，专门用于处理机器人发送事件
 * 它提供了一些基础的响应方法，用于机器人向用户发送不同类型的消息或内容
 */
public abstract class BotSendEvent extends ScriptSendEvent {
    // 使用Getter注解，表示该字段可以自动生成getter方法
    @Getter
    protected boolean isResponseInForward = false;

    /**
     * 构造方法，初始化BotSendEvent实例
     *
     * @param requestContext 请求上下文，包含请求相关的所有信息
     * @param basePath       可选参数，文件的基准路径
     */
    protected BotSendEvent(RequestContext requestContext, @Nullable File basePath) {
        super(requestContext, basePath);
    }

    /**
     * 抽象方法，用于响应一个新的段落
     * 子类必须实现该方法以提供具体的行为
     */
    public abstract void responseNewParagraph();

    /**
     * 抽象方法，用于设置是否在转发模式下响应
     *
     * @param flag 布尔值，表示是否在转发模式下
     */
    public abstract void responseInForward(boolean flag);

    /**
     * 抽象方法，用于响应文本消息
     *
     * @param text 字符串，要发送的文本内容
     */
    public abstract void response(String text);

    /**
     * 抽象方法，用于响应图片消息
     *
     * @param image 编码后的图片对象
     */
    public abstract void responseImage(EncodedImage image);

    /**
     * 响应图片消息，重载方法
     * 该方法接受一个ScriptObjectMirror对象作为模板，用于生成响应内容
     *
     * @param template 描述图片消息的模板对象
     */
    public void responseImage(ScriptObjectMirror template){
        super.result(template);
        responseImage(super.result);
    }

    /**
     * 响应图片消息，重载方法
     * 该方法接受一个字符串路径，用于指定图片文件的位置
     *
     * @param path 图片文件的路径
     */
    public void responseImage(String path){
        super.result(path);
        responseImage(super.result);
    }
}

