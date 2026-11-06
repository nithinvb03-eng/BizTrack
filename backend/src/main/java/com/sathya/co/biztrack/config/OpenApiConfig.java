package com.sathya.co.biztrack.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bizTrackOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BizTrack API")
                        .version("1.0")
                        .description("Daily expense and profit analysis for Sathya Traders. "
                                + "Log in with POST /api/auth/login first; the session cookie is used afterwards."))
                .components(new Components().addSecuritySchemes("sessionCookie",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")))
                .addSecurityItem(new SecurityRequirement().addList("sessionCookie"));
    }
}