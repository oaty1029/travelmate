package com.travelmate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j API 文档配置
 * 访问地址: /doc.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI travelmateOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TravelMate 旅伴 API")
                        .description("中国境内智能旅游推荐与行程规划系统")
                        .version("1.0.0")
                        .contact(new Contact().name("TravelMate")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
