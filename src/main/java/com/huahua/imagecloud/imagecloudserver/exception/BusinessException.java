package com.huahua.imagecloud.imagecloudserver.exception;


import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{
    private final int code;
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    public BusinessException(ErrorCode code) {

        super(code.getMessage());
        this.code = code.getCode();
    }
    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code.getCode();
    }

}
