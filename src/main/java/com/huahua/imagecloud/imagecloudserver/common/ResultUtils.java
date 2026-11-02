package com.huahua.imagecloud.imagecloudserver.common;

import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;

public class ResultUtils {
    /**
     * 成功
     * 响应数据
     *
     * */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0,"ok",data);
    }

    /**
     * 失败
     * 响应错误码和错误信息
     * */

    public static BaseResponse<?> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     * 响应自定义错误码和自定义信息
     * */
    public static BaseResponse<?>error(int code,String message){
        return new BaseResponse<>(code,message,null);
    }

    /**
     * 失败
     * 响应错误码和自定义消息
     * */
    public static BaseResponse<?> error(ErrorCode errorCode,String message){
        return new BaseResponse<>(errorCode.getCode(),message,null);
    }


}
