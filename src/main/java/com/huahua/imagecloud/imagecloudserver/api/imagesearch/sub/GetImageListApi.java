package com.huahua.imagecloud.imagecloudserver.api.imagesearch.sub;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.huahua.imagecloud.imagecloudserver.api.imagesearch.model.ImageSearchResult;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import java.util.List;




/**
 * 3） 获取图片列表
 * 通过调用百度接口返回的 JSON 数据，提取出其中的图片列表并返回。
 */
@Slf4j
public class GetImageListApi {

    /**
     * 获取图片列表
     *
     * @param url 接口地址
     * @return 图片搜索结果列表
     */
    public static List<ImageSearchResult> getImageList(String url) {
        try {
            // 发起GET请求
            HttpResponse response = HttpUtil.createGet(url).execute();

            // 获取响应内容
            int statusCode = response.getStatus();
            String body = response.body();

            // 处理响应
            if (statusCode == 200) {
                // 解析 JSON 数据并处理
                return processResponse(body);
            } else {
                throw new BusinessException(ErrorCode.OPERATION_ERROE, "接口调用失败");
            }
        } catch (Exception e) {
            log.error("获取图片列表失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROE, "获取图片列表失败");
        }
    }

    /**
     * 处理接口响应内容
     *
     * @param responseBody 接口返回的JSON字符串
     * @return 图片列表
     */
    private static List<ImageSearchResult> processResponse(String responseBody) {
        // 解析响应对象
        JSONObject jsonObject = new JSONObject(responseBody);

        if (!jsonObject.containsKey("data")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROE, "未获取到图片列表");
        }

        JSONObject data = jsonObject.getJSONObject("data");

        if (!data.containsKey("list")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROE, "未获取到图片列表");
        }

        JSONArray list = data.getJSONArray("list");
        // 将 JSON 数组转换为 Java 对象列表
        return JSONUtil.toList(list, ImageSearchResult.class);
    }

    // 测试 main 方法
    public static void main(String[] args) {
        // 这里的 URL 是示例，实际运行需要替换为有效的接口地址
        String url = "https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D...";
        List<ImageSearchResult> imageList = getImageList(url);
        System.out.println("搜索成功" + imageList);
    }
}