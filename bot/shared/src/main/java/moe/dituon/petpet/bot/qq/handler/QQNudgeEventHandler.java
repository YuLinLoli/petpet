package moe.dituon.petpet.bot.qq.handler;

import moe.dituon.petpet.bot.qq.QQBotService;
import moe.dituon.petpet.core.context.RequestContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * 处理QQ戳一戳事件的处理程序类
 * 继承自QQMessageEventHandler，专门用于处理戳一戳事件
 */
public class QQNudgeEventHandler extends QQMessageEventHandler {
    // 随机数生成器，用于决定是否响应戳一戳事件
    protected static final Random random = new Random();

    /**
     * 构造函数，初始化QQNudgeEventHandler
     *
     * @param service QQBot服务实例
     */
    protected QQNudgeEventHandler(QQBotService service) {
        super(service);
    }

    /**
     * 处理戳一戳事件的方法
     *
     * @param context 戳一戳事件上下文
     */
    public void handle(NudgeContext context) {
        context.handleCommand();
    }

    /**
     * 抽象的戳一戳事件上下文类
     * 继承自MessageContext，用于处理戳一戳事件的具体逻辑
     */
    public abstract class NudgeContext extends MessageContext {
        /**
         * 构造函数，初始化NudgeContext
         */
        protected NudgeContext() {
            rawMessageText = "";
        }

        /**
         * 处理戳一戳事件的方法
         * 根据权限、概率和冷却状态决定是否响应戳一戳事件
         */
        public void handleNudge() {
            // 获取当前用户的戳一戳事件发生概率
            float probability = permission.getNudgeProbability();
            // 当概率大于0、随机数小于等于概率值且不在冷却期内时执行响应逻辑
            if (probability > 0
                    && probability >= random.nextFloat()
                    && !isInCooldown()
            ) {
                // 从服务中获取随机的模板
                template = service.randomTemplate();
                // 在冷却时间内不会返回提示
                responseTemplate();
                // 锁定冷却期
                lockCooldown();
            }
        }

        /**
         * 构建请求上下文的方法
         * 根据戳一戳事件的参与者（发送者、目标）构建不同的上下文
         *
         * @return RequestContext实例
         */
        @Override
        protected RequestContext buildRequestContext() {
            // 定义一个存储可调整大小图像元素的列表，用于处理戳一戳事件中的图片信息
            List<QQMessageElement.ResizeableImageElement> imageList;

            // 获取当前机器人的ID
            String botId = getBotId();
            // 获取发送者的ID
            String senderId = getSenderId();
            // 获取目标对象的ID
            String targetId = getTargetId();

            // 判断是否为特殊情况：当目标是机器人本身或发送者自己戳自己时
            if (botId.equals(targetId) || senderId.equals(targetId)) {
                // 构建包含发送者和目标对象的At元素列表（机器人和发送者）
                imageList = List.of(
                    QQMessageElement.AtElement.from(getBotId(), getBotName()),  // 发送者（机器人）
                    QQMessageElement.AtElement.from(getSenderId(), getSenderName())  // 目标对象（发送者自己）
                );
            } else {
                // 构建包含发送者和目标对象的At元素列表（发送者和目标对象）
                imageList = List.of(
                    QQMessageElement.AtElement.from(getSenderId(), getSenderName()),  // 发送者
                    QQMessageElement.AtElement.from(getTargetId(), getTargetName())  // 目标对象
                );
            }
            // 构建 imageUrlMap 和 textMap
            return buildRequestContext(imageList);
        }

        /**
         * 根据索引获取消息令牌的方法
         * 在戳一戳事件处理中未使用
         *
         * @param key 消息令牌的索引
         * @return 消息令牌或null
         */
        @Override
        protected @Nullable String getMessageTokenByIndex(String key) {
            return null;
        }

        /**
         * 获取戳一戳事件目标的ID的抽象方法
         *
         * @return 目标ID
         */
        protected abstract String getTargetId();

        /**
         * 获取戳一戳事件目标的名称的抽象方法
         *
         * @return 目标名称
         */
        protected abstract String getTargetName();

        /**
         * 回复冷却中的戳一戳事件的方法
         * 在戳一戳事件触发冷却时不回复消息
         */
        @Override
        protected void replyCooldown() {
            // 戳一戳触发冷却时不回复消息
        }

        /**
         * 回复戳一戳事件的方法
         * 在戳一戳事件触发冷却时不回复消息
         */
        @Override
        protected void replyNudge() {
            // 戳一戳触发冷却时不回复消息
        }
    }
}

