package moe.dituon.petpet.bot.qq.handler;

import moe.dituon.petpet.bot.BotSendEvent;
import moe.dituon.petpet.bot.MessageEventHandler;
import moe.dituon.petpet.bot.qq.QQBotConfig;
import moe.dituon.petpet.bot.qq.QQBotService;
import moe.dituon.petpet.bot.qq.avatar.QQAvatarRequester;
import moe.dituon.petpet.bot.qq.permission.ContactPermission;
import moe.dituon.petpet.bot.utils.Cooler;
import moe.dituon.petpet.core.context.RequestContext;
import moe.dituon.petpet.core.element.PetpetModel;
import moe.dituon.petpet.core.element.PetpetTemplateModel;
import moe.dituon.petpet.core.utils.image.EncodedImage;
import moe.dituon.petpet.script.PetpetScriptModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public abstract class QQMessageEventHandler extends MessageEventHandler {
    // QQBot服务的实例，用于处理消息事件
    protected final QQBotService service;
    // QQBot配置的实例，用于获取配置信息
    protected final QQBotConfig config;

    /**
     * QQ消息事件处理器的构造函数
     * 初始化service和config字段
     *
     * @param service QQBot服务的实例，用于处理消息事件
     */
    protected QQMessageEventHandler(QQBotService service) {
        this.service = service;
        // 从service中获取配置信息
        this.config = service.getConfig();
    }

    /**
     * 处理消息事件的方法
     * 调用MessageContext的handleCommand方法来处理命令
     *
     * @param message 消息上下文，包含消息相关信息和处理方法
     */
    protected void handle(MessageContext message) {
        // 调用消息上下文的处理命令方法
        message.handleCommand();
    }
    /**
     * 抽象类 MessageContext 定义了处理消息的上下文环境
     * 它封装了消息的相关信息，如消息文本、模板、权限等
     */
    public abstract class MessageContext {
        // 消息文本，包含指令和id
        protected String messageText;
        // Petpet模型，用作某种处理模板
        protected PetpetModel template;
        // 不包含 指令与 id 的原始文本
        protected String rawMessageText;
        // 文本参数, 不包含指令与 id
        protected String[] messageTokens = null;
        // 联系人权限，决定处理消息的权限级别
        protected ContactPermission permission;
        // 消息链接口，代表接收到的消息
        protected QQMessageChainInterface message;

        /**
         * 构造函数，初始化 MessageContext 对象
         * @param message QQMessageChainInterface 类型的消息对象，包含消息内容和发送者信息等
         */
        protected MessageContext(QQMessageChainInterface message) {
            this.message = message;
            // 初始化 messageText 为消息的内容文本
            this.messageText = message.getContentText();
            // 根据消息的主题id获取权限
            this.permission = service.getPermission(getSubjectId());
        }

        /**
         * 默认构造函数，用于创建空的 MessageContext 对象
         * 需要在子类中根据具体需求进行扩展
         */
        protected MessageContext() {
        }

        /**
         * 处理管理员权限命令
         * 该方法用于处理管理员权限的开启或关闭请求
         * 它首先检查发送者是否具有相应的权限，然后根据命令操作进行相应的处理
         * 如果操作成功，它将发送一条回复消息，指示权限已更改
         *
         * @return boolean 表示命令处理是否成功如果发送者没有权限或命令操作无效，则返回false
         */
        public boolean handleOpCommand() {
            // 检查发送者是否具有管理员权限
            if (!senderHasGroupPermission()) {
                return false;
            }

            // 提取命令操作部分，以确定是开启还是关闭权限
            var commandOperation = messageText.substring(config.getCommand().length()); // [on|off]

            String result;
            try {
                // 调用权限处理方法，根据命令操作执行相应的逻辑
                result = permission.handleEditCommand(commandOperation);
                // 如果处理结果为空，则返回false
                if (result == null) {
                    return false;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                // 捕获可能的异常，并将异常消息作为结果
                result = e.getMessage();
            }

            // 发送回复消息，指示命令处理结果
            replyMessage(config.getCommand() + ' ' + result);

            // 返回true，表示命令处理成功
            return true;
        }

        /**
         * 处理接收到的命令消息
         * 本方法根据消息内容执行不同的逻辑路径，包括处理普通命令、管理员命令，以及根据模板ID回复消息
         */
        public void handleCommand() {
            // 如果消息为空，则直接返回，不进行处理
            if (this.messageText.isEmpty()) {
                return;
            }

            // 判断消息是否以配置的命令开头
            if (messageText.startsWith(config.getCommand())) {
                // 提取命令后的参数部分
                var params = messageText.substring(config.getCommand().length()).trim();

                // 检查命令是否未被禁用
                boolean isEnable = (permission.getCommandPermission() & ContactPermission.COMMAND) != 0;

                // 如果消息有指定目标且命令未被禁用
                if (message.hasTarget() && isEnable) {
                    // 检查是否处于冷却时间
                    if (isInCooldown()) {
                        replyCooldown();
                        return;
                    }
                    // 如果发送 pet 指令且有指定目标, 则使用随机模板
                    template = service.randomTemplate();
                    responseTemplate();
                    lockCooldown();
                } else if (params.isEmpty() && isEnable) {
                    // 如果只发送了 pet 指令, 使用默认模板
                    // 默认模板不会受到 冷却时间 影响
                    template = service.getDefaultTemplate();
                    responseTemplate();
                } else if (handleOpCommand()) {
                    // 处理管理员指令, 匹配成功直接返回
                    return;
                } else if (isEnable) {
                    // 检查是否处于冷却时间
                    if (isInCooldown()) {
                        replyCooldown();
                        return;
                    }
                    // 可能包含模板 id 与文本参数
                    var tokens = params.split(" +");
                    var id = service.getTemplateId(tokens[0]);
                    template = service.getTemplateById(id);
                    // 匹配模板失败, 使用默认模板
                    if (template == null || permission.getDisabledTemplateIds().contains(id)) {
                        template = service.getDefaultTemplate();
                        rawMessageText = params;
                    } else {
                        rawMessageText = params.substring(tokens[0].length()).trim();
                    }
                    // 处理消息中的参数
                    messageTokens = Arrays.copyOfRange(tokens, 1, tokens.length);
                    responseTemplate();
                    lockCooldown();
                }
            } else if (messageText.startsWith(config.getCommandHead())) {
                // 处理以命令头开头的消息
                rawMessageText = messageText.substring(config.getCommandHead().length()).trim();
                var tokens = rawMessageText.split(" +");
                var id = service.getTemplateId(tokens[0]);
                template = service.getTemplateById(id);
                // 如果模板无效或被禁用，则直接返回
                if (template == null || permission.getDisabledTemplateIds().contains(id)) {
                    return;
                }
                // 检查是否处于冷却时间
                if (isInCooldown()) {
                    replyCooldown();
                    return;
                }
                // 处理消息中的参数
                rawMessageText = rawMessageText.substring(tokens[0].length()).trim();
                messageTokens = Arrays.copyOfRange(tokens, 1, tokens.length);
                responseTemplate();
                lockCooldown();
            }
        }

        /**
         * 根据模板类型响应消息
         * 此方法处理消息模板的响应逻辑，根据模板的不同类型执行相应的操作
         * 首先构建请求上下文，然后根据模板类型，可能是表情模板模型或表情脚本模型
         * 对于表情模板模型，直接绘制并回复消息；对于表情脚本模型，构建一个机器人发送事件并触发
         */
        protected void responseTemplate() {
            // 构建请求上下文对象，该对象包含了处理请求所需的信息
            var request = buildRequestContext();

            // 判断模板是否为表情模板模型类型
            if (template instanceof PetpetTemplateModel) {
                // 如果是表情模板模型，调用其draw方法绘制回复消息，并发送回复
                replyMessage(template.draw(request));
            } else if (template instanceof PetpetScriptModel) {
                // 如果是表情脚本模型，首先将当前模板转换为表情脚本模型类型
                var script = (PetpetScriptModel) this.template;
                // 构建一个机器人发送事件，该事件基于当前脚本和请求上下文
                var event = buildBotSendEvent(script, request);
                // 触发脚本的事件管理器中的"bot_send"事件，并将构建的事件作为参数
                script.getEventManager().trigger("bot_send", event);
                // 回复事件作为消息回复
                replyMessage(event);
            }
        }


        /**
         * 构建请求上下文对象
         * 此方法用于组装RequestContext，主要处理消息中的图像元素，并可能添加额外的@元素
         *
         * @return RequestContext对象，包含处理后的图像元素列表
         */
        protected RequestContext buildRequestContext() {
            // 构建图像列表，过滤出AT和IMAGE类型的元素，并转换为ResizeableImageElement类型
            List<QQMessageElement.ResizeableImageElement> imageList = message.stream()
                    .filter(ele -> ele.getType() == QQMessageElement.MessageType.AT ||
                            ele.getType() == QQMessageElement.MessageType.IMAGE)
                    .map(ele -> (QQMessageElement.ResizeableImageElement) ele)
                    .collect(Collectors.toList());

            // 如果消息中包含回复图像，则将其添加到图像列表中
            if (message.getReplyImage() != null) {
                imageList.add(new QQMessageElement.ImageElement(message.getReplyImage()));
            }

            // 确保 imageList 至少有两个元素
            if (imageList.isEmpty()) {
                // 如果图像列表为空，添加Bot和Sender的@元素
                imageList.add(QQMessageElement.AtElement.from(getBotId(), getBotName())); // Bot
                imageList.add(QQMessageElement.AtElement.from(getSenderId(), getSenderName())); // Sender
            } else if (imageList.size() == 1) {
                // 如果图像列表只有一个元素，添加Sender的@元素，并与现有图像元素组成新列表
                imageList = List.of(
                        QQMessageElement.AtElement.from(getSenderId(), getSenderName()), // Sender
                        imageList.get(0) // Image
                );
            }

            // 调用重载方法buildRequestContext以进一步处理图像列表并构建RequestContext
            return buildRequestContext(imageList);
        }

        /**
         * 构建请求上下文对象
         * 此方法根据提供的图像列表和模板类型，构建一个RequestContext对象，该对象包含了处理请求所需的图像URL和文本信息
         *
         * @param imageList 包含可调整大小图像元素的列表
         * @return 返回一个RequestContext对象，其中包含了根据模板类型和图像列表构建的图像URL和文本信息
         */
        protected RequestContext buildRequestContext(List<QQMessageElement.ResizeableImageElement> imageList) {
            // 构建 imageUrlMap 和 textMap
            Map<String, String> imageUrlMap;
            Map<String, String> textMap;

            // 判断模板类型，以决定使用特定模板的键值还是使用所有键值
            if (template instanceof PetpetTemplateModel) {
                var petpetTemplate = (PetpetTemplateModel) template;
                // 根据特定模板类型构建图像URL映射和文本映射
                imageUrlMap = buildImageUrlMap(imageList, petpetTemplate.getRequestImageKeys());
                textMap = buildTextMap(imageList, petpetTemplate.getRequestTextKeys());
            } else {
                // 如果不是特定模板类型，则使用所有图像和文本键值构建映射
                imageUrlMap = buildImageUrlMap(imageList, ALL_IMAGE_KEYS);
                textMap = buildTextMap(imageList, ALL_TEXT_KEYS);
            }

            // 使用构建的图像URL映射和文本映射创建并返回请求上下文对象
            return new RequestContext(imageUrlMap, textMap);
        }

        /**
         * 根据给定的键集合和值映射函数构建一个映射表
         * 此方法用于将一组键与通过特定映射函数计算出的值关联起来，形成一个映射表
         *
         * @param keys 一组唯一的键，用于映射的键部分
         * @param valueMapper 值映射函数，用于根据键计算对应的值
         * @return 返回一个映射表，其中包含所有键和通过映射函数计算出的非空值
         */
        protected Map<String, String> buildMap(Collection<String> keys, UnaryOperator<String> valueMapper) {
            // 初始化一个HashMap，预设容量为键集合的大小，以避免不必要的哈希表扩容
            var map = new HashMap<String, String>(keys.size());

            // 遍历键集合，为每个键计算对应的值
            for (var key : keys) {
                // 使用映射函数计算当前键对应的值
                var value = valueMapper.apply(key);

                // 如果计算出的值不为空，则将键值对添加到映射表中
                // 这里避免了映射中出现空值，确保映射中的每个值都是有效的
                if (value != null) {
                    map.put(key, valueMapper.apply(key));
                }
            }

            // 返回构建完成的映射表
            return map;
        }

        /**
         * 判断当前是否处于冷却期
         *
         * 如果当前上下文是在群组中，则检查发送者ID或主题ID是否被锁定
         * 如果不在群组上下文中，则仅检查主题ID是否被锁定
         *
         * @return 如果当前上下文或主题ID被锁定，则返回true，否则返回false
         */
        protected boolean isInCooldown() {
            if (inGroupContext()) {
                // 在群组上下文中，检查发送者ID或主题ID是否被锁定
                return Cooler.isLocked(getSenderId()) || Cooler.isLocked(getSubjectId());
            }
            // 不在群组上下文中，仅检查主题ID是否被锁定
            return Cooler.isLocked(getSubjectId());
        }

        /**
         * 处理回复消息的冷却状态
         * 本方法旨在根据当前配置和冷却状态，决定是否发送冷却中的消息
         */
        protected void replyCooldown() {
            // 获取配置中的冷却消息文本
            var msg = config.getInCoolDownMessage();

            // 如果冷却消息为空，则不执行任何操作直接返回
            if (msg.isEmpty()) {
                return;
            }
            // 如果服务配置允许通过nudge方式进行冷却回复
            else if (service.cooldownReplyNudge) {
                // 执行nudge回复操作，并直接返回
                replyNudge();
                return;
            }
            // 如果上述条件都不满足，则直接回复配置中的冷却消息
            replyMessage(config.getInCoolDownMessage());
        }

        /**
         * 执行锁定冷却操作
         * 此方法首先获取当前上下文中的冷却时间，然后根据配置和上下文类型应用冷却
         * 如果用户在特定组内且配置了用户冷却时间，则应用用户级别的冷却
         * 无论用户是否在组内，如果存在全局冷却时间，则应用全局级别的冷却
         */
        protected void lockCooldown() {
            // 获取全局冷却时间
            long cooldownTime = permission.getCooldownTime();
            // 检查是否在组上下文中
            if (inGroupContext()) {
                // 获取用户级别的冷却时间
                long userCooldownTime = config.getUserCooldownTime();
                // 如果用户冷却时间大于0，则应用冷却
                if (userCooldownTime > 0) {
                    Cooler.lock(getSenderId(), userCooldownTime);
                }
            }
            // 如果全局冷却时间大于0，则应用冷却
            if (cooldownTime > 0) {
                Cooler.lock(getSubjectId(), cooldownTime);
            }
        }

        /**
         * 构建图像URL映射
         * 此方法根据提供的图像列表和键集合，构建一个映射，其中每个键对应一个特定的图像URL
         * 主要用于处理和获取不同身份（如发送者、接收者、群组、机器人等）的头像URL
         *
         * @param imageList 图像元素列表，包含可调整大小的图像信息
         * @param keys 一组键，用于指定需要构建的图像URL的类型，例如发送者、接收者等
         * @return 返回一个映射，其中每个键对应一个特定的图像URL
         */
        protected Map<String, String> buildImageUrlMap(
                List<QQMessageElement.ResizeableImageElement> imageList,
                Collection<String> keys
        ) {
            // 定义最终的图像列表，用于Lambda表达式中
            final var finalImageList = imageList;

            // 构建映射，每个键对应一个特定的图像URL
            return buildMap(keys, key -> {
                // 根据键的类型，选择相应的图像URL
                switch (key) {
                    case FROM_KEY:
                        // 获取发送者头像URL，假设倒数第二个图像是发送者头像
                        return finalImageList.get(finalImageList.size() - 2).getUrl(getAvatarSize(key));
                    case TO_KEY:
                        // 获取接收者头像URL，假设最后一个图像是接收者头像
                        return finalImageList.get(finalImageList.size() - 1).getUrl(getAvatarSize(key));
                    case GROUP_KEY:
                        // TODO: 分别处理群头像与用户头像
                        // 获取群头像URL，目前未实现区分群头像和用户头像的处理
                        return QQAvatarRequester.getAvatarUrlString(getSubjectId(), getAvatarSize(key));
                    case BOT_KEY:
                        // 获取机器人头像URL
                        return QQAvatarRequester.getAvatarUrlString(getBotId(), getAvatarSize(key));
                    default:
                        // 根据键获取其他图像URL
                        return getImageUrlByIndex(finalImageList, key);
                }
            });
        }

        /**
         * 构建文本映射
         * 该方法根据提供的图像列表和键集合，构建一个映射，映射中的键是提供的键集合中的元素，
         * 值是根据键的不同从图像列表或消息元素中获取的相关文本
         *
         * @param imageList 包含可调整大小图像元素的列表，用于获取图像名称
         * @param keys 用于构建映射的键的集合
         * @return 返回一个映射，其中键是提供的键集合中的元素，值是根据键获取的文本
         */
        protected Map<String, String> buildTextMap(
                List<QQMessageElement.ResizeableImageElement> imageList,
                Collection<String> keys
        ) {
            // 使用final修饰符将imageList声明为不可变，确保其在方法内部不会被修改
            final var finalImageList = imageList;

            // 构建并返回一个映射，映射中的键是keys集合中的元素，值是根据键通过Lambda表达式获取的文本
            return buildMap(keys, key -> {
                // 根据键的不同，返回相应的文本值
                switch (key) {
                    case FROM_KEY:
                        // 获取并返回倒数第二个图像元素的名称
                        return finalImageList.get(finalImageList.size() - 2).getName();
                    case TO_KEY:
                        // 获取并返回最后一个图像元素的名称
                        return finalImageList.get(finalImageList.size() - 1).getName();
                    case GROUP_KEY:
                        // 调用方法获取并返回主题名称
                        return getSubjectName();
                    case BOT_KEY:
                        // 调用方法获取并返回机器人名称
                        return getBotName();
                    case RAW_TEXT_KEY:
                        // 返回原始消息文本
                        return rawMessageText;
                    default:
                        // 根据提供的键获取并返回消息中的相应令牌
                        return getMessageTokenByIndex(key);
                }
            });
        }

        /**
         * 根据索引从图像列表中获取图像URL
         * 此方法用于处理可调整大小的图像元素，并返回指定索引对应的图像URL
         * 如果索引无效或图像列表中没有对应索引的元素，则返回null
         *
         * @param imageList 包含可调整大小图像元素的列表
         * @param key 图像的索引，以字符串形式提供
         * @return 对应索引的图像URL，如果索引无效或超出范围，则返回null
         */
        protected @Nullable String getImageUrlByIndex(List<QQMessageElement.ResizeableImageElement> imageList, String key) {
            try {
                int i = Integer.parseInt(key);
                return imageList.get(i).getUrl(getAvatarSize(key));
            } catch (NumberFormatException | IndexOutOfBoundsException ignored) {
                return null;
            }
        }

        /**
         * 根据索引从消息令牌数组中获取消息令牌
         * 此方法返回指定索引对应的消息令牌字符串
         * 如果索引无效或超出数组范围，则返回null
         *
         * @param key 消息令牌的索引，以字符串形式提供
         * @return 对应索引的消息令牌，如果索引无效或超出范围，则返回null
         */
        protected @Nullable String getMessageTokenByIndex(String key) {
            try {
                return messageTokens[Integer.parseInt(key)];
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
                return null;
            }
        }

        /**
         * 根据键获取头像的预期大小
         * 此方法用于从模板配置中获取特定键对应的头像大小
         * 如果键不存在于配置中，则返回默认大小（0）
         *
         * @param key 头像大小的键
         * @return 对应键的头像大小，如果键不存在，则返回默认大小（0）
         */
        protected int getAvatarSize(String key) {
            return service.getTemplateExpectedSize(template).getOrDefault(key, 0);
        }

        /**
         * 检查是否在群组上下文中
         * 此方法通过比较主题ID和发送者ID来判断当前消息是否发生在群组上下文中
         * 如果两者不相等，则认为是在群组上下文中
         *
         * @return 如果当前上下文是群组，则返回true；否则返回false
         */
        protected boolean inGroupContext() {
            return !getSubjectId().equals(getSenderId());
        }

        /**
         * 判断发送者是否为群聊管理员或拥有 bot 编辑权限
         */
        protected abstract boolean senderHasGroupPermission();

        /**
         * 构建 bot 发送事件
         * @param script 脚本模型
         * @param context 请求上下文
         * @return 构建的 bot 发送事件
         */
        protected abstract BotSendEvent buildBotSendEvent(PetpetScriptModel script, RequestContext context);

        /**
         * 获取 bot 名称
         * @return bot 名称
         */
        protected abstract String getBotName();

        /**
         * 获取 bot ID
         * @return bot ID
         */
        protected abstract String getBotId();

        /**
         * 获取发送者名称
         * @return 发送者名称
         */
        protected abstract String getSenderName();

        /**
         * 获取发送者 ID
         * @return 发送者 ID
         */
        protected abstract String getSenderId();

        /**
         * 获取主题名称，在群聊中为群名称，在私聊中为用户名称
         * @return 主题名称
         */
        protected abstract String getSubjectName();

        /**
         * 获取主题 ID，在群聊中为群 ID，在私聊中为用户 ID
         * @return 主题 ID
         */
        protected abstract String getSubjectId();

        /**
         * 回复消息，文本形式
         * @param text 要回复的文本
         */
        protected abstract void replyMessage(String text);

        /**
         * 回复消息，图片形式
         * @param image 要回复的图片
         */
        protected abstract void replyMessage(EncodedImage image);

        /**
         * 回复消息，自定义事件形式
         * @param event 要回复的事件
         */
        protected abstract void replyMessage(BotSendEvent event);

        /**
         * 回复戳一戳消息
         */
        protected abstract void replyNudge();
    }
}
