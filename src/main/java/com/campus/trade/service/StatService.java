package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.common.RedisCache;
import com.campus.trade.common.RedisKeys;
import com.campus.trade.entity.Comment;
import com.campus.trade.entity.Favorite;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.Report;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.CommentMapper;
import com.campus.trade.mapper.FavoriteMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.ReportMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.vo.ChartItem;
import com.campus.trade.vo.StatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员数据看板：核心指标 + 图表数据，整体缓存 60 秒
 */
@Service
@RequiredArgsConstructor
public class StatService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final UserMapper userMapper;

    private final ProductMapper productMapper;

    private final ReportMapper reportMapper;

    private final FavoriteMapper favoriteMapper;

    private final CommentMapper commentMapper;

    private final RedisCache redisCache;

    public StatVO overview() {
        StatVO cached = redisCache.get(RedisKeys.STAT_OVERVIEW, StatVO.class);
        if (cached != null) {
            return cached;
        }
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        StatVO vo = new StatVO();
        vo.setTotalUsers(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "USER")));
        vo.setTodayUsers(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "USER").ge(User::getCreateTime, todayStart)));
        vo.setTotalProducts(productMapper.selectCount(null));
        vo.setOnSaleProducts(productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)));
        vo.setTodayProducts(productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .ge(Product::getCreateTime, todayStart)));
        vo.setPendingProducts(productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 0)));
        vo.setPendingReports(reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getStatus, 0)));
        vo.setTotalFavorites(favoriteMapper.selectCount(null));
        vo.setTotalComments(commentMapper.selectCount(null));

        java.util.List<java.util.Map<String, Object>> viewRows = productMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product>()
                        .select("IFNULL(SUM(view_count), 0) AS value"));
        long totalViews = 0L;
        if (!viewRows.isEmpty() && viewRows.get(0) != null) {
            Object value = viewRows.get(0).get("value");
            if (value instanceof Number number) {
                totalViews = number.longValue();
            }
        }
        vo.setTotalViews(totalViews);

        vo.setCategoryStat(productMapper.selectCategoryStat());
        vo.setPublishTrend(productMapper.selectPublishTrend(7));
        vo.setUserTrend(productMapper.selectUserTrend(7));
        redisCache.set(RedisKeys.STAT_OVERVIEW, vo, CACHE_TTL);
        return vo;
    }

    /** 数据变更后清理看板缓存 */
    public void evict() {
        redisCache.delete(RedisKeys.STAT_OVERVIEW);
    }
}
