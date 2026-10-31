package com.campus.trade.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductVO {

    private Long id;

    private Long userId;

    private String sellerName;

    private String sellerAvatar;

    private String sellerDepartment;

    private Long categoryId;

    private String categoryName;

    private String title;

    private String description;

    private BigDecimal originalPrice;

    private BigDecimal price;

    private Integer negotiable;

    private String tradePlace;

    private String cover;

    private List<String> images;

    private Integer status;

    private String statusText;

    private String rejectReason;

    private Integer viewCount;

    private Integer favoriteCount;

    private Integer commentCount;

    private LocalDateTime publishTime;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    /** 当前登录用户是否已收藏 */
    private Boolean favorited;

    /** 当前登录用户是否为卖家 */
    private Boolean owner;

    /** 卖家联系方式（仅详情页且卖家公开时返回） */
    private String wechat;

    private String sellerPhone;
}
