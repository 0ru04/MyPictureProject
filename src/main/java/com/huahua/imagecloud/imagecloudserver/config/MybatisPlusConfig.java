package com.huahua.imagecloud.imagecloudserver.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置分页功能的标准代码
 * */
@Configuration  //告诉spring 这是一个配置类
public class MybatisPlusConfig {
    /**
     * 拦截器的配置
     * */

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        //分页插件
        //new PaginationInnerInterceptor(DbType.MYSQL)
        // 告诉这个分页插件，在接下来生成的SQL语句中，必须符合 MySQL 的语法方言”。
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
