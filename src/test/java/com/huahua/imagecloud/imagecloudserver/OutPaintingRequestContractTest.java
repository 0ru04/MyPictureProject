package com.huahua.imagecloud.imagecloudserver;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.AliYunAiApi;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model.CreateOutPaintingTaskRequest;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model.GetOutPaintingTaskResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OutPaintingRequestContractTest {

    @Test
    void serializesWanxImageEditExpandRequest() {
        CreateOutPaintingTaskRequest request = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setFunction("expand");
        input.setPrompt("扩展图片画面");
        input.setBaseImageUrl("https://example.com/a.jpg");
        request.setInput(input);

        CreateOutPaintingTaskRequest.Parameters parameters =
                new CreateOutPaintingTaskRequest.Parameters();
        parameters.setTopScale(1.2F);
        parameters.setBottomScale(1.3F);
        parameters.setLeftScale(1.4F);
        parameters.setRightScale(1.5F);
        parameters.setN(1);
        parameters.setWatermark(false);
        request.setParameters(parameters);

        JSONObject json = JSONUtil.parseObj(JSONUtil.toJsonStr(request));
        JSONObject inputJson = json.getJSONObject("input");
        JSONObject parametersJson = json.getJSONObject("parameters");

        assertEquals("wanx2.1-imageedit", json.getStr("model"));
        assertEquals("expand", inputJson.getStr("function"));
        assertEquals("https://example.com/a.jpg", inputJson.getStr("base_image_url"));
        assertFalse(inputJson.containsKey("image_url"));
        assertEquals(1.2F, parametersJson.getFloat("top_scale"));
        assertEquals(1.3F, parametersJson.getFloat("bottom_scale"));
        assertEquals(1.4F, parametersJson.getFloat("left_scale"));
        assertEquals(1.5F, parametersJson.getFloat("right_scale"));
        assertEquals(1, parametersJson.getInt("n"));
        assertFalse(parametersJson.containsKey("x_scale"));
        assertFalse(parametersJson.containsKey("output_ratio"));
    }

    @Test
    void parsesImageSynthesisTaskResult() {
        GetOutPaintingTaskResponse response = JSONUtil.toBean(
                "{\"request_id\":\"req-1\",\"output\":{\"task_id\":\"task-1\","
                        + "\"task_status\":\"SUCCEEDED\",\"results\":[{\"url\":\"https://example.com/result.png\"}],"
                        + "\"task_metrics\":{\"TOTAL\":1,\"SUCCEEDED\":1,\"FAILED\":0}},"
                        + "\"usage\":{\"image_count\":1}}",
                GetOutPaintingTaskResponse.class);

        assertEquals("task-1", response.getOutput().getTaskId());
        assertEquals("SUCCEEDED", response.getOutput().getTaskStatus());
        assertEquals("https://example.com/result.png",
                response.getOutput().getResults().get(0).getUrl());
        assertEquals(1, response.getOutput().getTaskMetrics().getTotal());
        assertEquals(1, response.getUsage().getImageCount());
    }

    @Test
    void usesOfficialImageSynthesisEndpoint() {
        assertEquals("https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/image-synthesis",
                AliYunAiApi.CREATE_OUT_PAINTING_TASK_URL);
    }
}
