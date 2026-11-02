package com.huahua.imagecloud.imagecloudserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.huahua.imagecloud.imagecloudserver.mapper")
@EnableAspectJAutoProxy(proxyTargetClass = true) //提供当前代理对象访问
public class ImageCloudServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageCloudServerApplication.class, args);
    }

}
