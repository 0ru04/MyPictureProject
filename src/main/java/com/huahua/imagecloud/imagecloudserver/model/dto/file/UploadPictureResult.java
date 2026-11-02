package com.huahua.imagecloud.imagecloudserver.model.dto.file;

import lombok.Data;

/**
 * 图片解析信息包装类
 * */
@Data
public class UploadPictureResult {
    /**
     * 图片地址
     **/

    private String url;

    /**
     * 测试时使用
     * */
    private String originUrl;
    /**
     * 缩略图url
     * */
    private String thumbnailUrl;
    /**
     * 图片名称
     * */
    private String picName;

    /**
     * 文件体积
     * */
    private Long picSize;

    /**
     * 图片宽度
     * */
    private int picWidth;

    private int picHeight;

    /**
     * 图片宽高比
     * */
    private Double picScale;

    /**
     * 图片格式
     * */
    private String picFormat;

    /**
     * 图片主色调
     * */
    private String picColor;
}
