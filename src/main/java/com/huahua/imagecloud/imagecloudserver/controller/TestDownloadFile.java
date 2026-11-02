package com.huahua.imagecloud.imagecloudserver.controller;


import com.huahua.imagecloud.imagecloudserver.Constant.Constant;
import com.huahua.imagecloud.imagecloudserver.annotation.AuthCheck;
import com.huahua.imagecloud.imagecloudserver.common.BaseResponse;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import com.huahua.imagecloud.imagecloudserver.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestDownloadFile {

    @Autowired
    private CosManager cosManager;


    @AuthCheck(mustRole= Constant.ADMIN_ROLE)
    @GetMapping("/download")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException{

        //COSObjectInputStream 是腾讯云 SDK 特有的输入流类
            COSObjectInputStream cosObjectInput = null;

        // try 包裹所有可能出错的代码（如网络中断、文件不存在、IO错误）
            try {

                //调用封装好的管理器 cosManager，根据路径 filepath 去腾讯云拉取文件对象,还没真正开始下载
                COSObject cosObject = cosManager.getObject(filepath);

                //从上一步获取的对象中，拿到真正的数据输入流。
                cosObjectInput = cosObject.getObjectContent();

                // 输入流中的所有数据一次性读取出来，存到一个字节数组 byte[] 中。
                byte[] bytes = IOUtils.toByteArray(cosObjectInput);
                // 设置响应头
                response.setContentType("application/octet-stream;charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
                // 写入响应
                response.getOutputStream().write(bytes);

                //强制将缓冲区中剩余的数据立即发送出去。
                //作用：确保用户能接收到完整的数据，防止数据滞留在服务器缓冲区。
                response.getOutputStream().flush();
            } catch (Exception e) {
                log.error("file download error, filepath = " + filepath, e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
            } finally {
                if (cosObjectInput != null) {
                    cosObjectInput.close();
                }
            }
        }



}
