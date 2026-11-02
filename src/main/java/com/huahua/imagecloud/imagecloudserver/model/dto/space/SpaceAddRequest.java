package com.huahua.imagecloud.imagecloudserver.model.dto.space;

import lombok.Data;

import java.io.Serializable;
@Data
public class SpaceAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 空间名称
     * */
    private String spaceName;
    /**
     * 空间级别
     *
     * */
    private Integer spaceLevel;


}
