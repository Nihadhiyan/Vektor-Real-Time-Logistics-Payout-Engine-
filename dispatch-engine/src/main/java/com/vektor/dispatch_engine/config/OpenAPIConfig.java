package com.vektor.dispatch_engine.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenAPIConfig {
    public OpenAPI vektorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Vektor Dispatch & Payout Engine API")
                        .description("REST API for triggering manual financial settlements and querying driver payout statuses.")
                        .version("v1.0.0")
                        .contact(new Contact()
                            .name("Vektor Engineer - NIHADH")
                            .url("https://github.com/Nihadhiyan/Vektor-Real-Time-Logistics-Payout-Engine-.git")
                        )
                        .license(new License().name("Apache 2.0").url("http://springdoc.org"))
                    );
    }
}
