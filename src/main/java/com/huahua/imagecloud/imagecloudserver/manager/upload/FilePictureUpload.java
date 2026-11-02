package com.huahua.imagecloud.imagecloudserver.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import com.huahua.imagecloud.imagecloudserver.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 本地图片上传。
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    private static final List<String> ALLOW_FORMAT_LIST =
            Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        long fileSize = multipartFile.getSize();
        ThrowUtils.throwIf(fileSize > MAX_FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件不能超过10M");

        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        fileSuffix = fileSuffix == null ? "" : fileSuffix.toLowerCase(Locale.ROOT);
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix),
                ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
