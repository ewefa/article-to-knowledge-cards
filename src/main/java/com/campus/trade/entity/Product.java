package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 二手商品实体
 */
@Data
@TableName("t_product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long categoryId;

    private String title;

    private String description;

    private BigDecimal originalPrice;

    private BigDecimal price;

    private Integer negotiable;

    private String tradePlace;

    private String cover;

    private Integer status;

    private String rejectReason;

    private Integer viewCount;

    private Integer favoriteCount;

    private Integer commentCount;

    private LocalDateTime publishTime;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
