package com.huahua.imagecloud.imagecloudserver.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum SpaceLevelEnum {
    COMMON("普通版",0,100,100L*1024*1024),
    PROFESSIONAL("专业版",1,1000,1000L*1024*1024),
    FLAGSHIP("旗舰版",2,10000,10000L*1024*1024);

    /**
     * 文本
     * */
    private final String text;

    /**
     * 值
     * */
    private final int  value;

    /**
     * 最大图片数量
     * */
    private final long maxCount;

    /**
     * 空间存储限额
     * */
    private final long maxSize;

    SpaceLevelEnum(String text, int value, long maxCount, long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    /**
     * 根据value获取枚举
     * */
    public static SpaceLevelEnum getEnumByValue(Integer value) {
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for (SpaceLevelEnum b : SpaceLevelEnum.values()) {
            if (b.value == value) {
                return b;
            }
        }
        return null;
    }




}
