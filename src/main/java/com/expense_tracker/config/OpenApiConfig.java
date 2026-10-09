package com.expense_tracker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI expenseTrackerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Expense Tracker & AI Financial Assistant API")
                        .description("Production-grade multi-module Personal Expense Tracker powered by Spring Boot 3, Redis caching, and Google Gemini AI Tool Execution.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Engineering Architecture Team")
                                .email("architect@expense-tracker.internal"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT Bearer token obtained from /users/login to authenticate requests.")));
    }

    /**
     * Explicitly displays the 'Authorization' header parameter under the Parameters section
     * of every secured endpoint in Swagger UI so developers can supply the token directly per request
     * in addition to using the global 'Authorize' button.
     */
    @Bean
    public OperationCustomizer addGlobalHeaderParameters() {
        return (operation, handlerMethod) -> {
            String methodName = handlerMethod.getMethod().getName();
            // Do not require header on public auth endpoints
            if ("registerUser".equalsIgnoreCase(methodName) || "login".equalsIgnoreCase(methodName)) {
                return operation;
            }

            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("Authorization")
                    .description("JWT Bearer token (format: 'Bearer <token>'). Optional if you have already set it using the green Authorize button at the top.")
                    .required(false)
                    .schema(new StringSchema().example("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")));

            return operation;
        };
    }
}
