package moe.dituon.petpet.bot.qq.permission

import kotlinx.serialization.Serializable
import lombok.Builder
import moe.dituon.petpet.uitls.GlobalJson

/**
 * 该类用于定义群组权限配置，包含命令权限、编辑权限、禁用模板、戳一戳概率以及冷却时间等设置。
 *
 * 参数说明：
 * - commandPermission: 指定执行命令所需的权限，若为 null 则表示无限制。
 * - editPermission: 指定编辑操作所需的权限，若为 null 则表示无限制。
 * - disabledTemplates: 禁用的模板集合，若为 null 或空集则表示无禁用模板。
 * - nudgeProbability: 戳一戳触发的概率值，范围应在 0.0 至 1.0 之间，若为 null 则表示默认概率。
 * - cooldownTime: 冷却时间（单位为毫秒），若为 null 则表示无冷却时间限制。
 */
@Serializable
@Builder
data class GroupPermissionConfig(
    val commandPermission: String?,
    val editPermission: String?,
    val disabledTemplates: Set<String>?,
    val nudgeProbability: Float?,
    val cooldownTime: Long?,
) {

    /**
     * 使用Builder模式初始化配置对象的私有构造函数
     * 这个构造函数不直接对外暴露，而是通过Builder类的实例进行配置和创建对象
     * 这样做可以提供更灵活的配置方式，并且可以避免创建带有不合法或未初始化属性的对象
     *
     * @param builder Builder对象，包含了配置对象的所有必要属性
     */
    private constructor(builder: Builder) : this(
        commandPermission = builder.commandPermission,
        editPermission = builder.editPermission,
        disabledTemplates = builder.disabledTemplates,
        nudgeProbability = builder.nudgeProbability,
        cooldownTime = builder.cooldownTime
    )

    /**
     * 将当前对象转换为JSON字符串表示形式
     *
     * 此方法使用全局的JSON编码器来序列化当前对象实例它首先获取当前对象的序列化器，
     * 然后使用该序列化器将对象转换为JSON字符串这样做是为了确保对象的数据可以被
     * 以一种标准化、可读的方式转换和使用
     *
     * @return 当前对象的JSON字符串表示
     */
    fun toJsonString(): String {
        return GlobalJson.encodeToString(serializer(), this)
    }

    /**
     * 伴生对象，提供了与GroupPermissionConfig相关的静态方法
     */
    companion object {
        /**
         * 从JSON字符串解析GroupPermissionConfig对象
         *
         * @param str JSON字符串，应包含GroupPermissionConfig对象的所有必要信息
         * @return 解析得到的GroupPermissionConfig对象
         */
        @JvmStatic
        fun fromJsonString(str: String): GroupPermissionConfig =
            GlobalJson.decodeFromString(str)

        /**
         * 创建一个GroupPermissionConfig的Builder对象，用于构建GroupPermissionConfig实例
         *
         * @return 一个GroupPermissionConfig的Builder对象
         */
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        // 定义一个可空的字符串变量，用于存储命令权限
        var commandPermission: String? = null
            private set

        // 定义一个可空的字符串变量，用于存储编辑权限
        var editPermission: String? = null
            private set

        // 定义一个可空的字符串集合变量，用于存储禁用的模板
        var disabledTemplates: Set<String>? = null
            private set

        // 定义一个可空的浮点数变量，用于存储戳一戳概率
        var nudgeProbability: Float? = null
            private set

        // 定义一个可空的长整型变量，用于存储冷却时间
        var cooldownTime: Long? = null
            private set

        /**
         * 设置命令权限的函数
         * @param commandPermission 可空的字符串，代表新的命令权限
         * @return 返回当前对象，支持链式调用
         */
        fun commandPermission(commandPermission: String?) = apply {
            this.commandPermission = commandPermission
        }

        /**
         * 设置编辑权限的函数
         * @param editPermission 可空的字符串，代表新的编辑权限
         * @return 返回当前对象，支持链式调用
         */
        fun editPermission(editPermission: String?) = apply {
            this.editPermission = editPermission
        }

        /**
         * 设置禁用的模板
         * 当提供的禁用模板集合为空或null时，将禁用的模板设置为null；否则，将其设置为提供的集合
         * @param disabledTemplates 可能为空的禁用模板集合
         */
        fun disabledTemplates(disabledTemplates: Set<String>?) = apply {
            if (disabledTemplates.isNullOrEmpty()) {
                this.disabledTemplates = null
            } else {
                this.disabledTemplates = disabledTemplates
            }
        }

        /**
         * 设置推动概率
         * 推动概率是决定是否对用户进行提示的概率，以浮点数表示
         * @param nudgeProbability 可能为null的推动概率
         */
        fun nudgeProbability(nudgeProbability: Float?) = apply {
            this.nudgeProbability = nudgeProbability
        }

        /**
         * 设置冷却时间
         * 冷却时间是指在进行下一次推动之前需要等待的时间，以毫秒为单位
         * @param cooldownTime 可能为null的冷却时间
         */
        fun cooldownTime(cooldownTime: Long?) = apply {
            this.cooldownTime = cooldownTime
        }

        /**
         * 构建GroupPermissionConfig实例
         * 使用当前配置的禁用模板、推动概率和冷却时间来创建一个新的GroupPermissionConfig实例
         * @return GroupPermissionConfig实例
         */
        fun build() = GroupPermissionConfig(this)
    }
}
