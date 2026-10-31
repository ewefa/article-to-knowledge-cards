package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.common.PageResult;
import com.campus.trade.entity.BrowseHistory;
import com.campus.trade.entity.Product;
import com.campus.trade.mapper.BrowseHistoryMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 浏览记录：同一商品只保留最新一条，最多保留 50 条
 */
@Service
@RequiredArgsConstructor
public class BrowseHistoryService extends ServiceImpl<BrowseHistoryMapper, BrowseHistory> {

    private static final int MAX_RECORDS = 50;

    private final ProductMapper productMapper;

    private final ProductService productService;

    @Transactional(rollbackFor = Exception.class)
    public void record(Long userId, Long productId) {
        if (productMapper.selectById(productId) == null) {
            return;
        }
        remove(new LambdaQueryWrapper<BrowseHistory>()
                .eq(BrowseHistory::getUserId, userId)
                .eq(BrowseHistory::getProductId, productId));
        BrowseHistory history = new BrowseHistory();
        history.setUserId(userId);
        history.setProductId(productId);
        history.setViewTime(LocalDateTime.now());
        save(history);

        List<BrowseHistory> all = list(new LambdaQueryWrapper<BrowseHistory>()
                .eq(BrowseHistory::getUserId, userId)
                .orderByDesc(BrowseHistory::getViewTime));
        if (all.size() > MAX_RECORDS) {
            List<Long> expired = new ArrayList<>();
            for (int i = MAX_RECORDS; i < all.size(); i++) {
                expired.add(all.get(i).getId());
            }
            removeByIds(expired);
        }
    }

    public PageResult<ProductVO> page(Long userId, int page, int size) {
        Page<BrowseHistory> result = page(Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 50)),
                new LambdaQueryWrapper<BrowseHistory>()
                        .eq(BrowseHistory::getUserId, userId)
                        .orderByDesc(BrowseHistory::getViewTime));
        List<BrowseHistory> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), result.getCurrent(), result.getSize());
        }
        List<Long> productIds = new ArrayList<>();
        for (BrowseHistory history : records) {
            productIds.add(history.getProductId());
        }
        List<Product> products = productMapper.selectBatchIds(productIds);
        products.sort((a, b) -> productIds.indexOf(a.getId()) - productIds.indexOf(b.getId()));
        return PageResult.of(productService.toVOList(products, userId), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    public void clear(Long userId) {
        remove(new LambdaQueryWrapper<BrowseHistory>().eq(BrowseHistory::getUserId, userId));
    }
}
