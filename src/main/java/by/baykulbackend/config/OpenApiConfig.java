package by.baykulbackend.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
    description = "JWT Authentication"
)
public class OpenApiConfig {
    
    @Bean
    public OpenAPI marketplaceOpenAPI(Environment env) {
        return new OpenAPI()
                .info(new Info()
                        .title(env.getProperty("openapi.title"))
                        .description(env.getProperty("openapi.description"))
                        .version(env.getProperty("openapi.version"))
                        .contact(new Contact()
                                .name(env.getProperty("openapi.contact.name"))
                                .email(env.getProperty("openapi.contact.email"))
                                .url(env.getProperty("openapi.contact.url")))
                        .license(new License()
                                .name(env.getProperty("openapi.license.name"))
                                .url(env.getProperty("openapi.license.url"))
                        )
                        .termsOfService(env.getProperty("openapi.terms-of-service")))
                .servers(List.of(
                        new Server()
                                .url(env.getProperty("openapi.servers.dev.url"))
                                .description(env.getProperty("openapi.servers.dev.description")),
                        new Server()
                                .url(env.getProperty("openapi.servers.production.url"))
                                .description(env.getProperty("openapi.servers.production.description"))
                ))
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Access token JWT")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/v1/auth/**", "/api/v1/public/**")
                .build();
    }
    
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("users")
                .pathsToMatch("/api/v1/users/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .addSecurityItem(new SecurityRequirement().addList("bearerAuth")))
                .build();
    }
    
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/v1/admin/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .addSecurityItem(new SecurityRequirement().addList("bearerAuth")))
                .build();
    }
    
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/api/**")
                .build();
    }
}