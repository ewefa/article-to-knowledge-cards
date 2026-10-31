-- ============================================================
-- 校园二手交易平台 数据库脚本
-- MySQL 8.0+ / utf8mb4
-- 说明：线上沟通、线下当面交易，不涉及在线支付，因此没有订单/支付表
-- ============================================================

DROP DATABASE IF EXISTS campus_trade;
CREATE DATABASE campus_trade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE campus_trade;

-- ------------------------------------------------------------
-- 用户表：游客注册后为普通用户（师生），role=ADMIN 为管理员
-- ------------------------------------------------------------
CREATE TABLE t_user
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    account         VARCHAR(32)  NOT NULL COMMENT '学号/工号（登录账号）',
    name            VARCHAR(32)  NOT NULL COMMENT '真实姓名',
    nickname        VARCHAR(32)           DEFAULT NULL COMMENT '昵称',
    phone           VARCHAR(20)  NOT NULL COMMENT '手机号',
    password        VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
    avatar          VARCHAR(255)          DEFAULT NULL COMMENT '头像地址',
    department      VARCHAR(64)           DEFAULT NULL COMMENT '院系',
    intro           VARCHAR(255)          DEFAULT NULL COMMENT '个人简介',
    role            VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER-普通用户 ADMIN-管理员',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-正常 1-禁用',
    identity_status TINYINT      NOT NULL DEFAULT 1 COMMENT '身份审核：0-待审核 1-已认证 2-驳回',
    wechat          VARCHAR(64)           DEFAULT NULL COMMENT '微信号',
    contact_public  TINYINT      NOT NULL DEFAULT 1 COMMENT '是否公开联系方式：0-否 1-是',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account (account),
    KEY idx_phone (phone),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB COMMENT ='用户表';

-- ------------------------------------------------------------
-- 商品分类表
-- ------------------------------------------------------------
CREATE TABLE t_category
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    name        VARCHAR(32) NOT NULL COMMENT '分类名称',
    icon        VARCHAR(255)         DEFAULT NULL COMMENT '分类图标',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0-隐藏 1-显示',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_sort (sort)
) ENGINE = InnoDB COMMENT ='商品分类表';

-- ------------------------------------------------------------
-- 商品表
-- status: 0-待审核 1-在售 2-已下架 3-已售出 4-审核驳回 5-违规下架
-- ------------------------------------------------------------
CREATE TABLE t_product
(
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    user_id        BIGINT         NOT NULL COMMENT '卖家ID',
    category_id    BIGINT         NOT NULL COMMENT '分类ID',
    title          VARCHAR(100)   NOT NULL COMMENT '商品标题',
    description    TEXT COMMENT '商品描述',
    original_price DECIMAL(10, 2)          DEFAULT NULL COMMENT '原价',
    price          DECIMAL(10, 2) NOT NULL COMMENT '售价',
    negotiable     TINYINT        NOT NULL DEFAULT 0 COMMENT '是否可议价：0-否 1-是',
    trade_place    VARCHAR(64)             DEFAULT NULL COMMENT '校内交易地点',
    cover          VARCHAR(255)            DEFAULT NULL COMMENT '封面图',
    status         TINYINT        NOT NULL DEFAULT 0 COMMENT '状态：0-待审核 1-在售 2-已下架 3-已售出 4-审核驳回 5-违规下架',
    reject_reason  VARCHAR(255)            DEFAULT NULL COMMENT '驳回/违规原因',
    view_count     INT            NOT NULL DEFAULT 0 COMMENT '浏览量（Redis 增量定时落库）',
    favorite_count INT            NOT NULL DEFAULT 0 COMMENT '收藏数',
    comment_count  INT            NOT NULL DEFAULT 0 COMMENT '留言数',
    publish_time   DATETIME                DEFAULT NULL COMMENT '上架时间',
    expire_time    DATETIME                DEFAULT NULL COMMENT '过期自动下架时间',
    create_time    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_status_category (status, category_id),
    KEY idx_user (user_id),
    KEY idx_create_time (create_time),
    KEY idx_view (view_count),
    KEY idx_title (title)
) ENGINE = InnoDB COMMENT ='商品表';

-- ------------------------------------------------------------
-- 商品图片表（一个商品多张图）
-- ------------------------------------------------------------
CREATE TABLE t_product_image
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    product_id  BIGINT       NOT NULL COMMENT '商品ID',
    url         VARCHAR(255) NOT NULL COMMENT '图片地址',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_product (product_id)
) ENGINE = InnoDB COMMENT ='商品图片表';

