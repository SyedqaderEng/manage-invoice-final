package com.manageinvoice.authonly;

import com.manageinvoice.common.api.ApiHealthController;
import com.manageinvoice.common.security.RevokedToken;
import com.manageinvoice.common.security.RevokedTokenRepository;
import com.manageinvoice.common.security.SecurityConfig;
import com.manageinvoice.identity.AuthController;
import com.manageinvoice.identity.User;
import com.manageinvoice.identity.UserRepository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@Profile("auth-only")
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = { AuthController.class, SecurityConfig.class, ApiHealthController.class })
@EntityScan(basePackageClasses = { User.class, RevokedToken.class })
@EnableJpaRepositories(basePackageClasses = { UserRepository.class, RevokedTokenRepository.class })
public class AuthOnlyApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AuthOnlyApplication.class);
        app.setAdditionalProfiles("auth-only");
        app.run(args);
    }
}