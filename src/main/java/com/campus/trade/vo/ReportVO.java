package com.campus.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportVO {

    private Long id;

    private Long productId;

    private String productTitle;

    private String productCover;

    private Long userId;

    private String reporterName;

    private Long sellerId;

    private String sellerName;

    private String reason;

    private String description;

    private Integer status;

    private String statusText;

    private String handleRemark;

    private LocalDateTime handleTime;

    private LocalDateTime createTime;
}
