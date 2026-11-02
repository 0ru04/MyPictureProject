package com.huahua.imagecloud.imagecloudserver.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorCodeTest {

    @Test
    void keepsLoginAndParameterCodesSeparate() {
        assertEquals(40000, ErrorCode.PARAMS_ERROR.getCode());
        assertEquals(40100, ErrorCode.NOT_LOGIN_ERROR.getCode());
        assertEquals(40300, ErrorCode.NO_AUTH_ERROR.getCode());
        assertEquals(40400, ErrorCode.NOT_FOUND_ERROR.getCode());
    }
}
