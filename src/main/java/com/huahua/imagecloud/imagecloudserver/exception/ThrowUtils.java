package com.huahua.imagecloud.imagecloudserver.exception;

public class ThrowUtils {
    // 定义工具类。通常工具类不需要实例化，所有方法都是 static 静态方法
    public static void throwIf(boolean condition,RuntimeException runtimeException){
        if(condition){
            throw runtimeException;
        }
    }


    public static void throwIf(boolean condition,ErrorCode errorCode)
    {
        //只知道错误码枚举，不知道具体异常对象时使用。
        throwIf(condition,new BusinessException(errorCode));
    }

    public static void throwIf(boolean condition,ErrorCode errorCode,String message){
        throwIf(condition,new BusinessException(errorCode,message));
    }
}
