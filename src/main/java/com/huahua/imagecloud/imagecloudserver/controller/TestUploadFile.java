package com.huahua.imagecloud.imagecloudserver.controller;

import ch.qos.logback.core.Context;
import com.huahua.imagecloud.imagecloudserver.Constant.Constant;
import com.huahua.imagecloud.imagecloudserver.annotation.AuthCheck;
import com.huahua.imagecloud.imagecloudserver.common.BaseResponse;
import com.huahua.imagecloud.imagecloudserver.common.ResultUtils;
import com.huahua.imagecloud.imagecloudserver.config.CosClientConfig;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import com.huahua.imagecloud.imagecloudserver.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestUploadFile{


    @Autowired
    private   CosManager cosManager;


    @AuthCheck(mustRole= Constant.ADMIN_ROLE)
    @PostMapping("/upload")

    // @RequestPart 它在处理大文件或混合内容类型时更稳健。
    // MultipartFile 是spring提供的上传文件的接口 可以获取上传文件的文件名等信息
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {

        // 获取用户上传文件的原始文件名
        String fileName = multipartFile.getOriginalFilename();
        //拼接最终存储在 COS 上的路径。
        String filepath = String.format("/test/%s", fileName);

        //声明一个 File 对象用于后续创建临时文件，初始化为 null，方便在 finally 块中判断是否创建成功以便删除
        File file = null;
        try {

            // 上传文件
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);

            cosManager.putObject(filepath, file);
            // 返回可访问地址
            return ResultUtils.success(filepath);


        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");


        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }

    }

}
