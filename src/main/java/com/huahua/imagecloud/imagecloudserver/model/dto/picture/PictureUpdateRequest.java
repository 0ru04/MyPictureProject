package com.huahua.imagecloud.imagecloudserver.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String introduction;

    private String category;

    /**
     * 标签
     * */

    private List<String> tags;

    private Long spaceId;











}
