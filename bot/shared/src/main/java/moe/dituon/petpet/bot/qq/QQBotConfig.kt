package moe.dituon.petpet.bot.qq

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.dituon.petpet.bot.utils.Cooler
import moe.dituon.petpet.core.BaseRenderConfig
import moe.dituon.petpet.core.FontManager
import java.util.concurrent.TimeUnit

val DEFAULT_COMMAND_PERMISSION_NAME = mapOf(
    "所有" to "all",
    "cmd" to "command",
    "指令" to "command",
    "提及" to "at",
    "img" to "image",
    "回复" to "image",
    "图像" to "image",
    "id" to "command_head",
    "key" to "command_head",
    "指令头" to "command_head",
)

val DEFAULT_COMMAND_OPERATION_NAME = mapOf(
    "启用" to "on",
    "禁用" to "off",
    "概率" to "nudge_probability",
    "戳一戳概率" to "nudge_probability",
    "冷却" to "cooldown_time",
    "冷却时间" to "cooldown_time",
    "禁用" to "disable_template",
    "禁用模板" to "disable_template",
)

val DEFAULT_TIME_UNIT_NAME = mapOf(
    "毫秒" to 1L,
    "秒" to TimeUnit.SECONDS.toMillis(1L),
    "分" to TimeUnit.MINUTES.toMillis(1L),
    "小时" to TimeUnit.HOURS.toMillis(1L),
    "天" to TimeUnit.DAYS.toMillis(1L),
)

const val DEFAULT_DEFAULT_GROUP_EDIT_PERMISSION = "command_permission nudge_probability disable_template"

enum class ReplyType {
    @SerialName("random")
    RANDOM,
    @SerialName("text")
    TEXT,
    @SerialName("forward_text")
    FORWARD_TEXT,
    @SerialName("template")
    TEMPLATE,
    @SerialName("url")
    URL, // TODO
}

/**
 * QQBot配置类，用于序列化和反序列化配置信息
 *
 * @param command 用于触发机器人的命令前缀，默认为"pet"
 * @param commandHead 命令的头部，用于特定的命令识别，默认为空字符串
 * @param respondSelfNudge 是否响应自身的戳一戳事件，默认为false
 * @param respondFriend 是否响应好友消息，默认为true
 * @param respondGroup 是否响应群消息，默认为true
 * @param defaultFontFamily 默认的字体家族，默认使用FontManager中的默认字体
 *
 * @param defaultReplyType 默认的回复类型，使用ReplyType枚举，默认为TEMPLATE
 * @param defaultTemplate 默认的模板字符串，用于回复消息时的模板，默认为null
 * @param commandPermissionName 命令与权限名称的映射，用于管理不同命令的权限，默认为预设的命令权限名称
 * @param timeUnitName 时间单位的名称映射，用于解析时间相关的命令参数，默认为预设的时间单位名称
 * @param commandOperationName 命令与操作名称的映射，用于解析命令参数中的操作，默认为预设的命令操作名称
 *
 * @param defaultGroupCommandPermission 默认的群命令权限，默认为"all"，即所有命令都可以在群中使用
 * @param defaultGroupEditPermission 默认的群编辑权限，默认为预设的编辑权限
 * @param nudgeProbability 戳一戳事件的响应概率，默认为QQBotService中的默认概率
 * @param disabledGroups 禁用机器人的群ID集合，默认为空集合
 * @param disabledTemplates 禁用的模板集合，默认为空集合
 * @param imageCachePoolSize 图像缓存池大小，默认为2048
 * @param groupCooldownTime 群冷却时间，默认为Colder中的默认群冷却时间
 * @param userCooldownTime 用户冷却时间，默认为Colder中的默认用户冷却时间
 * @param inCoolDownMessage 在冷却时间内的默认回复消息，默认为Colder中的默认消息
 *
 * @param autoUpdate 是否自动更新，默认为true
 * @param repositoryUrls 仓库URL列表，用于自动更新时获取资源，默认为空列表
 * @param headless 是否为无头模式，即不显示图形界面，默认为true
 * @param gifQuality GIF质量，影响生成的GIF图像的质量和大小，默认为10
 */
@Serializable
data class QQBotConfig(
    val command: String = "pet",
    @SerialName("command_head")
    val commandHead: String = "",
    @SerialName("respond_self_nudge")
    val respondSelfNudge: Boolean = false,
    @SerialName("respond_friend")
    val respondFriend: Boolean = true,
    @SerialName("respond_group")
    val respondGroup: Boolean = true,
    @SerialName("default_font_family")
    val defaultFontFamily: String = FontManager.DEFAULT_FONT,

    @SerialName("default_reply_type")
    val defaultReplyType: ReplyType = ReplyType.TEMPLATE,
    @SerialName("default_template")
    val defaultTemplate: String? = null,
    @SerialName("command_permission_name")
    val commandPermissionName: Map<String, String> = DEFAULT_COMMAND_PERMISSION_NAME,
    @SerialName("time_unit_name")
    val timeUnitName: Map<String, Long> = DEFAULT_TIME_UNIT_NAME,
    @SerialName("command_operation_name")
    val commandOperationName: Map<String, String> = DEFAULT_COMMAND_OPERATION_NAME,

    @SerialName("group_command_permission")
    val defaultGroupCommandPermission: String = "all",
    @SerialName("group_edit_permission")
    val defaultGroupEditPermission: String = DEFAULT_DEFAULT_GROUP_EDIT_PERMISSION,
    @SerialName("nudge_probability")
    val nudgeProbability: Float = QQBotService.DEFAULT_NUDGE_PROBABILITY,
    @SerialName("disabled_groups")
    val disabledGroups: Set<Long> = emptySet(),
    @SerialName("disabled_templates")
    val disabledTemplates: Set<String> = emptySet(),
    @SerialName("image_cache_pool_size")
    val imageCachePoolSize: Int = 2048,
    @SerialName("group_cooldown_time")
    val groupCooldownTime: Long = Cooler.DEFAULT_GROUP_COOLDOWN,
    @SerialName("user_cooldown_time")
    val userCooldownTime: Long = Cooler.DEFAULT_USER_COOLDOWN,
    @SerialName("in_cooldown_message")
    val inCoolDownMessage: String = Cooler.DEFAULT_MESSAGE,

    @SerialName("auto_update")
    val autoUpdate: Boolean = true,
    @SerialName("repository_urls")
    val repositoryUrls: List<String> = listOf(),
    val headless: Boolean = true,
    @SerialName("gif_quality")
    override val gifQuality: Int = 10,
): BaseRenderConfig(
    gifQuality = gifQuality,
)

