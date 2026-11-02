package com.huahua.imagecloud.imagecloudserver.model.dto.picture;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * 用于接收来自前端请求参数的类
 * */
@Data
public class PictureUploadRequest implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 图片id（用于修改）
     */
    private Long id;

    private String url;
    private String thumbnailUrl;
    private Long spaceId;
    private String picName;
}
