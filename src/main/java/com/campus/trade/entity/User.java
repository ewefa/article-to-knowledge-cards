package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.LocalDateTime;

/**
 * 用户实体（师生 / 管理员）
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String account;

    private String name;

    private String nickname;

    private String phone;

    private String password;

    private String avatar;

    private String department;

    private String intro;

    private String role;

    private Integer status;

    private Integer identityStatus;

    private String wechat;

    private Integer contactPublic;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
