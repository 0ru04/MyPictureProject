package com.huahua.imagecloud.imagecloudserver.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;


@Data
public class UserVO implements Serializable {

    private Long id;
    private String userAccount;
    private String userName;
    private String userProfile;
    private String userAvatar;

    private String userRole;
    private Date createTime ;
}
