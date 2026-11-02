package com.huahua.imagecloud.imagecloudserver.manager.upload;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import com.huahua.imagecloud.imagecloudserver.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * URL 图片上传。
 */
@Service
@Slf4j
public class UrlPictureUpload extends PictureUploadTemplate {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    private static final List<String> ALLOW_CONTENT_TYPES =
            Arrays.asList("image/jpeg", "image/jpg", "image/png",
                    "image/webp", "image/gif", "image/bmp");

    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "图片地址不能为空");

        try {
            URL url = new URL(fileUrl);
            String protocol = url.getProtocol().toLowerCase(Locale.ROOT);
            ThrowUtils.throwIf(!"http".equals(protocol) && !"https".equals(protocol),
                    ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 图片地址");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片地址格式不正确");
        }
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        String filename = "";
        try {
            String path = new URL(fileUrl).getPath();
            filename = path.substring(path.lastIndexOf("/") + 1);
        } catch (Exception e) {
            log.debug("解析图片文件名失败: {}", fileUrl, e);
        }
        if (StrUtil.isBlank(filename)) {
            filename = "image";
        }
        if (StrUtil.contains(filename, ".")) {
            return filename;
        }

        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            String extension = contentTypetoExtension(
                    normalizeContentType(response.header("Content-Type")));
            if (extension != null) {
                return filename + "." + extension;
            }
        } catch (Exception ignored) {
            log.debug("无法从 Content-Type 推断图片后缀: {}", fileUrl);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return filename + ".webp";
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        try (HttpResponse response = HttpUtil.createRequest(Method.GET, fileUrl).execute()) {
            ThrowUtils.throwIf(!response.isOk(), ErrorCode.PARAMS_ERROR,
                    "图片地址无法访问，HTTP " + response.getStatus());

            String contentType = normalizeContentType(response.header("Content-Type"));
            if (StrUtil.isNotBlank(contentType)
                    && !ALLOW_CONTENT_TYPES.contains(contentType)
                    && !"application/octet-stream".equals(contentType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片地址不是有效图片");
            }
            ThrowUtils.throwIf(response.contentLength() > MAX_FILE_SIZE,
                    ErrorCode.PARAMS_ERROR, "图片大小不能超过10M");

            response.writeBody(file);
            ThrowUtils.throwIf(file.length() == 0, ErrorCode.PARAMS_ERROR, "图片内容为空");
            ThrowUtils.throwIf(file.length() > MAX_FILE_SIZE,
                    ErrorCode.PARAMS_ERROR, "图片大小不能超过10M");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("图片下载失败: {}", fileUrl, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "图片下载失败，请检查 URL 是否可访问");
        }
    }

    private String normalizeContentType(String contentType) {
        if (StrUtil.isBlank(contentType)) {
            return "";
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private String contentTypetoExtension(String contentType) {
        Map<String, String> map = new HashMap<>();
        map.put("image/jpeg", "jpg");
        map.put("image/jpg", "jpg");
        map.put("image/png", "png");
        map.put("image/webp", "webp");
        map.put("image/gif", "gif");
        map.put("image/bmp", "bmp");
        return map.get(contentType);
    }
}
