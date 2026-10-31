package com.campus.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.trade.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.campus.trade.vo.ChartItem;

import java.util.List;

public interface ProductMapper extends BaseMapper<Product> {

    /** 浏览量增量落库（Redis 定时刷新调用） */
    @Select("SELECT id, view_count FROM t_product WHERE deleted = 0")
    List<Product> selectViewCounts();

    @Update("UPDATE t_product SET view_count = view_count + #{delta} WHERE id = #{id}")
    int increaseViewCount(@Param("id") Long id, @Param("delta") long delta);

    /** 热门商品：浏览量 + 收藏*3 + 留言*5 加权 */
    @Select("SELECT id FROM t_product WHERE status = 1 AND deleted = 0 " +
            "ORDER BY (view_count + favorite_count * 3 + comment_count * 5) DESC LIMIT #{limit}")
    List<Long> selectHotIds(@Param("limit") int limit);

    /** 分类商品数量统计 */
    @Select("SELECT c.name AS name, COUNT(p.id) AS value FROM t_category c " +
            "LEFT JOIN t_product p ON p.category_id = c.id AND p.deleted = 0 " +
            "WHERE c.deleted = 0 GROUP BY c.id, c.name ORDER BY c.sort")
    List<ChartItem> selectCategoryStat();

    /** 近 N 天发布量趋势 */
    @Select("SELECT DATE_FORMAT(create_time, '%m-%d') AS name, COUNT(*) AS value FROM t_product " +
            "WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE_FORMAT(create_time, '%m-%d') ORDER BY name")
    List<ChartItem> selectPublishTrend(@Param("days") int days);

    /** 近 N 天注册量趋势 */
    @Select("SELECT DATE_FORMAT(create_time, '%m-%d') AS name, COUNT(*) AS value FROM t_user " +
            "WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE_FORMAT(create_time, '%m-%d') ORDER BY name")
    List<ChartItem> selectUserTrend(@Param("days") int days);
}
