package moe.dituon.petpet.bot

import kotlinx.serialization.Serializable
import moe.dituon.petpet.template.Metadata

@Serializable
/**
 * TemplateExtraMetadata 是一个数据类，用于存储模板的额外元数据信息。
 * 这些信息包括模板的别名、标签、作者、描述、是否隐藏、是否出现在随机列表中以及预览图片。
 */
data class TemplateExtraMetadata(
    // 模板的别名列表，默认为空列表
    val alias: List<String> = emptyList(),
    // 模板的标签列表，默认为空列表
    val tags: List<String> = emptyList(),
    // 模板的作者，默认为空字符串
    val author: String = "",
    // 模板的描述，默认为空字符串
    val desc: String = "",
    // 模板是否隐藏，默认为不隐藏
    val hidden: Boolean = false,
    // 模板是否出现在随机列表中，默认为是
    val inRandomList: Boolean = true,
    // 模板的预览图片链接，允许为空
    val preview: String? = null
) {
    /**
     * 将 TemplateExtraMetadata 转换为 Metadata 对象。
     * 这个函数 essentially 创建一个新的 Metadata 实例，使用当前对象的属性值进行初始化。
     *
     * @return 一个包含当前模板额外元数据的 Metadata 对象
     */
    fun toMetadata(): Metadata {
        return Metadata(
            alias = alias,
            tags = tags,
            author = author,
            desc = desc,
            hidden = hidden,
            inRandomList = inRandomList,
            preview = preview
        )
    }

    /**
     * 伴生对象，提供从 Metadata 对象创建 TemplateExtraMetadata 实例的能力。
     */
    companion object {
        /**
         * 从一个 Metadata 对象创建一个 TemplateExtraMetadata 实例。
         * 这个函数 essentially 创建一个新的 TemplateExtraMetadata 实例，使用传入的 Metadata 对象的属性值进行初始化。
         *
         * @param metadata 要转换的 Metadata 对象
         * @return 一个包含与传入的 Metadata 对象相同数据的 TemplateExtraMetadata 实例
         */
        @JvmStatic
        fun fromMetadata(metadata: Metadata) = TemplateExtraMetadata(
            alias = metadata.alias,
            tags = metadata.tags,
            author = metadata.author,
            desc = metadata.desc,
            hidden = metadata.hidden,
            inRandomList = metadata.inRandomList,
            preview = metadata.preview
        )
    }
}
