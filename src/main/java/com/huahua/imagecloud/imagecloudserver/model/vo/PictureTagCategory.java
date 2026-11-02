package com.huahua.imagecloud.imagecloudserver.model.vo;


import lombok.Data;

import java.util.List;

/**
 *
 * 图片标签分类列表视图
 * */
@Data
public class PictureTagCategory {

    /**
     * 标签列表：是图片的“属性”或“关键词”
     * */
    private List<String> tagList;

    /**
     * 分类列表:是图片的“归属类别”

     * */
    private List<String> categoryList;
}
