package com.huahua.imagecloud.imagecloudserver.model.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    /**
     * 空间名称
     * */
    private String spaceName;
    /**
     * 空间级别
     *
     * */
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     * */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     * */
    private Long maxCount;
}
