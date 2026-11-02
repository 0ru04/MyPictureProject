package com.huahua.imagecloud.imagecloudserver.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.huahua.imagecloud.imagecloudserver.common.BaseResponse;
import com.huahua.imagecloud.imagecloudserver.common.ResultUtils;
import com.huahua.imagecloud.imagecloudserver.config.CosClientConfig;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import com.huahua.imagecloud.imagecloudserver.exception.ThrowUtils;
import com.huahua.imagecloud.imagecloudserver.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 已废弃，改用 upload包的模版方法
 * */
@Slf4j
@Service
@Deprecated
public class FileManager {

    @Autowired
    private CosManager cosManager;

    @Autowired
    private  CosClientConfig cosClientConfig;

    /**
     *
     * @param uploadPathPrefix 图片路径前包含用户ID
     *     service层传过来的    eg： public/888/...
     *
     * */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile,String uploadPathPrefix) {
        //1.效验图片（包括图片的大小，后缀是否符合要求）
        validPicture(multipartFile);

        //2.图片上传地址
        //调用工具类 RandomUtil 生成一个长度为 16 的随机字符串。（ 数字 + 大小写字母 组成的随机码）
        String uuid= RandomUtil.randomString(16);

        //获取文件的原始名字
        String originalFileName = multipartFile.getOriginalFilename();
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,FileUtil.getSuffix(originalFileName));

        //生成图片上传的最终地址
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);


        //3. 上传图片
        //声明一个 File 对象用于后续创建临时文件，初始化为 null，方便在 finally 块中判断是否创建成功以便删除
        // 是为了防止上传失败 才可以重新来
        File file = null;
        try {
            // 在本地磁盘创建临时文件
            file = File.createTempFile(uploadPath, null);
            //前端上传的文件流（内存中）复制/移动到刚才创建的本地临时文件中。
            multipartFile.transferTo(file);
            // 上传图片 接收返回的 总结果对象
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);

            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 将解析后的图片包装到专门的图片解析类 UploadPictureResult 中
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

            uploadPictureResult.setPicName(FileUtil.mainName(originalFileName));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);

            //imageInfo.getFormat() 图片文件格式（后缀）
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            return uploadPictureResult;
        }
            catch (Exception e) {
                log.error("file upload error " , e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");


            } finally {
               this.deleteTempFile(file);
            }
    }

    private void deleteTempFile(File file) {
        if(file==null){
            return;
        }
        //删除临时文件
        boolean deleteResult = file.delete();
        if(!deleteResult){
            log.error("file delete error, filepath = {}" + file.getAbsolutePath());

        }
    }

    /**
     * 效验文件
     * multipartFile 图片对象包装类
     * */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR,"文件不能为空");

        //1.效验文件大小
        long fileSize=multipartFile.getSize();
        final long ONE_M=1024*1024L;
        ThrowUtils.throwIf(fileSize>2*ONE_M,ErrorCode.PARAMS_ERROR,"文件不能超过2M");

        //2.效验文件后缀
        String fileSuffix= FileUtil.getSuffix(multipartFile.getOriginalFilename());

        //3.允许上传的文件后缀
        final List<String> ALLOW_FORMAT_LIST= Arrays.asList("jpg","jpeg","png","gif","bmp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix),ErrorCode.PARAMS_ERROR,"文件类型错误");

    }

    /**
     *
     * @param uploadPathPrefix 图片路径前包含用户ID
     *     service层传过来的    eg： public/888/...
     *
     * */

    public UploadPictureResult uploadPictureByUrl(String fileUrl,String uploadPathPrefix) {

        //1.效验参数
         validPicture(fileUrl);

        //2.图片上传地址
        //调用工具类 RandomUtil 生成一个长度为 16 的随机字符串。（ 数字 + 大小写字母 组成的随机码）
        String uuid= RandomUtil.randomString(16);

        //获取文件的原始名字

        String originalFileName = FileUtil.mainName(fileUrl);
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,FileUtil.getSuffix(originalFileName));

        //生成图片上传的最终地址
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);


        //3. 上传图片
        //声明一个 File 对象用于后续创建临时文件，初始化为 null，方便在 finally 块中判断是否创建成功以便删除
        // 是为了防止上传失败 才可以重新来是吧
        File file = null;
        try {
            // 在本地磁盘创建临时文件
            file = File.createTempFile(uploadPath, null);
            //前端上传的文件流（内存中）复制/移动到刚才创建的本地临时文件中。

                HttpUtil.downloadFile(fileUrl, file);
            // 上传图片 接收返回的 总结果对象
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);

            /**
             *
             * 对上传图片返回的总结果对象进行封装处理
             *
             * */


            // getCiUploadResult() 拿到一个包含所有智能分析数据的对象
            // getOriginalInfo() 获取 原始文件 的分析信息
            //  getImageInfo() 从原始文件信息中，专门提取 图片属性 部分（宽，高，格式，大小...）
            // ImageInfo类 封装图片元数据的普通 Java 类（POJO）
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 将解析后的图片包装到专门的图片解析类 UploadPictureResult 中
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();

            //NumberUtil.round(..., 2) 通常返回一个 BigDecimal 对象
            // 调用工具类 进行四舍五入，表示保留 2 位小数。
            // doubleValue() 将上面的 BigDecimal 结果强制转换回基础的 double 类型
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            uploadPictureResult.setPicName(FileUtil.mainName(originalFileName));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);

            //imageInfo.getFormat() 图片文件格式（后缀）
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            return uploadPictureResult;
        }
        catch (Exception e) {
            log.error("file upload error " , e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");


        } finally {
            this.deleteTempFile(file);
        }
    }

    /**
     * 效验 url 图片
     *
     * 该方法专门用于校验远程链接形式的图片，而不是本地上传的文件流。
     *
     * */
    private void validPicture(String fileUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR,"文件不能为空");

        //确保传入的字符串长得像一个合法的网址。
        try{
            //1.验证 URL 格式
            new URL(fileUrl);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件地址格式不正确");
        }

        //2.效验 URL 协议,不能上传自己服务器的东西 只能拿公网的图片去上传 有效避免服务器被当作跳板访问内部资源。
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://")|| fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR,"仅支持 HTTP 或 HTTPS 协议的文件地址");

        //3.发送head请求以验证文件是否存在
        HttpResponse response = null;

        try {
            // 发起 HEAD 请求（只获取头信息，不下载内容，效率高）
            /**
             * HttpUtil：Hutool 封装的 HTTP 工具类，简化请求操作；
             * createRequest(Method.HEAD, fileUrl)：创建「HEAD 类型」的请求对象，指定目标图片 URL；
             * execute()：真正发送请求到服务器，返回包含响应信息的 HttpResponse；
             * 整行代码核心：高效创建并发送 HEAD 请求，校验目标 URL 的有效性
             * */
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();

            // 未正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }

            // 4. 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp"); // 注：图片最后一行被截断，推测是 image/gif

                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }

            // 5. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024L; // 限制文件大小为 2MB

                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {

            //6.资源清理
            if (response != null) {
                response.close();
            }
        }


    }





}
