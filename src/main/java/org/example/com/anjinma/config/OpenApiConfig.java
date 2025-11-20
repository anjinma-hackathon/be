package org.example.com.anjinma.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
            .info(new Info()
                .title("Anjinma API")
                .description("Rooms, Attendance, and Translation WebSocket APIs")
                .version("v1")
                .contact(new Contact().name("Backend").email("dev@example.com"))
                .license(new License().name("Proprietary")))
            .externalDocs(new ExternalDocumentation()
                .description("WebSocket/STOMP Usage")
                .url("/room.md"));
    }
}

