package com.westlake.air.propro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-07 23:55
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
//				registry.addMapping("/api/**");
                registry.addMapping("/**")
                        .allowedOrigins("http://wias.oss-cn-shanghai.aliyuncs.com")
                        .allowedHeaders("*")
                        .allowedMethods("GET", "POST", "DELETE", "PUT", "OPTIONS")
                        .allowCredentials(false).maxAge(3600);
            }
        };
    }

}
