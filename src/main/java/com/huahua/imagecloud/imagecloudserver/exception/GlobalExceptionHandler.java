package com.huahua.imagecloud.imagecloudserver.exception;


import com.huahua.imagecloud.imagecloudserver.common.BaseResponse;
import com.huahua.imagecloud.imagecloudserver.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * */
@RestControllerAdvice //自动扫描并拦截所有的controller抛出的异常，无需在每个controller和service里写try-catch
@Slf4j  // lombok注解。自动生成一个名为 “log” 的日志对象，简化编写日志代码
public class GlobalExceptionHandler {

    /**
     * 专业捕获 ”业务异常“
     * 只要代码里抛出了 BusinessException Spring 就会立刻调用这个方法来处理
     * */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e){
        log.error("BusinessException",e);//日志打印框架，方便追查
        return  ResultUtils.error(e.getCode(),e.getMessage());
    }


    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e){
        log.error("RuntimeException",e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"系统错误");
    }
}
