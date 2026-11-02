package com.huahua.imagecloud.imagecloudserver.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {
    /**
     *  将 java对象序列化 持久化存储
     * 手动定义serialVersionUID 的值就是保证java存取对象一致 不会导致旧数据报废
     * */
    private static final long serialVersionUID = 1L;

    private String userAccount;
    private String userPassword;
    private String checkPassword;
}
