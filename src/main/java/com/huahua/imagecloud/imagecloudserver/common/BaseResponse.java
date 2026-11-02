package com.huahua.imagecloud.imagecloudserver.common;


import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;


/**
 * 定义统一的响应包装类BaseResponse
 * 格式：
 * {
 *   "code": 0,          // 状态码
 *   "message": "ok",    // 提示信息
 *   "data": { ... }     // 具体业务数据（泛型 T）
 * }
 * */
@Data
public class BaseResponse<T> implements Serializable {

    //响应状态码，对应ErrorCode枚举
    private int code;

    //响应消息，成功时为“ok”， 失败时为具体的错误描述（eg："参数错误"）
    private String message;

    //核心数据载体 存放具体的业务数据（用户信息，列表数据）类型由调用者决定
    private T data;


    public BaseResponse(int code,String message,T data){
        this.code = code;
        this.message = message;
        this.data = data;

    }

    // 简化构造函数
    public BaseResponse(int code,T data){
        //this 关键字在这里代表“调用本类中的另一个构造函数”,避免忘记传递 message 而导致编译错误
        this(code,"",data);

    }

    //错误专用构造函数
    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(),errorCode.getMessage(),null);

    }

}
