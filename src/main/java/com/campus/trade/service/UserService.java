package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.common.BizException;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.ProductStatus;
import com.campus.trade.common.RedisKeys;
import com.campus.trade.dto.AdminQuery;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户相关业务：资料查询、卖家主页、管理员用户管理
 */
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final ProductMapper productMapper;

    private final StringRedisTemplate redisTemplate;

    public User getByAccount(String account) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getAccount, account), false);
    }

    public User getByPhone(String phone) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone), false);
    }

    public User requireUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return user;
    }

    /** 转换为前端展示对象，附带在售、已售数量 */
    public UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setAccount(user.getAccount());
        vo.setName(user.getName());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setDepartment(user.getDepartment());
        vo.setIntro(user.getIntro());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setIdentityStatus(user.getIdentityStatus());
        vo.setWechat(user.getWechat());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setContactPublic(user.getContactPublic());
        vo.setCreateTime(user.getCreateTime());
        vo.setOnSaleCount(countProducts(user.getId(), ProductStatus.ON_SALE));
        vo.setSoldCount(countProducts(user.getId(), ProductStatus.SOLD));
        return vo;
    }

    private long countProducts(Long userId, int status) {
        return productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getUserId, userId)
                .eq(Product::getStatus, status));
    }

    /** 手机号脱敏，仅展示前 3 位与后 4 位 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 卖家主页基础信息 */
    public UserVO sellerProfile(Long userId) {
        return toVO(requireUser(userId));
    }

    /** 管理员：用户分页查询 */
    public PageResult<UserVO> pageUsers(AdminQuery query) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(User::getAccount, keyword)
                    .or().like(User::getName, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (StringUtils.hasText(query.getRole())) {
            wrapper.eq(User::getRole, query.getRole());
        }
        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }
        if (query.getIdentityStatus() != null) {
            wrapper.eq(User::getIdentityStatus, query.getIdentityStatus());
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> page = page(Page.of(query.getPage(), query.getSize()), wrapper);
        List<UserVO> records = new ArrayList<>();
        for (User user : page.getRecords()) {
            records.add(toVO(user));
        }
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 管理员：启用（0）/ 禁用（1）用户，禁用后立即踢下线 */
    public void updateStatus(Long userId, Integer status) {
        User user = requireUser(userId);
        if ("ADMIN".equals(user.getRole())) {
            throw new BizException("不能操作管理员账号");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        if (status != null && status == 1) {
            redisTemplate.delete(RedisKeys.loginToken(userId));
        }
        redisTemplate.delete(RedisKeys.userInfo(userId));
    }

    /** 管理员：身份审核 */
    public void updateIdentity(Long userId, Integer identityStatus) {
        User user = requireUser(userId);
        user.setIdentityStatus(identityStatus);
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        redisTemplate.delete(RedisKeys.userInfo(userId));
    }

    public void updateProfile(Long userId, String nickname, String department, String intro,
                              String avatar, String wechat, Integer contactPublic) {
        User user = requireUser(userId);
        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname.trim());
        }
        if (department != null) {
            user.setDepartment(department.trim());
        }
        if (intro != null) {
            user.setIntro(intro.trim());
        }
        if (StringUtils.hasText(avatar)) {
            user.setAvatar(avatar.trim());
        }
        if (wechat != null) {
            user.setWechat(wechat.trim());
        }
        if (contactPublic != null) {
            user.setContactPublic(contactPublic);
        }
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        redisTemplate.delete(RedisKeys.userInfo(userId));
    }
}
