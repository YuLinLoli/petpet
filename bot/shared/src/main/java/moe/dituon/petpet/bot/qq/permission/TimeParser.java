package moe.dituon.petpet.bot.qq.permission;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * TimeParser 类用于解析时间字符串到毫秒值
 * 它提供了一个灵活的方式来解析不同时间单位的时间字符串
 */
public class TimeParser {
    // 静态常量映射，定义了常见时间单位到毫秒的转换关系
    public static final Map<String, Long> TIME_UNIT_MAP = Map.of(
            "ms", 1L,
            "s", TimeUnit.SECONDS.toMillis(1),
            "sec", TimeUnit.SECONDS.toMillis(1),
            "m", TimeUnit.MINUTES.toMillis(1),
            "min", TimeUnit.MINUTES.toMillis(1),
            "h", TimeUnit.HOURS.toMillis(1),
            "hr", TimeUnit.HOURS.toMillis(1),
            "hour", TimeUnit.HOURS.toMillis(1),
            "d", TimeUnit.DAYS.toMillis(1),
            "day", TimeUnit.DAYS.toMillis(1)
    );

    // 可能会被自定义时间单位映射覆盖的实例变量
    protected final Map<String, Long> timeUnitMap;

    /**
     * 构造一个 TimeParser 实例
     * 如果提供的 timeUnitMap 为空、为空地图或为默认地图，则使用默认的 TIME_UNIT_MAP
     * 否则，将自定义地图与默认地图合并
     *
     * @param timeUnitMap 可选的自定义时间单位映射
     */
    public TimeParser(@Nullable Map<String, Long> timeUnitMap) {
        // 检查传入的timeUnitMap是否为null、空或等于预定义的TIME_UNIT_MAP
        if (timeUnitMap == null || timeUnitMap.isEmpty() || timeUnitMap == TIME_UNIT_MAP) {
            // 如果条件满足，则将当前对象的timeUnitMap设置为预定义的TIME_UNIT_MAP
            this.timeUnitMap = TIME_UNIT_MAP;
            // 并结束此方法的执行
            return;
        }
        // 如果传入的timeUnitMap不满足上述条件，则创建一个新的HashMap，大小为传入的timeUnitMap和预定义的TIME_UNIT_MAP的总大小
        this.timeUnitMap = new HashMap<>(timeUnitMap.size() + TIME_UNIT_MAP.size());
        // 将传入的timeUnitMap的所有键值对添加到当前对象的timeUnitMap中
        this.timeUnitMap.putAll(timeUnitMap);
        // 再将预定义的TIME_UNIT_MAP的所有键值对添加到当前对象的timeUnitMap中
        this.timeUnitMap.putAll(TIME_UNIT_MAP);
    }

    /**
     * 使用实例的时间单位映射解析时间字符串到毫秒值
     *
     * @param time 时间字符串
     * @return 解析后的毫秒值
     */
    public long parse(String time) {
        return parseTimeToMillis(time, timeUnitMap);
    }

    /**
     * 使用默认的时间单位映射解析时间字符串到毫秒值
     * 这是一个静态方法，允许在没有 TimeParser 实例的情况下解析时间
     *
     * @param time 时间字符串
     * @return 解析后的毫秒值
     */
    public static long parseTimeToMillis(String time) {
        return parseTimeToMillis(time, TIME_UNIT_MAP);
    }

    /**
     * 解析时间字符串到毫秒值，使用给定的时间单位映射
     * 如果时间字符串无效或时间单位不受支持，则抛出异常
     *
     * @param time       时间字符串
     * @param timeUnitMap 时间单位到毫秒的映射
     * @return 解析后的毫秒值
     * @throws IllegalArgumentException 如果时间字符串无效或时间单位不受支持
     */
    public static long parseTimeToMillis(String time, Map<String, Long> timeUnitMap) {
        // 检查时间字符串是否为空或空字符串
        if (time == null || time.isEmpty()) {
            throw new IllegalArgumentException("Time string cannot be null or empty");
        }

        // 移除前后空格并将时间字符串转换为小写，以简化后续处理
        time = time.trim().toLowerCase();

        // 查找第一个非数字字符的索引，用于分割时间值和时间单位
        int index = 0;
        while (index < time.length() && Character.isDigit(time.charAt(index))) {
            index++;
        }

        // 如果没有找到任何数字或整个字符串都是数字，则时间格式无效
        if (index == 0 || index == time.length()) {
            throw new IllegalArgumentException("Invalid time format: " + time);
        }

        // 解析时间值部分
        long value = Long.parseLong(time.substring(0, index));
        // 提取时间单位部分
        String unit = time.substring(index);

        // 查找时间单位对应的毫秒数乘法因子
        Long multiplier = timeUnitMap.get(unit);
        // 如果时间单位不受支持，则抛出异常
        if (multiplier == null) {
            throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }

        // 返回时间值与对应毫秒数的乘积
        return value * multiplier;
    }
}
