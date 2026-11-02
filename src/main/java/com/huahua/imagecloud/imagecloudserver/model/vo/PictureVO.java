package com.huahua.imagecloud.imagecloudserver.model.vo;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.huahua.imagecloud.imagecloudserver.model.entity.Picture;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * 文件上传成功后返回给前端的响应类，是一个视图包装类，可以额外关联上传图片的用户信息
 * 实现Picture实体类与该VO类的转换方法
 * */
@Data
public class PictureVO implements Serializable {

    private static final long serialVersionUID = 1L;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String url;

    private String thumbnailUrl;

    private String name;

    private String introduction;

    /**
     * 标签
     * */
    private List<String > tags;
    /**
     * 分类
     * */
    private String category;

    /**
     * 文件体积
     * */
    private Long picSize;

    private Integer picWidth;

    private Integer picHeight;
    /**
     * 图片比例
     * */
    private Double picScale;

    /**
     * 图片格式
     * */
    private String picFormat;

    /**
     * 用户id
     * */
    private Long userId;

    private Date editTime;
    private Date createTime;
    private Date updateTime;

    /**
     * 创建用户信息
     *
     * */
    private UserVO user;

    /**
     * 空间 id
     * */
    private Long spaceId;

    /**
     * 图片主色调
     * */
    private String picColor;


    /**
     * PictureVO类 转 Picture 实体类
     * voToObj（封装类转对象）这个动作的核心目的，就是为了把前端传过来的“业务数据”清洗、转换、格式化，
     * 变成数据库能“吃得下”且“存得对”的格式
     * */
    public static Picture voToObj(PictureVO pictureVO) {
        if (pictureVO == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureVO, picture);
        //类型不同，需要转换
        /**
         * 前端传递 ["风景", "旅游"] (Java List)   ===>  数据库存储"[\"风景\",\"旅游\"]" (Java String)
         * */
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
        return picture;
    }

    /**
     * Picture实体对象 转 PictureVO类
     * 将数据库中的“原始存储数据”转换为前端易于使用的“展示数据”
     *
     * 数据库存储"[\"风景\",\"旅游\"]"  ===> ["风景", "旅游"]
     * String.class 是希望取出来的 list 列表 数据类型是 String 类型，即 List<String>
     *
     * */
    public static PictureVO objToVo(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtils.copyProperties(picture, pictureVO);

        //类型不同，需要转换
        pictureVO.setTags(JSONUtil.toList(picture.getTags(),String.class));
        return pictureVO;
    }
}
