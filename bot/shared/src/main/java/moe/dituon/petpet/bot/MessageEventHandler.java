package moe.dituon.petpet.bot;

import java.util.Set;

/**
 * 消息事件处理器的抽象类
 * 用于定义处理消息事件时的通用方法和属性
 */
public abstract class MessageEventHandler {
    // 定义消息事件中常见字段的键名
    public static final String FROM_KEY = "from";
    public static final String TO_KEY = "to";
    public static final String GROUP_KEY = "group";
    public static final String BOT_KEY = "bot";
    public static final String FROM_ID_KEY = "from_id";
    public static final String TO_ID_KEY = "to_id";
    public static final String GROUP_ID_KEY = "group_id";
    public static final String BOT_ID_KEY = "bot_id";
    public static final String RAW_TEXT_KEY = "raw";

    // 默认图片名称
    public static final String DEFAULT_IMAGE_NAME = "这个";

    // 所有文本消息键的集合
    public static final Set<String> ALL_TEXT_KEYS = Set.of(
            FROM_KEY, TO_KEY, GROUP_KEY, BOT_KEY,
            FROM_ID_KEY, TO_ID_KEY, GROUP_ID_KEY, BOT_ID_KEY
    );
    // 所有图片消息键的集合
    public static final Set<String> ALL_IMAGE_KEYS = Set.of(
            FROM_KEY, TO_KEY, GROUP_KEY, BOT_KEY
    );
}

