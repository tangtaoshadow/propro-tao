package com.westlake.air.propro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;


/***
 * @UpdateAuthor TangTao
 * @CreateTime
 * @UpdateTime 2019-7-23 13:40:23
 * @Achieve 修改了跨域规则 允许跨域
 * @Copyright 西湖 PROPRO http://www.proteomics.pro/
 *
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 跨域脚本规则,仅允许来自Aliyun 指定OSS文件可以访问
     *
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
//				registry.addMapping("/api/**");
        registry.addMapping("/**")
                // .allowedOrigins("http://wias.oss-cn-shanghai.aliyuncs.com")
                .allowedOrigins("*")
                .allowedHeaders("*")
                .allowedMethods("GET", "POST", "DELETE", "PUT", "OPTIONS")
                .allowCredentials(false).maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        registry.addInterceptor(interceptor);
        // 拦截所有请求
        // registry.addInterceptor(authenticationInterceptor()).addPathPatterns("/**");
    }


    @Bean
    public AuthenticationInterceptor authenticationInterceptor() {
        return new AuthenticationInterceptor();
    }
}
