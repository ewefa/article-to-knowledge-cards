package com.campus.trade.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时把数据库中的明文密码（演示数据）加密为 BCrypt，保证种子账号可直接登录又不在库中存明文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordInitializer implements ApplicationRunner {

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getPassword));
        int count = 0;
        for (User user : users) {
            String password = user.getPassword();
            if (password == null || password.startsWith("$2a$") || password.startsWith("$2b$")
                    || password.startsWith("$2y$")) {
                continue;
            }
            User update = new User();
            update.setId(user.getId());
            update.setPassword(passwordEncoder.encode(password));
            userMapper.updateById(update);
            count++;
        }
        if (count > 0) {
            log.info("已将 {} 个历史明文密码升级为 BCrypt 加密存储", count);
        }
    }
}
