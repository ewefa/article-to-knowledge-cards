package com.campus.trade.common;

/**
 * Redis Key 统一管理，避免散落在业务代码中
 */
public final class RedisKeys {

    /** 登录令牌：login:token:{userId} -> token（单点登录，新登录会覆盖旧令牌） */
    public static final String LOGIN_TOKEN = "login:token:%d";

    /** 短信验证码：sms:code:{phone} */
    public static final String SMS_CODE = "sms:code:%s";

    /** 短信发送频率限制：sms:limit:{phone} */
    public static final String SMS_LIMIT = "sms:limit:%s";

    /** 商品浏览量增量（Hash）：field = productId, value = 增量 */
    public static final String PRODUCT_VIEW_DELTA = "product:view:delta";

    /** 首页热门商品 ID 集合（ZSet） */
    public static final String HOT_IDS = "home:hot:ids";

    /** 首页热门商品列表缓存（JSON） */
    public static final String HOT_LIST = "home:hot:list";

    /** 首页轮播图缓存 */
    public static final String HOME_BANNERS = "home:banners";

    /** 分类列表缓存 */
    public static final String HOME_CATEGORIES = "home:categories";

    /** 商品详情缓存 */
    public static final String PRODUCT_DETAIL = "product:detail:%d";

    /** 用户收藏商品 ID 集合 */
    public static final String FAVORITE_IDS = "favorite:ids:%d";

    /** 用户信息缓存 */
    public static final String USER_INFO = "user:info:%d";

    /** 购物车（意向清单，Hash：productId -> 加入时间） */
    public static final String CART = "cart:%d";

    /** 数据看板缓存 */
    public static final String STAT_OVERVIEW = "stat:overview";

    /** 搜索热词（ZSet） */
    public static final String SEARCH_HOT = "search:hot";

    /** 接口限流 */
    public static final String RATE_LIMIT = "rate:%s";

    private RedisKeys() {
    }

    public static String loginToken(Long userId) {
        return String.format(LOGIN_TOKEN, userId);
    }

    public static String smsCode(String phone) {
        return String.format(SMS_CODE, phone);
    }

    public static String productDetail(Long productId) {
        return String.format(PRODUCT_DETAIL, productId);
    }

    public static String favoriteIds(Long userId) {
        return String.format(FAVORITE_IDS, userId);
    }

    public static String userInfo(Long userId) {
        return String.format(USER_INFO, userId);
    }

    public static String cart(Long userId) {
        return String.format(CART, userId);
    }
}
