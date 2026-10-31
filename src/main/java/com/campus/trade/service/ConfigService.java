package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.common.RedisCache;
import com.campus.trade.entity.SysConfig;
import com.campus.trade.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置（t_sys_config）：审核开关、自动下架天数等运营参数，Redis 缓存 5 分钟
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService extends ServiceImpl<SysConfigMapper, SysConfig> {

    public static final String AUDIT_ENABLED = "product.audit.enabled";

    public static final String EXPIRE_DAYS = "product.expire.days";

    public static final String IDENTITY_CHECK = "user.identity.check";

    private static final String CACHE_KEY = "config:all";

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final RedisCache redisCache;

    /** 读取全部配置（Map），带缓存 */
    public Map<String, String> all() {
        Map<String, String> cached = redisCache.get(CACHE_KEY,
                new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, String>>() {
                });
        if (cached != null) {
            return cached;
        }
        Map<String, String> map = new LinkedHashMap<>();
        List<SysConfig> list = list();
        for (SysConfig config : list) {
            map.put(config.getConfigKey(), config.getConfigValue());
        }
        redisCache.set(CACHE_KEY, map, CACHE_TTL);
        return map;
    }

    public String get(String key, String defaultValue) {
        String value = all().get(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public boolean getBool(String key, boolean defaultValue) {
        String value = all().get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 保存配置（不存在则新增），并清理缓存 */
    public void save(String key, String value, String remark) {
        SysConfig exists = getOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key), false);
        if (exists == null) {
            SysConfig config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setRemark(remark);
            save(config);
        } else {
            exists.setConfigValue(value);
            if (remark != null && !remark.isBlank()) {
                exists.setRemark(remark);
            }
            updateById(exists);
        }
        redisCache.delete(CACHE_KEY);
    }
}
