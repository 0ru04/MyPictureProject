package com.huahua.imagecloud.imagecloudserver.manager.upload;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.huahua.imagecloud.imagecloudserver.config.CosClientConfig;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import com.huahua.imagecloud.imagecloudserver.manager.CosManager;
import com.huahua.imagecloud.imagecloudserver.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;


@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    protected CosManager cosManager;

    @Resource
    protected CosClientConfig cosClientConfig;

    // 缩略图开关（读取 yml 配置，压测时关掉）
    @Value("${picture.thumbnail.enable:true}") // 默认 true，兼容生产
    private boolean enableThumbnail;

    /**
     * 模板方法，定义上传流程
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片
        validPicture(inputSource);

        // 2. 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        // 3. 创建临时文件
        // 声明一个 File 对象用于后续创建临时文件，初始化为 null，方便在 finally 块中判断是否创建成功以便删除,是为了防止上传失败 才可以重新来
        File file = null;
        try {

            file = File.createTempFile(uploadPath, null);
            // 处理文件来源（本地或 URL）
            processFile(inputSource, file);
            long cosStart=System.currentTimeMillis();
            // 4. 上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);

            // 原代码第 58 行下方加：
            long cosCost = System.currentTimeMillis() - cosStart;
            boolean hasThumbnail = file.length() > 20 * 1024;
            log.info("【压测埋点】素材大小={}KB, COS处理耗时={}ms, 缩略图开关={}, 触发缩略图分支={}",
                    file.length() / 1024, cosCost, enableThumbnail, hasThumbnail);

            //  getCiUploadResult() 拿到一个包含所有智能分析数据的对象
            //  getOriginalInfo() 获取 原始文件 的分析信息
            //  getImageInfo() 从原始文件信息中，专门提取 图片属性 部分（宽，高，格式，大小...）
            //  ImageInfo类 封装图片元数据的普通 Java 类（POJO）

            // 5.获取图片信息对象，封装返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //获取到图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();

            if(CollectionUtil.isNotEmpty(objectList)){
                //获取压缩之后的文件信息
                CIObject compressedCiObject = objectList.get(0);
                //缩略图默认等于压缩图
                CIObject thumbnailCiObject = compressedCiObject;
                // 有缩略图才拿缩略图 不然get（1）就会越界
                if(objectList.size()>1){
                    thumbnailCiObject = objectList.get(1);
                }
                // 封装压缩图的返回结果
                return buildResult(originFilename,compressedCiObject ,thumbnailCiObject,imageInfo);
            }

            return buildResult(originFilename, file, uploadPath, imageInfo);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6. 清理临时文件
            deleteTempFile(file);
        }
    }


    /**
     * 【新加：压测用重载】三个参数，skipCompress=true 时跳过所有压缩/缩略图
     * */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix, boolean skipCompress) {
        // 原来的 validPicture 校验不变
        validPicture(inputSource);

        // 原来的路径生成逻辑不变
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
            processFile(inputSource, file);

            long cosStart=System.currentTimeMillis();

            // 👇 核心改动：根据 skipCompress 选 CosManager 方法
            PutObjectResult putObjectResult;
            if (skipCompress) {
                // 无压缩：调你之前加的 putPictureObjectNoCompress
                putObjectResult = cosManager.putPictureObjectNoCompress(uploadPath, file);
            } else {
                // 有压缩（生产逻辑）
                putObjectResult = cosManager.putPictureObject(uploadPath, file);
            }


            long cosCost = System.currentTimeMillis() - cosStart;
            boolean hasThumbnail = file.length() > 20 * 1024;
            log.info("【压测埋点】素材大小={}KB, COS处理耗时={}ms",
                    skipCompress?"对照组(无压缩)":"实验组(有压缩)",
                    file.length() / 1024, cosCost);

            // 👇 核心改动：加空指针判断（无压缩时 CI 结果是 null）
            if (skipCompress) {
                // 无压缩：CI 处理结果为空，直接用原图信息构建返回
                ImageInfo imageInfo = putObjectResult.getCiUploadResult() == null
                        ? new ImageInfo()
                        : putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
                if (imageInfo.getWidth() == 0) {

                    java.awt.image.BufferedImage img = ImgUtil.read(file);
                    imageInfo.setWidth(img.getWidth());
                    imageInfo.setHeight(img.getHeight());
                    imageInfo.setFormat(FileUtil.getSuffix(originFilename));
                }
                return buildResult(originFilename, file, uploadPath, imageInfo);
            }

            // 👇 下面是原来的压缩逻辑，完全不动
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();

            if (CollectionUtil.isNotEmpty(objectList)) {
                CIObject compressedCiObject = objectList.get(0);
                CIObject thumbnailCiObject = compressedCiObject;
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
            return buildResult(originFilename, compressedCiObject, thumbnailCiObject, imageInfo);
        }
            return buildResult(originFilename, file, uploadPath, imageInfo);
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTempFile(file);
        }
    }

    /**
     * 封装返回结果
     * @param originFilename 原始文件名
     * @param  compressedCiObject 压缩后的图片
     * @param  thumbnailCiObject 缩略图
      * */
    private UploadPictureResult buildResult(String originFilename, CIObject compressedCiObject,CIObject thumbnailCiObject,ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();

        // NumberUtil.round(..., 2) 通常返回一个 BigDecimal 对象
        //  调用工具类 进行四舍五入，表示保留 2 位小数。
        //  doubleValue() 将上面的 BigDecimal 结果强制转换回基础的 double 类型
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }

    /**
     * 校验输入源（本地文件或 URL）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 封装返回结果
     */
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();

        // NumberUtil.round(..., 2) 通常返回一个 BigDecimal 对象
        //  调用工具类 进行四舍五入，表示保留 2 位小数。
        //  doubleValue() 将上面的 BigDecimal 结果强制转换回基础的 double 类型
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }

        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
