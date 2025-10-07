package com.mogakjak.mogakjak.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.local.server.url}")
    private String localServerUrl;

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Mogakjak API Document")
                .version("v1.0.0")
                .description("모각작 프로젝트의 API 명세서입니다.");

        Server localServer = new Server().url(localServerUrl).description("Local server");

//        String jwtSchemeName = "jwtAuth";
//        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
//
//        Components components = new Components()
//                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
//                        .name(jwtSchemeName)
//                        .type(SecurityScheme.Type.HTTP)
//                        .scheme("bearer")
//                        .bearerFormat("JWT"));

        return new OpenAPI()
//                .components(components)
//                .addSecurityItem(securityRequirement)
                .info(info)
                .servers(List.of(localServer));
    }
}