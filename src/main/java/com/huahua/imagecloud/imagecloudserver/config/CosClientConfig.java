package com.huahua.imagecloud.imagecloudserver.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 负责读取配置文件，并创建一个 COS 客户端的 Bean
 * */
@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {

    /**
     * 域名
     */
    private String host;

    /**
     * secretId
     */
    private String secretId;

    /**
     * 密钥（注意不要泄露）
     */
    private String secretKey;

    /**
     * 区域
     */
    private String region;

    /**
     * 桶名
     */
    private  String bucket;

    @Bean
    public COSClient cosClient() {

        // 初始化用户身份信息(secretId, secretKey)
        // COSCredentials 就是 SDK 用来“索取账号密码”的接口
        // BasicCOSCredentials是COSCredentials具体实现类
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);

        //ClientConfig 就是腾讯云 COS SDK 专门设计用来封装和持有所有客户端配置信息的类。
        //在配置对象中锁定“目标地域”
        ClientConfig clientConfig = new ClientConfig(new Region(region));

        // 生成cos客户端（凭证，配置）

        //创建 COSClient 对象的唯一目的，就是作为你代码与腾讯云 COS 服务器之间的“全能操作员”
        return new COSClient(cred, clientConfig);
    }
}