package com.campus.trade.controller;

import com.campus.trade.common.PageResult;
import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import com.campus.trade.dto.ChangePasswordRequest;
import com.campus.trade.dto.UpdateProfileRequest;
import com.campus.trade.security.LoginRequired;
import com.campus.trade.service.AuthService;
import com.campus.trade.service.BrowseHistoryService;
import com.campus.trade.service.FavoriteService;
import com.campus.trade.service.ProductService;
import com.campus.trade.service.UserService;
import com.campus.trade.vo.ProductVO;
import com.campus.trade.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 个人中心：资料、改密、收藏、浏览记录、卖家主页
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    private final UserService userService;

    private final FavoriteService favoriteService;

    private final BrowseHistoryService browseHistoryService;

    private final ProductService productService;

    @GetMapping("/me")
    @LoginRequired
    public Result<UserVO> me() {
        return Result.ok(authService.profile(UserContext.requireUserId()));
    }

    @PutMapping("/profile")
    @LoginRequired
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        authService.updateProfile(UserContext.requireUserId(), request);
        return Result.ok("资料已更新", null);
    }

    @PutMapping("/password")
    @LoginRequired
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(UserContext.requireUserId(), request);
        return Result.ok("密码修改成功，请重新登录", null);
    }

    @GetMapping("/favorites")
    @LoginRequired
    public Result<PageResult<ProductVO>> favorites(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "12") int size) {
        return Result.ok(favoriteService.myFavorites(UserContext.requireUserId(), page, size));
    }

    @GetMapping("/history")
    @LoginRequired
    public Result<PageResult<ProductVO>> history(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "12") int size) {
        return Result.ok(browseHistoryService.page(UserContext.requireUserId(), page, size));
    }

    @DeleteMapping("/history")
    @LoginRequired
    public Result<Void> clearHistory() {
        browseHistoryService.clear(UserContext.requireUserId());
        return Result.ok("浏览记录已清空", null);
    }

    /** 卖家主页：个人信息 */
    @GetMapping("/{id}")
    public Result<UserVO> seller(@PathVariable Long id) {
        return Result.ok(userService.sellerProfile(id));
    }

    /** 卖家主页：在售商品 */
    @GetMapping("/{id}/products")
    public Result<List<ProductVO>> sellerProducts(@PathVariable Long id) {
        return Result.ok(productService.listBySeller(id));
    }
}
