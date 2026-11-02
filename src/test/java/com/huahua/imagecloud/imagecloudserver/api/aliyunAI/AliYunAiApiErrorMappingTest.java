package com.huahua.imagecloud.imagecloudserver.api.aliyunAI;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AliYunAiApiErrorMappingTest {

    @Test
    void mapsArrearageToReadableMessage() {
        String message = AliYunAiApi.resolveErrorMessage(
                400,
                "Arrearage",
                "Access denied, please make sure your account is in good standing.");

        assertEquals("阿里云百炼账户欠费或余额不足，请充值后再使用 AI 扩图", message);
    }

    @Test
    void keepsDashScopeMessageForOtherErrors() {
        String message = AliYunAiApi.resolveErrorMessage(
                400, "InvalidParameter", "base_image_url is invalid");

        assertEquals("AI 扩图失败：base_image_url is invalid", message);
    }
}
