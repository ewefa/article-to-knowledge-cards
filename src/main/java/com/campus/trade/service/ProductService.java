package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.common.BizException;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.ProductStatus;
import com.campus.trade.common.RedisKeys;
import com.campus.trade.config.CampusProperties;
import com.campus.trade.dto.AdminQuery;
import com.campus.trade.dto.ProductRequest;
import com.campus.trade.entity.Category;
import com.campus.trade.entity.Favorite;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.ProductImage;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.CategoryMapper;
import com.campus.trade.mapper.FavoriteMapper;
import com.campus.trade.mapper.ProductImageMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品业务：发布、编辑、上下架、搜索、详情、审核
 */
@Service
@RequiredArgsConstructor
public class ProductService extends ServiceImpl<ProductMapper, Product> {

    private static final int MAX_IMAGES = 9;

    private final ProductImageMapper imageMapper;

    private final UserMapper userMapper;

    private final CategoryMapper categoryMapper;

    private final FavoriteMapper favoriteMapper;

    private final ConfigService configService;

    private final CampusProperties properties;

    private final StringRedisTemplate redisTemplate;

    /** 首页 / 搜索列表：只展示在售商品 */
    public PageResult<ProductVO> search(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                       Integer negotiable, String sort, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ProductStatus.ON_SALE);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Product::getTitle, kw).or().like(Product::getDescription, kw));
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (minPrice != null) {
            wrapper.ge(Product::getPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(Product::getPrice, maxPrice);
        }
        if (negotiable != null) {
            wrapper.eq(Product::getNegotiable, negotiable);
        }
        switch (sort == null ? "new" : sort) {
            case "priceAsc" -> wrapper.orderByAsc(Product::getPrice);
            case "priceDesc" -> wrapper.orderByDesc(Product::getPrice);
            case "hot" -> wrapper.orderByDesc(Product::getViewCount);
            default -> wrapper.orderByDesc(Product::getPublishTime).orderByDesc(Product::getId);
        }
        Page<Product> result = page(Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 60)), wrapper);
        return PageResult.of(toVOList(result.getRecords(), null), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    /** 热门商品：浏览量 + 收藏 * 3 + 留言 * 5 加权，定时任务把 ID 写入 Redis */
    public List<ProductVO> hot(int limit) {
        List<Long> ids = baseMapper.selectHotIds(limit);
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Product> products = listByIds(ids);
        products.sort((a, b) -> ids.indexOf(a.getId()) - ids.indexOf(b.getId()));
        return toVOList(products, null);
    }

    /** 卖家主页：某用户所有在售商品 */
    public List<ProductVO> listBySeller(Long sellerId) {
        List<Product> products = list(new LambdaQueryWrapper<Product>()
                .eq(Product::getUserId, sellerId)
                .eq(Product::getStatus, ProductStatus.ON_SALE)
                .orderByDesc(Product::getPublishTime));
        return toVOList(products, null);
    }

    /** 我的商品：按状态筛选 */
    public PageResult<ProductVO> pageMine(Long userId, Integer status, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getUserId, userId)
                .eq(status != null, Product::getStatus, status)
                .orderByDesc(Product::getCreateTime);
        Page<Product> result = page(Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 60)), wrapper);
        return PageResult.of(toVOList(result.getRecords(), userId), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    /** 管理员：商品查询（含各种状态） */
    public PageResult<ProductVO> pageAdmin(AdminQuery query) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(Product::getTitle, kw).or().like(Product::getDescription, kw));
        }
        if (query.getStatus() != null) {
            wrapper.eq(Product::getStatus, query.getStatus());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, query.getCategoryId());
        }
        wrapper.orderByDesc(Product::getCreateTime);
        Page<Product> result = page(Page.of(Math.max(query.getPage(), 1),
                Math.min(Math.max(query.getSize(), 1), 100)), wrapper);
        return PageResult.of(toVOList(result.getRecords(), null), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    /** 商品详情：累加浏览量（Redis），返回数据库值 + 未落库增量 */
    public ProductVO detail(Long productId, Long viewerId, boolean isAdmin) {
        Product product = getById(productId);
        if (product == null) {
            throw BizException.notFound("商品不存在或已被删除");
        }
        boolean owner = viewerId != null && viewerId.equals(product.getUserId());
        if (product.getStatus() != ProductStatus.ON_SALE && !owner && !isAdmin) {
            throw BizException.notFound("商品已下架或已售出");
        }
        if (product.getStatus() == ProductStatus.ON_SALE) {
            redisTemplate.opsForHash().increment(RedisKeys.PRODUCT_VIEW_DELTA, String.valueOf(productId), 1);
        }
        List<ProductVO> list = toVOList(List.of(product), viewerId);
        ProductVO vo = list.get(0);
        Object delta = redisTemplate.opsForHash().get(RedisKeys.PRODUCT_VIEW_DELTA, String.valueOf(productId));
        long extra = delta == null ? 0 : Long.parseLong(delta.toString());
        vo.setViewCount((product.getViewCount() == null ? 0 : product.getViewCount()) + (int) extra);
        return vo;
    }

    /** 发布商品：按配置决定「直接上架」还是「待审核」 */
    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long userId, ProductRequest request, Integer identityStatus) {
        if (identityStatus != null && identityStatus == 0) {
            throw new BizException("您的身份正在审核中，暂时无法发布商品");
        }
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null || (category.getStatus() != null && category.getStatus() == 0)) {
            throw new BizException("商品分类不存在或已停用");
        }
        boolean audit = configService.getBool(ConfigService.AUDIT_ENABLED,
                properties.getProduct().isAuditEnabled());
        List<String> images = normalizeImages(request.getImages());

        Product product = new Product();
        product.setUserId(userId);
        product.setCategoryId(request.getCategoryId());
        product.setTitle(request.getTitle().trim());
        product.setDescription(request.getDescription());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setPrice(request.getPrice());
        product.setNegotiable(request.getNegotiable() == null ? 0 : request.getNegotiable());
        product.setTradePlace(request.getTradePlace());
        product.setCover(images.isEmpty() ? null : images.get(0));
        product.setStatus(audit ? ProductStatus.PENDING : ProductStatus.ON_SALE);
        product.setViewCount(0);
        product.setFavoriteCount(0);
        product.setCommentCount(0);
        product.setPublishTime(LocalDateTime.now());
        if (!audit) {
            product.setExpireTime(expireAt());
        }
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        save(product);
        saveImages(product.getId(), images);
        evictHomeCache();
        return product.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long productId, ProductRequest request) {
        Product product = requireOwner(userId, productId);
        if (product.getStatus() == ProductStatus.SOLD) {
            throw new BizException("已售出的商品不能编辑");
        }
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new BizException("商品分类不存在");
        }
        List<String> images = normalizeImages(request.getImages());
        product.setCategoryId(request.getCategoryId());
        product.setTitle(request.getTitle().trim());
        product.setDescription(request.getDescription());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setPrice(request.getPrice());
        product.setNegotiable(request.getNegotiable() == null ? 0 : request.getNegotiable());
        product.setTradePlace(request.getTradePlace());
        product.setCover(images.isEmpty() ? null : images.get(0));
        // 重新编辑后，若开启审核需要再次审核
        boolean audit = configService.getBool(ConfigService.AUDIT_ENABLED,
                properties.getProduct().isAuditEnabled());
        if (audit && product.getStatus() != ProductStatus.PENDING) {
            product.setStatus(ProductStatus.PENDING);
            product.setPublishTime(null);
            product.setExpireTime(null);
        }
        product.setUpdateTime(LocalDateTime.now());
        updateById(product);
        imageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId));
        saveImages(productId, images);
        evictHomeCache();
    }

    /** 卖家上下架 / 标记已售出：status = 1 上架, 2 下架, 3 已售出 */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long userId, Long productId, int status) {
        Product product = requireOwner(userId, productId);
        if (status != ProductStatus.ON_SALE && status != ProductStatus.OFF_SHELF
                && status != ProductStatus.SOLD) {
            throw new BizException("不支持的商品状态");
        }
        if (product.getStatus() == ProductStatus.SOLD && status != ProductStatus.SOLD) {
            throw new BizException("已售出的商品不能重新上架");
        }
        if (status == ProductStatus.ON_SALE) {
            boolean audit = configService.getBool(ConfigService.AUDIT_ENABLED,
                    properties.getProduct().isAuditEnabled());
            if (audit && product.getStatus() == ProductStatus.OFF_SHELF) {
                product.setStatus(ProductStatus.PENDING);
                product.setRejectReason(null);
                updateById(product);
                return;
            }
            product.setPublishTime(LocalDateTime.now());
            product.setExpireTime(expireAt());
            product.setRejectReason(null);
        }
        product.setStatus(status);
        product.setUpdateTime(LocalDateTime.now());
        updateById(product);
        evictHomeCache();
        redisTemplate.delete(RedisKeys.productDetail(productId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMine(Long userId, Long productId) {
        requireOwner(userId, productId);
        removeById(productId);
        evictHomeCache();
        redisTemplate.delete(RedisKeys.productDetail(productId));
    }

    /** 管理员审核：通过则上架，驳回需填写理由 */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long productId, boolean pass, String reason) {
        Product product = getById(productId);
        if (product == null) {
            throw BizException.notFound("商品不存在");
        }
        if (product.getStatus() != ProductStatus.PENDING) {
            throw new BizException("该商品不在待审核状态");
        }
        if (pass) {
            product.setStatus(ProductStatus.ON_SALE);
            product.setPublishTime(LocalDateTime.now());
            product.setExpireTime(expireAt());
            product.setRejectReason(null);
        } else {
            product.setStatus(ProductStatus.REJECTED);
            product.setRejectReason(StringUtils.hasText(reason) ? reason : "内容不符合平台规范");
        }
        product.setUpdateTime(LocalDateTime.now());
        updateById(product);
        evictHomeCache();
    }

    /** 管理员强制下架（违规） */
    public void forceOffline(Long productId, String reason) {
        Product product = getById(productId);
        if (product == null) {
            throw BizException.notFound("商品不存在");
        }
        product.setStatus(ProductStatus.VIOLATION);
        product.setRejectReason(StringUtils.hasText(reason) ? reason : "违规商品，强制下架");
        product.setUpdateTime(LocalDateTime.now());
        updateById(product);
        evictHomeCache();
    }

    public void adminDelete(Long productId) {
        if (getById(productId) == null) {
            throw BizException.notFound("商品不存在");
        }
        removeById(productId);
        evictHomeCache();
    }

    private Product requireOwner(Long userId, Long productId) {
        Product product = getById(productId);
        if (product == null) {
            throw BizException.notFound("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            throw BizException.forbidden("只能操作自己发布的商品");
        }
        return product;
    }

    private LocalDateTime expireAt() {
        int days = configService.getInt(ConfigService.EXPIRE_DAYS, properties.getProduct().getExpireDays());
        return LocalDateTime.now().plusDays(days <= 0 ? 30 : days);
    }

    private List<String> normalizeImages(List<String> images) {
        if (images == null) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        for (String image : images) {
            if (StringUtils.hasText(image) && !list.contains(image.trim())) {
                list.add(image.trim());
            }
        }
        if (list.size() > MAX_IMAGES) {
            throw new BizException("最多上传 " + MAX_IMAGES + " 张图片");
        }
        return list;
    }

    private void saveImages(Long productId, List<String> images) {
        for (int i = 0; i < images.size(); i++) {
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setUrl(images.get(i));
            image.setSort(i);
            image.setCreateTime(LocalDateTime.now());
            imageMapper.insert(image);
        }
    }

    /** 批量组装 VO，避免 N+1 查询 */
    public List<ProductVO> toVOList(List<Product> products, Long viewerId) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        List<Long> productIds = new ArrayList<>();
        for (Product product : products) {
            userIds.add(product.getUserId());
            if (product.getCategoryId() != null) {
                categoryIds.add(product.getCategoryId());
            }
            productIds.add(product.getId());
        }
        Map<Long, User> users = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIds)) {
                users.put(user.getId(), user);
            }
        }
        Map<Long, String> categories = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            for (Category category : categoryMapper.selectBatchIds(categoryIds)) {
                categories.put(category.getId(), category.getName());
            }
        }
        Map<Long, List<String>> images = new HashMap<>();
        List<ProductImage> imageList = imageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                .in(ProductImage::getProductId, productIds)
                .orderByAsc(ProductImage::getSort));
        for (ProductImage image : imageList) {
            images.computeIfAbsent(image.getProductId(), k -> new ArrayList<>()).add(image.getUrl());
        }
        Set<Long> favorites = new HashSet<>();
        if (viewerId != null) {
            List<Favorite> favoriteList = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, viewerId)
                    .in(Favorite::getProductId, productIds));
            for (Favorite favorite : favoriteList) {
                favorites.add(favorite.getProductId());
            }
        }
        List<ProductVO> result = new ArrayList<>();
        for (Product product : products) {
            ProductVO vo = new ProductVO();
            vo.setId(product.getId());
            vo.setUserId(product.getUserId());
            vo.setCategoryId(product.getCategoryId());
            vo.setCategoryName(categories.get(product.getCategoryId()));
            vo.setTitle(product.getTitle());
            vo.setDescription(product.getDescription());
            vo.setOriginalPrice(product.getOriginalPrice());
            vo.setPrice(product.getPrice());
            vo.setNegotiable(product.getNegotiable());
            vo.setTradePlace(product.getTradePlace());
            vo.setCover(product.getCover());
            List<String> urls = images.get(product.getId());
            vo.setImages(urls == null ? (product.getCover() == null ? Collections.emptyList()
                    : List.of(product.getCover())) : urls);
            vo.setStatus(product.getStatus());
            vo.setStatusText(ProductStatus.text(product.getStatus()));
            vo.setRejectReason(product.getRejectReason());
            vo.setViewCount(product.getViewCount());
            vo.setFavoriteCount(product.getFavoriteCount());
            vo.setCommentCount(product.getCommentCount());
            vo.setPublishTime(product.getPublishTime());
            vo.setExpireTime(product.getExpireTime());
            vo.setCreateTime(product.getCreateTime());
            User seller = users.get(product.getUserId());
            if (seller != null) {
                vo.setSellerName(StringUtils.hasText(seller.getNickname()) ? seller.getNickname() : seller.getName());
                vo.setSellerAvatar(seller.getAvatar());
                vo.setSellerDepartment(seller.getDepartment());
                if (seller.getContactPublic() != null && seller.getContactPublic() == 1) {
                    vo.setWechat(seller.getWechat());
                    vo.setSellerPhone(seller.getPhone());
                }
            }
            vo.setFavorited(favorites.contains(product.getId()));
            vo.setOwner(viewerId != null && viewerId.equals(product.getUserId()));
            result.add(vo);
        }
        return result;
    }

    public void evictHomeCache() {
        redisTemplate.delete(List.of(RedisKeys.HOT_LIST, RedisKeys.HOT_IDS));
    }
}
