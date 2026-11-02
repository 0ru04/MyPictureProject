package com.huahua.imagecloud.imagecloudserver.api.aliyunAI;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model.CreateOutPaintingTaskRequest;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model.CreateOutPaintingTaskResponse;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model.GetOutPaintingTaskResponse;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {

    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    public static final String CREATE_OUT_PAINTING_TASK_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/image-synthesis";

    public static final String GET_OUT_PAINTING_TASK_URL =
            "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    public CreateOutPaintingTaskResponse createOutPaintingTask(
            CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "扩图参数不能为空");
        }

        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));

        try (HttpResponse httpResponse = httpRequest.execute()) {
            String responseBody = httpResponse.body();
            CreateOutPaintingTaskResponse response = parseCreateTaskResponse(responseBody);

            if (!httpResponse.isOk()) {
                throw buildDashScopeException(
                        httpResponse.getStatus(), response, responseBody);
            }
            if (response == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROE,
                        "AI 扩图接口返回内容无法解析");
            }
            if (StrUtil.isNotBlank(response.getCode())) {
                throw buildDashScopeException(
                        httpResponse.getStatus(), response, responseBody);
            }
            if (response.getOutput() == null
                    || StrUtil.isBlank(response.getOutput().getTaskId())) {
                log.error("DashScope 创建扩图任务未返回 taskId，response={}", responseBody);
                throw new BusinessException(ErrorCode.OPERATION_ERROE,
                        "AI 扩图接口未返回任务 ID");
            }
            return response;
        }
    }

    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务 ID 不能为空");
        }

        try (HttpResponse httpResponse = HttpRequest.get(
                        String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            String responseBody = httpResponse.body();
            if (!httpResponse.isOk()) {
                log.error("获取扩图任务失败，status={}，response={}",
                        httpResponse.getStatus(), responseBody);
                throw new BusinessException(ErrorCode.OPERATION_ERROE,
                        "获取 AI 扩图任务失败，HTTP " + httpResponse.getStatus());
            }

            GetOutPaintingTaskResponse response = JSONUtil.toBean(
                    responseBody, GetOutPaintingTaskResponse.class);
            fillOutputImageUrl(response);
            return response;
        }
    }

    private CreateOutPaintingTaskResponse parseCreateTaskResponse(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return null;
        }
        try {
            return JSONUtil.toBean(responseBody, CreateOutPaintingTaskResponse.class);
        } catch (Exception e) {
            log.error("解析 DashScope 响应失败，response={}", responseBody, e);
            return null;
        }
    }

    private BusinessException buildDashScopeException(
            int status, CreateOutPaintingTaskResponse response, String responseBody) {
        String code = response == null ? null : response.getCode();
        String message = response == null ? null : response.getMessage();
        String requestId = response == null ? null : response.getRequestId();
        log.error("DashScope 创建扩图任务失败，status={}，code={}，message={}，requestId={}，response={}",
                status, code, message, requestId, responseBody);

        return new BusinessException(ErrorCode.OPERATION_ERROE,
                resolveErrorMessage(status, code, message));
    }

    static String resolveErrorMessage(int status, String code, String message) {
        if ("Arrearage".equalsIgnoreCase(code)) {
            return "阿里云百炼账户欠费或余额不足，请充值后再使用 AI 扩图";
        }
        if (StrUtil.isNotBlank(message)) {
            return "AI 扩图失败：" + message;
        }
        return "AI 扩图失败，阿里云接口返回 HTTP " + status;
    }

    private void fillOutputImageUrl(GetOutPaintingTaskResponse response) {
        if (response == null || response.getOutput() == null
                || response.getOutput().getResults() == null) {
            return;
        }
        for (GetOutPaintingTaskResponse.Result result : response.getOutput().getResults()) {
            if (result != null && StrUtil.isNotBlank(result.getUrl())) {
                response.getOutput().setOutputImageUrl(result.getUrl());
                return;
            }
        }
    }
}
