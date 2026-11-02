package com.huahua.imagecloud.imagecloudserver.model.dto.user;


import lombok.Data;
import org.springframework.stereotype.Component;

@Data
public class UserLoginRequest {
    private static final long serialVersionUID = 2L;

    private String userAccount;
    private String userPassword;
}
