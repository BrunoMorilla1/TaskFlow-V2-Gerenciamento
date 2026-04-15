package br.com.taskflow.gerenciamento.seguranca.configuracao;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));

        registry.addResourceHandler("/sw.js")
                .addResourceLocations("classpath:/static/sw.js")
                .setCacheControl(CacheControl.noCache().cachePrivate());

        registry.addResourceHandler("/site.webmanifest", "/favicon.ico")
                .addResourceLocations("classpath:/static/site.webmanifest", "classpath:/static/favicon.ico")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS));
    }
}