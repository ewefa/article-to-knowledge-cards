package com.campus.trade.vo;

import lombok.Data;

import java.util.List;

/** 管理员数据看板 */
@Data
public class StatVO {

    private long totalUsers;

    private long todayUsers;

    private long totalProducts;

    private long onSaleProducts;

    private long todayProducts;

    private long pendingProducts;

    private long totalViews;

    private long pendingReports;

    private long totalFavorites;

    private long totalComments;

    private List<ChartItem> categoryStat;

    private List<ChartItem> publishTrend;

    private List<ChartItem> userTrend;
}
