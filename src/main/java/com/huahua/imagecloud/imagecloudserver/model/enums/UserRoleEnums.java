package com.huahua.imagecloud.imagecloudserver.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 *用户角色这样的值的数量是有限的，可枚举的字段，定义一个枚举类
 * */
@Getter
public enum UserRoleEnums {
    /**
     *  public static final UserRoleEnums USER=new UserRoleEnum("用户","user")
     */

    USER("用户","user"),
    ADMIN("管理员","admin");

    private final String text;
    private final String value;

    UserRoleEnums(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * 注意：
     * 1. ObjUtil.isEmpty(value):
     *   这是一个工具类方法（可能是项目自定义的，类似 StringUtils.isEmpty 或 ObjectUtils.isEmpty）。
     *
     * 2. UserRoleEnums.values():
     *   这是 Java 枚举自带的静态方法。
     *   作用：返回一个数组，包含该枚举类中定义的所有实例（例如：[ADMIN, USER, GUEST]）
     * */
    public static UserRoleEnums getEnumByValue(String value) {

        //临界判断 ，判断value 是否为 null，或者是否为空字符串 ""，甚至可能包含纯空格。
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for (UserRoleEnums anEnum : UserRoleEnums.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        //for循环结束没有找到对应的枚举，则返回null
        return null;




    }
}
