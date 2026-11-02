package com.huahua.imagecloud.imagecloudserver.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.huahua.imagecloud.imagecloudserver.model.entity.Space;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

@Data
public class SpaceVO implements Serializable {


    private Long id;
    private String spaceName;
    private Integer spaceLevel;
    private Long maxSize;
    private Long maxCount;
    private Long totalSize;
    private Long totalCount;
    private Long userId;
    private Date createTime;
    private Date updateTime;
    private UserVO user;

    /**
     * 封装类转对象
     * */
    public static Space voToObj(SpaceVO spaceVO){
        if(spaceVO == null){
            return null;
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceVO,space);
        return space;
    }

    /**
     * 对象转封装类
     * */
    public static SpaceVO objToVo(Space space){
        if(space == null){
            return null;
        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space,spaceVO);
        return spaceVO;
    }
}
