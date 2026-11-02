package com.huahua.imagecloud.imagecloudserver.model.dto.picture;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.huahua.imagecloud.imagecloudserver.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class PictureQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String introduction;

    private String category;

    /**
     * 标签
     * */

    private List<String> tags;

    private  Long picSize;

    private Integer picWidth;
    private Integer picHeight;

    /**
     * 图片比例
     * */
    private Double picScale;

    private String picFormat;

    /**
     * 搜索词（同时搜名称，简介等）
     * */
    private String searchText;

    private Long userId;

    /**
     * 状态：0-待审核，1-通过，2-拒绝
     * */
    private Integer reviewStatus;

    /**
     * 审核信息
     * */

    private String reviewMessage;

    /**
     * 审核人ID
     * */

    private Long reviewerId;

    /**
     * 空间 id
     * */

    private Long spaceId;

    /**
     * 是否只查询spaceId 为 null 的数据
     * */
    private boolean nullSpaceId;

    /**
     * 开始编辑时间
     * */
    private Date startEditTime;

    /**
     * 结束编辑时间
     * */
    private Date endEditTime;

}
