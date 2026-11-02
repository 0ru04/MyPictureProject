package com.huahua.imagecloud.imagecloudserver.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 自定义注解---配合拦截器
 * */
//规定 @AuthCheck 这个注解只能加在方法上，加在类或字段上会报错。
@Target(ElementType.METHOD)
//想让代码在运行时识别注解@AuthCheck，必须选 RUNTIME，让权限效验 贯穿整个运行过程
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须有某个角色
     * */
    String mustRole() default "";

}
