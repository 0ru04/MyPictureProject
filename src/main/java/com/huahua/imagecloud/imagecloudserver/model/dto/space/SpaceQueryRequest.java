package com.huahua.imagecloud.imagecloudserver.model.dto.space;

import com.baomidou.mybatisplus.annotation.TableField;
import com.huahua.imagecloud.imagecloudserver.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;
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
