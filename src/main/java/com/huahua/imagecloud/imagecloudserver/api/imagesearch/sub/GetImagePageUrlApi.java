package com.huahua.imagecloud.imagecloudserver.api.imagesearch.sub;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.cookie.GlobalCookieManager;
import cn.hutool.json.JSONUtil;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.net.CookieManager;
import java.net.URLDecoder;
import java.util.Map;

/**
 * 获取图片页面地址工具类 (Cookie 伪装版)
 */
@Slf4j
public class GetImagePageUrlApi {

    private static final String BAIDU_HOME_URL = "https://graph.baidu.com/pcpage/index?tpl=upload";
    private static final String UPLOAD_API_URL = "https://graph.baidu.com/upload";

    public static String getImagePageUrl(String imageUrl) {
// 注意这里变成了 CookieManager
        CookieManager cookieManager = GlobalCookieManager.getCookieManager();

        try {
            // 1. 第一步：假装访问首页，获取 Cookie (这是解决 Reject 的关键)
            log.info("正在访问百度识图首页以获取 Cookie...");
            HttpResponse homeResponse = HttpRequest.get(BAIDU_HOME_URL)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .execute();

            if (homeResponse.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROE, "无法访问百度首页");
            }
            log.info("✅ 首页访问成功，Cookie 已获取");

            // 2. 第二步：构造上传请求
            // 注意：这里去掉了 stoken，因为现在的接口主要靠 Cookie 校验
            String bodyData = "image=" + imageUrl +
                    "&tn=pc" +
                    "&from=pc" +
                    "&image_source=PC_UPLOAD_URL" +
                    "&type=url";

            String url = UPLOAD_API_URL + "?uptime=" + System.currentTimeMillis();

            log.info("正在发送图片搜索请求...");

            // 3. 第三步：发送上传请求 (GlobalCookieManager 会自动把刚才拿到的 Cookie 带上)
            HttpResponse uploadResponse = HttpRequest.post(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Referer", BAIDU_HOME_URL)
                    .header("Origin", "https://graph.baidu.com")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(bodyData)
                    .timeout(10000)
                    .execute();

            if (uploadResponse.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROE, "HTTP Error: " + uploadResponse.getStatus());
            }

            String responseBody = uploadResponse.body();
            log.info("🔍 接口原始响应: {}", responseBody);

            Map<String, Object> result = JSONUtil.toBean(responseBody, Map.class);

            // 检查状态码
            // 注意：有时候成功是 status=0，有时候可能是其他，我们要看具体的 msg
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                String msg = MapUtil.getStr(result, "msg", "Unknown Error");
                if ("Reject".equals(msg)) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROE, "被百度反爬拦截 (Reject)。建议：1. 检查网络 2. 稍后再试 (IP可能被限)");
                }
                throw new BusinessException(ErrorCode.OPERATION_ERROE, "接口错误: " + msg);
            }

            Map<String, Object> data = (Map<String, Object>) result.get("data");
            if (data == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROE, "接口返回数据为空");
            }

            String rawUrl = String.valueOf(data.get("url"));
            String searchResultUrl = URLDecoder.decode(rawUrl, "UTF-8");

            if (StrUtil.isBlank(searchResultUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROE, "未返回有效结果URL");
            }

            return searchResultUrl;

        } catch (Exception e) {
            log.error("❌ 搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROE, "失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String imageUrl = "https://imagehub-1409021502.cos.ap-guangzhou.myqcloud.com/public/2030488835208638465/2026-04-28_C6L3adayKMXIPCXk.thumbnail.png";
        try {
            String result = getImagePageUrl(imageUrl);
            System.out.println("🎉 搜图成功！结果地址: " + result);
        } catch (Exception e) {
            System.err.println("😭 报错详情: " + e.getMessage());
        }
    }
}