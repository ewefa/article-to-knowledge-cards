package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.common.BizException;
import com.campus.trade.common.RedisCache;
import com.campus.trade.common.RedisKeys;
import com.campus.trade.dto.NoticeRequest;
import com.campus.trade.entity.Notice;
import com.campus.trade.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 轮播图与平台公告
 */
@Service
@RequiredArgsConstructor
public class NoticeService extends ServiceImpl<NoticeMapper, Notice> {

    public static final int TYPE_BANNER = 0;

    public static final int TYPE_NOTICE = 1;

    private final RedisCache redisCache;

    private final StringRedisTemplate redisTemplate;

    /** 首页轮播图，缓存 30 分钟 */
    public List<Notice> banners() {
        List<Notice> cached = redisCache.getList(RedisKeys.HOME_BANNERS, Notice.class);
        if (cached != null) {
            return cached;
        }
        List<Notice> list = list(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getType, TYPE_BANNER)
                .eq(Notice::getStatus, 1)
                .orderByAsc(Notice::getSort));
        redisCache.set(RedisKeys.HOME_BANNERS, list, Duration.ofMinutes(30));
        return list;
    }

    public List<Notice> notices() {
        return list(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getType, TYPE_NOTICE)
                .eq(Notice::getStatus, 1)
                .orderByDesc(Notice::getCreateTime));
    }

    public List<Notice> listAll(Integer type) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(type != null, Notice::getType, type)
                .orderByDesc(Notice::getCreateTime);
        return list(wrapper);
    }

    public Notice create(NoticeRequest request) {
        Notice notice = new Notice();
        apply(notice, request);
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        save(notice);
        evict();
        return notice;
    }

    public Notice update(Long id, NoticeRequest request) {
        Notice notice = getById(id);
        if (notice == null) {
            throw BizException.notFound("公告不存在");
        }
        apply(notice, request);
        notice.setUpdateTime(LocalDateTime.now());
        updateById(notice);
        evict();
        return notice;
    }

    public void delete(Long id) {
        if (getById(id) == null) {
            throw BizException.notFound("公告不存在");
        }
        removeById(id);
        evict();
    }

    public void evict() {
        redisTemplate.delete(RedisKeys.HOME_BANNERS);
    }

    private void apply(Notice notice, NoticeRequest request) {
        notice.setTitle(request.getTitle().trim());
        notice.setContent(request.getContent());
        notice.setImage(request.getImage());
        notice.setType(request.getType());
        notice.setSort(request.getSort() == null ? 99 : request.getSort());
        notice.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }
}
