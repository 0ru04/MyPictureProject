package com.huahua.imagecloud.imagecloudserver.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class LoginUserVO {
    /**
     * id
     * 告诉 MP 用雪花算法生成 id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */

    private String userAccount;

    // 返回的没有密码

    /**
     * 昵称
     */

    private String userName;

    /**
     * 用户头像
     */

    private String userAvatar;

    /**
     * 用户简介
     */

    private String userProfile;

    /**
     * 用户角色 user/admin
     */

    private String userRole;

    /**
     * 编辑时间
     */

    private Date editTime;

    /**
     * 创建时间
     */

    private Date createTime;

    /**
     * 更新时间
     */

    private Date updateTime;
}
