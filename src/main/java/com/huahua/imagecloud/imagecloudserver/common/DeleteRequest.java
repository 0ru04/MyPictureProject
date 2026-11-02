package com.huahua.imagecloud.imagecloudserver.common;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 通用的删除请求类
 * */
@Data
public class DeleteRequest implements Serializable {
    private Long id;

    private static final long serialVersionUID = 1L;
}
