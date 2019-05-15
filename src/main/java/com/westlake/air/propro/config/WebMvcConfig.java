package com.westlake.air.propro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 跨域脚本规则,仅允许来自Aliyun 指定OSS文件可以访问
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
//				registry.addMapping("/api/**");
        registry.addMapping("/**")
                .allowedOrigins("http://wias.oss-cn-shanghai.aliyuncs.com")
                .allowedHeaders("*")
                .allowedMethods("GET", "POST", "DELETE", "PUT", "OPTIONS")
                .allowCredentials(false).maxAge(3600);
    }
}
