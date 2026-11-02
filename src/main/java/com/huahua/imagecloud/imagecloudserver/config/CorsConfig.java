package com.huahua.imagecloud.imagecloudserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig  implements WebMvcConfigurer {
    // 1. 实现 WebMvcConfigurer 接口
    // 这是 Spring MVC 提供的扩展点，允许你自定义 MVC 配置（如拦截器、跨域、资源映射等）

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 2. 注册跨域规则
        // registry 是用于配置跨域策略的注册表

        registry.addMapping("/**")
                // 👉 关键点：匹配所有路径 (/**)
                // 意味着项目里的每一个接口 (/user/login, /image/delete 等) 都应用以下规则

                .allowCredentials(true)
                // 👉 允许携带凭证 (Cookies, Authorization Header)
                // 如果前端需要发送 Cookie 或 Token，这个必须设为 true。
                // ⚠️ 注意：如果这里设为 true，allowedOriginPatterns 通常不能设为 "*" (但在 Spring 5.3+ 使用 patterns 可以)

                .allowedOriginPatterns("*")
                // 👉 允许的源 (域名/端口)
                // "*" 表示允许**任何**域名访问。
                // 在生产环境中，为了安全，通常会改成具体的域名，如 "https://www.mywebsite.com"

                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                // 👉 允许的 HTTP 请求方法
                // 明确告诉浏览器：我只接受这几种操作。
                // OPTIONS 是浏览器发送的“预检请求”，必须包含在内。

                .allowedHeaders("*")
                // 👉 允许的请求头
                // "*" 表示前端可以发送任意自定义 Header (如 Content-Type, Authorization 等)

                .exposedHeaders("*");
        // 👉 暴露给前端的响应头
        // 允许前端 JS 代码读取服务器返回的任意 Header 信息。
    }


}