-- ------------------------------------------------------------
-- 收藏表
-- ------------------------------------------------------------
CREATE TABLE t_favorite
(
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    product_id  BIGINT   NOT NULL COMMENT '商品ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id),
    KEY idx_product (product_id)
) ENGINE = InnoDB COMMENT ='商品收藏表';

-- ------------------------------------------------------------
-- 留言表：parent_id=0 为商品留言，否则为对某条留言的回复
-- ------------------------------------------------------------
CREATE TABLE t_comment
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '留言ID',
    product_id    BIGINT       NOT NULL COMMENT '商品ID',
    user_id       BIGINT       NOT NULL COMMENT '留言人ID',
    parent_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '父留言ID，0 表示直接留言',
    reply_user_id BIGINT                DEFAULT NULL COMMENT '被回复用户ID',
    content       VARCHAR(500) NOT NULL COMMENT '留言内容',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '留言时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_product (product_id),
    KEY idx_parent (parent_id)
) ENGINE = InnoDB COMMENT ='商品留言表';

-- ------------------------------------------------------------
-- 站内消息表
-- type: COMMENT-商品被留言  REPLY-留言被回复  SYSTEM-系统通知  REPORT-举报处理结果
-- ------------------------------------------------------------
CREATE TABLE t_message
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    user_id     BIGINT       NOT NULL COMMENT '接收人ID',
    type        VARCHAR(20)  NOT NULL COMMENT '消息类型',
    title       VARCHAR(100) NOT NULL COMMENT '消息标题',
    content     VARCHAR(500)          DEFAULT NULL COMMENT '消息内容',
    related_id  BIGINT                DEFAULT NULL COMMENT '关联业务ID',
    is_read     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_read (user_id, is_read),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB COMMENT ='站内消息表';

-- ------------------------------------------------------------
-- 举报表
-- status: 0-待处理 1-已处理 2-已驳回
-- ------------------------------------------------------------
CREATE TABLE t_report
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '举报ID',
    product_id    BIGINT       NOT NULL COMMENT '被举报商品ID',
    user_id       BIGINT       NOT NULL COMMENT '举报人ID',
    reason        VARCHAR(50)  NOT NULL COMMENT '举报理由',
    description   VARCHAR(500)          DEFAULT NULL COMMENT '补充说明',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-待处理 1-已处理 2-已驳回',
    handle_remark VARCHAR(255)          DEFAULT NULL COMMENT '处理说明',
    handler_id    BIGINT                DEFAULT NULL COMMENT '处理人ID',
    handle_time   DATETIME              DEFAULT NULL COMMENT '处理时间',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_product (product_id)
) ENGINE = InnoDB COMMENT ='商品举报表';

-- ------------------------------------------------------------
-- 首页轮播图 / 平台公告
-- type: 0-轮播图 1-公告
-- ------------------------------------------------------------
CREATE TABLE t_notice
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    title       VARCHAR(100) NOT NULL COMMENT '标题',
    content     TEXT COMMENT '内容',
    image       VARCHAR(255)          DEFAULT NULL COMMENT '图片地址（轮播图）',
    type        TINYINT      NOT NULL DEFAULT 0 COMMENT '类型：0-轮播图 1-公告',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-下线 1-上线',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_type_status (type, status)
) ENGINE = InnoDB COMMENT ='轮播图与公告表';

-- ------------------------------------------------------------
-- 浏览记录表
-- ------------------------------------------------------------
CREATE TABLE t_browse_history
(
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    user_id    BIGINT   NOT NULL COMMENT '用户ID',
    product_id BIGINT   NOT NULL COMMENT '商品ID',
    view_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近浏览时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id),
    KEY idx_view_time (view_time)
) ENGINE = InnoDB COMMENT ='商品浏览记录表';

-- ------------------------------------------------------------
-- 系统配置表：审核开关、自动下架天数等
-- ------------------------------------------------------------
CREATE TABLE t_sys_config
(
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    config_key   VARCHAR(64) NOT NULL COMMENT '配置键',
    config_value VARCHAR(255) NOT NULL COMMENT '配置值',
    remark       VARCHAR(255)         DEFAULT NULL COMMENT '说明',
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_key (config_key)
) ENGINE = InnoDB COMMENT ='系统配置表';
