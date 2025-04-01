package moe.dituon.petpet.bot.qq.mirai

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import moe.dituon.petpet.bot.TemplateExtraMetadata
import net.mamoe.mirai.contact.Member
import net.mamoe.yamlkt.Yaml

/**
 * 获取成员名称
 * 如果成员对象为null，则返回"未知用户"
 * 否则，优先返回成员的昵称，如果昵称为空，则返回成员的用户名
 *
 * @param member 可能为null的成员对象
 * @return 成员的昵称或用户名，如果都为空则默认为"未知用户"
 */
fun getMemberName(member: Member?): String {
    // 检查成员对象是否为null，如果为null则返回默认用户名"未知用户"
    if (member == null) return "未知用户"
    // 返回成员的昵称，如果昵称为空，则返回成员的用户名
    return member.nameCard.ifEmpty { member.nick }
}


/**
 * 自定义元数据配置序列化器对象
 * 实现了KSerializer接口，用于Kotlin的Map到JSON的序列化和反序列化
 * 具体来说，这个序列化器处理的是String到TemplateExtraMetadata对象的映射
 */
object CustomMetadataConfigSerializer: KSerializer<Map<String, TemplateExtraMetadata>> {
    // 定义一个Map序列化器，用于后续的序列化和反序列化操作
    private val mapSerializer = MapSerializer(String.serializer(), TemplateExtraMetadata.serializer())

    // 实现KSerializer接口的descriptor属性，提供序列化描述符
    override val descriptor: SerialDescriptor = mapSerializer.descriptor

    /**
     * 序列化实现函数
     * 将给定的Map对象转换为JSON格式
     *
     * @param encoder 编码器，用于输出序列化后的数据
     * @param value 待序列化的Map对象，映射String到TemplateExtraMetadata
     */
    override fun serialize(encoder: Encoder, value: Map<String, TemplateExtraMetadata>) {
        // 使用mapSerializer进行序列化，确保键值按顺序排列
        mapSerializer.serialize(encoder, value.toSortedMap())
    }

    /**
     * 反序列化实现函数
     * 将JSON格式的数据转换回Map对象
     *
     * @param decoder 解码器，用于读取序列化数据
     * @return 反序列化后的Map对象，映射String到TemplateExtraMetadata
     */
    override fun deserialize(decoder: Decoder): Map<String, TemplateExtraMetadata> {
        // 使用mapSerializer进行反序列化
        return mapSerializer.deserialize(decoder)
    }
}


/**
 * 解析自定义模板元数据配置字符串
 * 该函数使用YAML格式将字符串解析为映射对象这种映射对象的键是字符串，值是TemplateExtraMetadata对象
 * 主要用于将配置文件或配置数据加载到内存中以便于程序访问和使用
 *
 * @param str 待解析的YAML格式字符串
 * @return 解析后的映射对象，键为字符串，值为TemplateExtraMetadata对象
 */
fun decodeCustomMetadataConfig(str: String): Map<String, TemplateExtraMetadata> =
    Yaml.decodeFromString(CustomMetadataConfigSerializer, str)

/**
 * 将自定义模板元数据配置映射编码为YAML字符串
 * 该函数将一个映射对象（其键为字符串，值为TemplateExtraMetadata对象）编码为YAML格式的字符串
 * 主要用于将内存中的配置数据转换为字符串形式，以便于存储或传输
 *
 * @param map 待编码的映射对象，键为字符串，值为TemplateExtraMetadata对象
 * @return 编码后的YAML格式字符串
 */
fun encodeCustomMetadataConfig(map: Map<String, TemplateExtraMetadata>): String =
    Yaml { encodeDefaultValues = false }.encodeToString(CustomMetadataConfigSerializer, map)
