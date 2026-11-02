package com.huahua.imagecloud.imagecloudserver.model.dto.picture;

import lombok.Data;

import java.util.List;

@Data
public class PictureEditRequest {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String introduction;

    private String category;

    private Long spaceId;
    /**
     * 标签
     * */

    private List<String> tags;

}
