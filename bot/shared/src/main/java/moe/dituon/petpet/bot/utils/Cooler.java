package moe.dituon.petpet.bot.utils;

import java.util.concurrent.ConcurrentHashMap;
/**
 * 冷却器类，用于管理对象的冷却时间
 */
public class Cooler {
    // 默认的用户冷却时间
    public static final Long DEFAULT_USER_COOLDOWN = 1000L;
    // 默认的组冷却时间
    public static final Long DEFAULT_GROUP_COOLDOWN = 0L;
    // 默认的冷却中消息
    public static final String DEFAULT_MESSAGE = "技能冷却中...";
    // 存储对象最后冷却时间的映射表
    private static final ConcurrentHashMap<Object, Long> coolDownMap = new ConcurrentHashMap<>(128);
    // 存储对象锁定时间的映射表
    private static final ConcurrentHashMap<Object, Long> lockTimeMap = new ConcurrentHashMap<>(128);

    /**
     * 锁定指定对象
     *
     * @param uid 要锁定的对象的唯一标识
     * @param lockTime 对象的锁定时间，单位为毫秒
     */
    public static void lock(Object uid, long lockTime) {
        // 如果锁定时间小于等于0，则不执行锁定操作
        if (lockTime <= 0L) return;
        // 更新或添加对象的冷却时间和锁定时间
        coolDownMap.put(uid, System.currentTimeMillis());
        lockTimeMap.put(uid, lockTime);
    }

    /**
     * 检查指定对象是否被锁定
     *
     * @param uid 要检查的对象的唯一标识
     * @return 如果对象被锁定且锁定未到期，则返回true，否则返回false
     */
    public static boolean isLocked(Object uid) {
        // 如果冷却时间映射表或锁定时间映射表不包含该对象，则对象未被锁定
        if (!coolDownMap.containsKey(uid) || !lockTimeMap.containsKey(uid)) return false;
        // 判断当前时间与对象冷却时间的差值是否小于等于对象的锁定时间
        return (System.currentTimeMillis() - coolDownMap.get(uid)) <= lockTimeMap.get(uid);
    }
}
