-- ============================================================
-- 校园二手交易平台 初始化数据
-- 说明：为保证可直接演示，初始账号密码统一为 123456，
--       首次启动时 PasswordInitializer 会自动把明文密码加密为 BCrypt。
-- ============================================================
USE campus_trade;

-- 用户（1 个管理员 + 5 个师生）
INSERT INTO t_user (id, account, name, nickname, phone, password, avatar, department, intro, role, status, identity_status, wechat, contact_public, create_time)
VALUES (1, 'admin', '平台管理员', '管理员', '13800000000', '123456', NULL, '信息中心', '负责平台日常审核与运营', 'ADMIN', 0, 1, NULL, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)),
       (2, '2021001', '张三', '爱看书的小张', '13800000001', '123456', '/images/avatar/1.png', '计算机与信息学院', '大二，喜欢折腾数码，闲置物品低价出～', 'USER', 0, 1, 'zhangsan_wx', 1, DATE_SUB(NOW(), INTERVAL 30 DAY)),
       (3, '2021002', '李四', '体育生小李', '13800000002', '123456', '/images/avatar/2.png', '体育学院', '健身、篮球爱好者，出手闲置运动装备', 'USER', 0, 1, 'lisi_wx', 1, DATE_SUB(NOW(), INTERVAL 25 DAY)),
       (4, '2021003', '王五', '设计系小王', '13800000003', '123456', '/images/avatar/3.png', '艺术设计学院', '喜欢淘好物，也爱出闲置', 'USER', 0, 1, NULL, 0, DATE_SUB(NOW(), INTERVAL 20 DAY)),
       (5, 'T2020001', '赵老师', '赵老师', '13800000004', '123456', '/images/avatar/4.png', '外国语学院', '教师，偶尔处理家中闲置', 'USER', 0, 1, 'zhao_wx', 1, DATE_SUB(NOW(), INTERVAL 15 DAY)),
       (6, '2021004', '孙七', '数码控', '13800000005', '123456', '/images/avatar/5.png', '电子信息学院', '数码产品发烧友', 'USER', 0, 0, 'sun_wx', 1, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- 商品分类
INSERT INTO t_category (id, name, icon, sort, status)
VALUES (1, '书籍教材', '📚', 1, 1),
       (2, '数码电子', '💻', 2, 1),
       (3, '服饰鞋包', '👕', 3, 1),
       (4, '生活用品', '🧺', 4, 1),
       (5, '运动器材', '🏀', 5, 1),
       (6, '代步工具', '🚲', 6, 1),
       (7, '美妆个护', '💄', 7, 1),
       (8, '其他', '📦', 8, 1);

-- 轮播图与公告
INSERT INTO t_notice (id, title, content, image, type, sort, status)
VALUES (1, '新学期二手书市', '开学季教材专场，教辅、考研资料低价淘，图书馆门前当面交易。', '/images/banner/banner1.png', 0, 1, 1),
       (2, '校内当面交易更安心', '平台仅支持本校师生实名注册，请优先选择食堂门口、图书馆等公开场所交易。', '/images/banner/banner2.png', 0, 2, 1),
       (3, '文明交易公约', '禁止发布违规物品、虚假信息；一经核实将下架商品并封禁账号。', '/images/banner/banner3.png', 0, 3, 1),
       (4, '平台使用说明', '发布商品需填写真实信息，建议上传实物照片；交易完成后请卖家及时标记「已售出」。', NULL, 1, 1, 1),
       (5, '关于超期商品自动下架', '商品上架满 30 天仍未售出将自动下架，可重新编辑后再次上架。', NULL, 1, 2, 1);

-- 商品（覆盖各分类与各种状态）
INSERT INTO t_product (id, user_id, category_id, title, description, original_price, price, negotiable, trade_place, cover, status, reject_reason, view_count, favorite_count, comment_count, publish_time, expire_time, create_time, update_time)
VALUES (1, 2, 1, '高等数学（第七版）上下册 教材 笔记少', '大一高数教材，同济第七版上下册，只有少量勾画，无缺页。期末复习用它考了 90+，转给需要的学弟学妹。', 89.00, 18.00, 1, '图书馆一楼大厅', '/images/products/p1.jpg', 1, NULL, 326, 2, 2, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_ADD(NOW(), INTERVAL 21 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),
       (2, 4, 1, '考研英语红宝书 词汇+真题 全套', '2025 版考研英语红宝书，词汇书和真题册，做过一部分真题（铅笔），适合二刷。', 68.00, 25.00, 1, '三食堂门口', '/images/products/p2.jpg', 1, NULL, 158, 1, 0, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_ADD(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
       (3, 6, 2, 'iPad Air 5 64G 深空灰 99新', '去年双十一购入，日常看课件和记笔记，贴膜带壳无划痕，电池健康 96%。原装充电器齐，可当面验机。', 4399.00, 2800.00, 1, '宿舍区 3 号楼门口', '/images/products/p3.jpg', 1, NULL, 642, 5, 1, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 23 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
       (4, 2, 2, '机械键盘 87 键 茶轴 白色', '自用键盘，茶轴手感好，带白光背光，键帽无磨损。因为换了静电容所以出掉。', 399.00, 150.00, 1, '图书馆一楼大厅', '/images/products/p4.jpg', 1, NULL, 211, 3, 0, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 24 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
       (5, 3, 3, '优衣库 男款轻型羽绒服 L 码 藏青', '冬天买大了一码，只穿过两次，无污渍无破损，吊牌已拆。适合 175 左右。', 599.00, 120.00, 1, '体育馆门口', '/images/products/p5.jpg', 1, NULL, 97, 1, 0, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
       (6, 3, 3, '耐克 Air Force 1 42 码 经典白', '鞋柜吃灰，上脚 5 次以内，鞋底轻微氧化，已清洗干净。42 码偏大半码。', 799.00, 260.00, 1, '体育馆门口', '/images/products/p6.jpg', 1, NULL, 183, 2, 0, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 26 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
       (7, 5, 4, '宿舍小冰箱 50L 静音款', '毕业清宿舍，50L 双门小冰箱，制冷正常噪音小，需自提（宿舍区 5 号楼 2 楼）。', 499.00, 200.00, 1, '宿舍区 5 号楼', '/images/products/p7.jpg', 1, NULL, 274, 4, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
       (8, 4, 4, '便携榨汁杯 全新未拆封', '活动赠品，家里已经有了，全新未拆封，USB 充电便携款。', 99.00, 35.00, 0, '三食堂门口', '/images/products/p8.jpg', 1, NULL, 66, 0, 0, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 27 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
       (9, 3, 5, '斯伯丁 7 号篮球 室内外通用', '打了一个学期，气足无鼓包，纹路清晰，附赠打气筒。', 159.00, 60.00, 1, '篮球场', '/images/products/p9.jpg', 1, NULL, 143, 2, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
       (10, 3, 5, '迪卡侬登山杖 一对 铝合金', '去年爬山买的，用过两次，可调节长度，收纳方便。', 129.00, 45.00, 1, '体育馆门口', '/images/products/p10.jpg', 1, NULL, 51, 0, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
       (11, 2, 6, '永久牌自行车 26 寸 带车锁', '骑了一年多，刚换过刹车皮和链条，车况良好，送 U 型锁。毕业出，需自行骑走。', 899.00, 350.00, 1, '东门车棚', '/images/products/p11.jpg', 1, NULL, 468, 6, 2, DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_ADD(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
       (12, 6, 6, '折叠电动滑板车 续航 25km', '通勤代步用，续航实测 25km 左右，折叠方便可进电梯，电池健康良好。', 1599.00, 600.00, 1, '东门车棚', '/images/products/p12.jpg', 1, NULL, 388, 3, 0, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 24 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
       (13, 4, 7, '兰蔻粉水 400ml 未开封 专柜小票', '生日礼物，肤质不合适所以转出，未开封，附专柜小票可验。', 480.00, 220.00, 1, '图书馆一楼大厅', '/images/products/p13.jpg', 1, NULL, 129, 1, 0, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 29 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
       (14, 2, 2, '罗技 G102 游戏鼠标 二代', '已售出，感谢支持～手感轻，适合小手。', 129.00, 55.00, 0, '图书馆一楼大厅', '/images/products/p14.jpg', 3, NULL, 205, 0, 0, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_ADD(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
       (15, 6, 2, '小米充电宝 20000mAh 22.5W', '买了新的所以出闲置，循环次数少，容量正常，附数据线。', 199.00, 80.00, 1, '宿舍区 1 号楼门口', '/images/products/p15.jpg', 0, NULL, 0, 0, 0, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
       (16, 5, 4, '懒人沙发 单人 灰色', '用了一年，布面有轻微起球，已清洁。占地方所以低价出。', 299.00, 60.00, 1, '宿舍区 5 号楼', '/images/products/p16.jpg', 2, NULL, 88, 0, 0, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 商品图片
INSERT INTO t_product_image (product_id, url, sort)
VALUES (1, '/images/products/p1.jpg', 0), (1, '/images/products/p1b.jpg', 1),
       (2, '/images/products/p2.jpg', 0), (2, '/images/products/p2b.jpg', 1),
       (3, '/images/products/p3.jpg', 0), (3, '/images/products/p3b.jpg', 1),
       (4, '/images/products/p4.jpg', 0), (4, '/images/products/p4b.jpg', 1),
       (5, '/images/products/p5.jpg', 0), (5, '/images/products/p5b.jpg', 1),
       (6, '/images/products/p6.jpg', 0), (6, '/images/products/p6b.jpg', 1),
       (7, '/images/products/p7.jpg', 0),
       (8, '/images/products/p8.jpg', 0),
       (9, '/images/products/p9.jpg', 0),
       (10, '/images/products/p10.jpg', 0),
       (11, '/images/products/p11.jpg', 0), (11, '/images/products/p11b.jpg', 1),
       (12, '/images/products/p12.jpg', 0),
       (13, '/images/products/p13.jpg', 0),
       (14, '/images/products/p14.jpg', 0),
       (15, '/images/products/p15.jpg', 0),
       (16, '/images/products/p16.jpg', 0);

-- 留言与回复
INSERT INTO t_comment (id, product_id, user_id, parent_id, reply_user_id, content, create_time)
VALUES (1, 1, 4, 0, NULL, '请问笔记是用铅笔写的吗？会不会影响复习？', DATE_SUB(NOW(), INTERVAL 8 DAY)),
       (2, 1, 2, 1, 4, '是铅笔，可以擦掉，重点章节我用荧光笔标了几页，不影响使用～', DATE_SUB(NOW(), INTERVAL 8 DAY)),
       (3, 1, 5, 0, NULL, '教材还在吗？明天下午方便在图书馆交易吗？', DATE_SUB(NOW(), INTERVAL 6 DAY)),
       (4, 3, 4, 0, NULL, '电池健康可以现场用爱思看吗？', DATE_SUB(NOW(), INTERVAL 5 DAY)),
       (5, 7, 2, 0, NULL, '冰箱能帮忙搬到楼下吗？', DATE_SUB(NOW(), INTERVAL 4 DAY)),
       (6, 11, 6, 0, NULL, '车锁是原装的吗？刹车是碟刹还是 V 刹？', DATE_SUB(NOW(), INTERVAL 3 DAY)),
       (7, 11, 2, 6, 6, '锁是后配的 U 型锁，前轮 V 刹后轮也是 V 刹，刚换过刹车皮。', DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 站内消息
INSERT INTO t_message (user_id, type, title, content, related_id, is_read, create_time)
VALUES (2, 'COMMENT', '你的商品收到新留言', '李四 留言：教材还在吗？明天下午方便在图书馆交易吗？', 1, 0, DATE_SUB(NOW(), INTERVAL 6 DAY)),
       (2, 'COMMENT', '你的商品收到新留言', '数码控 留言：车锁是原装的吗？刹车是碟刹还是 V 刹？', 11, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
       (4, 'REPLY', '你的留言收到回复', '爱看书的小张 回复：是铅笔，可以擦掉……', 1, 1, DATE_SUB(NOW(), INTERVAL 8 DAY)),
       (3, 'COMMENT', '你的商品收到新留言', '设计系小王 留言：冰箱能帮忙搬到楼下吗？', 7, 0, DATE_SUB(NOW(), INTERVAL 4 DAY)),
       (2, 'SYSTEM', '欢迎加入校园二手交易平台', '发布闲置请填写真实信息，交易请选择校内公开场所，祝交易愉快！', NULL, 1, DATE_SUB(NOW(), INTERVAL 30 DAY));

-- 举报（1 条待处理）
INSERT INTO t_report (product_id, user_id, reason, description, status, create_time)
VALUES (5, 4, '虚假信息', '图片和描述明显不是同一件衣服，疑似网图，建议核实。', 0, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- 收藏
INSERT INTO t_favorite (user_id, product_id, create_time)
VALUES (2, 3, DATE_SUB(NOW(), INTERVAL 5 DAY)),
       (2, 11, DATE_SUB(NOW(), INTERVAL 6 DAY)),
       (4, 1, DATE_SUB(NOW(), INTERVAL 8 DAY)),
       (4, 13, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 浏览记录
INSERT INTO t_browse_history (user_id, product_id, view_time)
VALUES (2, 3, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
       (2, 11, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
       (4, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
       (4, 13, DATE_SUB(NOW(), INTERVAL 3 HOUR));

-- 系统配置
INSERT INTO t_sys_config (config_key, config_value, remark)
VALUES ('product.audit.enabled', '0', '发布商品是否需要管理员审核：1-需要 0-直接上架'),
       ('product.expire.days', '30', '商品上架多少天后自动下架'),
       ('user.identity.check', '1', '新注册用户是否需要管理员审核身份：1-需要 0-注册即可发布'),
       ('platform.name', '校园二手交易平台', '平台名称'),
       ('platform.notice', '仅限本校师生使用，线上沟通、线下当面交易。', '平台公告语');
