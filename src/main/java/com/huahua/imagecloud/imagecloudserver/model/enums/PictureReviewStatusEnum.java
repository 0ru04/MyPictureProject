package com.huahua.imagecloud.imagecloudserver.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Data;
import lombok.Getter;

@Getter
public enum PictureReviewStatusEnum {
    PEVIEWING("待审核",0),
    PASS("通过",1),
    REJECT("拒绝",2);

    private final String text;
    private final int value;

    PictureReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     */
    public static PictureReviewStatusEnum getEnumByValue(int value) {
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for (PictureReviewStatusEnum item : PictureReviewStatusEnum.values()) {
            if (item.value == value) {
                return item;
            }
        }
        return null;
    }















}
