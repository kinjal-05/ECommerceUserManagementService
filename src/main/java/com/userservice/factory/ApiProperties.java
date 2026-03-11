package com.userservice.factory;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Binds all values from api-paths.yml into strongly-typed fields.
 *
 * @PropertySource loads the file from src/main/resources/api-paths.yml
 * factory = YamlPropertySourceFactory.class is required because Spring's
 * default @PropertySource only supports .properties files, not .yml.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api.auth")
@PropertySource(
        value = "classpath:api-paths.yml",
        factory = YamlPropertySourceFactory.class
)
public class ApiProperties {

    private String base;
    private String register;
    private String login;
    private String logout;
    private String me;
    private String users;
    private String usersSearch;
    private String usersById;
    private String usersRole;

    // Nested binding for api.token.*
    private Token token = new Token();

    @Getter
    @Setter
    public static class Token {
        private String refresh;
    }
}