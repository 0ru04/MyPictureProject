package com.huahua.imagecloud.imagecloudserver.manager;

import cn.hutool.core.io.FileUtil;
import com.huahua.imagecloud.imagecloudserver.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.demo.PicOperationDemo;
import com.qcloud.cos.demo.PutObjectDemo;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PutMapping;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.yaml.snakeyaml.tokens.Token.ID.Key;


/**
 * 通用对象存储操作--文件上传，文件下载等
 * */
@Component
@Data
public class CosManager {

    @Autowired
    private COSClient cosClient;

    @Autowired
    private CosClientConfig cosClientConfig;

    // 缩略图开关（读取 yml 配置，压测时关掉）
    @Value("${picture.thumbnail.enable:true}") // 默认 true，兼容生产
    private boolean enableThumbnail;
    /**
     * 一些操作 COS 的方法
     * 上传对象(图片，视频...)
     * key：文件在云上的存储路径/名称
     * file：本地要上传的文件对象
     *
     * */
    public  PutObjectResult putObject(String key, File file){


        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(),key,file);

        //cosClient.putObject() 执行上传动作，把刚才创建好的“请求对象”真正发送到腾讯云服务器。（快递员）
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     * */
    public COSObject getObject(String key){
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(),key);

        //那调用 COSClient 实例方法 getObject(getObjectRequest)去腾讯云那边调用出对应路径的图片，但还没有下载
        return cosClient.getObject(getObjectRequest);
    }


    /**
     * 在通用图片处理类中 --- 添加图片上传并解析图片的方法
     *
     * @param  key == uploadPath
     *
     *
     * 深入了解 PutObjectRequest 类：本质上是一个“上传配置清单”，
     * 它把原本散乱的几十个参数（存哪里、传什么、权限怎么设、要不要加密、要不要处理图片等）打包成一个对象，
     * 告诉 COS 服务器：“请按这些要求帮我存这个文件”。
     *
     *  PutObjectRequest 类内部有一个成员变量（字段），其类型是 PicOperations。
     * */

    public PutObjectResult putPictureObject(String key,File file){


        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(),key,file);

        //对图片进行处理，获取图片信息
        PicOperations picOperations = new PicOperations();
        //表示返回原图信息
        picOperations.setIsPicInfo(1);
        // 图片处理规则列表
        List<PicOperations.Rule> rules = new ArrayList<>();
        //1----图片压缩（转为webp格式）
        String webpKey= FileUtil.mainName(key)+".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);
        // 2----缩略图的处理，仅对 > 20KB 的图片生成缩略图
        if(enableThumbnail&& file.length()>20*1024){
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>",128,128));
            String thumbnailKey= FileUtil.mainName(key)+".thumbnail."+FileUtil.getSuffix(key);
            thumbnailRule.setFileId( thumbnailKey);
            rules.add(thumbnailRule);

        }


        //构造处理参数
        picOperations.setRules(rules);
        //上传图片的同时告知图片如何处理
        putObjectRequest.setPicOperations(picOperations);

        //cosClient.putObject() 是腾讯云 COS Java SDK 提供的最核心、最通用的文件上传方法。
        return cosClient.putObject(putObjectRequest);
    }


    /**
     * 临时测试用：无任何图片处理的纯上传（对照组用）
     * 测完可以删
     */
    public PutObjectResult putPictureObjectNoCompress(String key, File file){
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 不设 PicOperations，纯上传原图
        return cosClient.putObject(putObjectRequest);
    }










}
