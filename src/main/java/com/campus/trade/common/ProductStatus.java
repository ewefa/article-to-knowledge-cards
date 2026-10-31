package com.campus.trade.common;

/**
 * 商品状态常量
 */
public final class ProductStatus {

    /** 待审核 */
    public static final int PENDING = 0;
    /** 在售 */
    public static final int ON_SALE = 1;
    /** 已下架 */
    public static final int OFF_SHELF = 2;
    /** 已售出 */
    public static final int SOLD = 3;
    /** 审核驳回 */
    public static final int REJECTED = 4;
    /** 违规下架 */
    public static final int VIOLATION = 5;

    private ProductStatus() {
    }

    public static String text(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case PENDING -> "待审核";
            case ON_SALE -> "在售";
            case OFF_SHELF -> "已下架";
            case SOLD -> "已售出";
            case REJECTED -> "审核驳回";
            case VIOLATION -> "违规下架";
            default -> "未知";
        };
    }
}
