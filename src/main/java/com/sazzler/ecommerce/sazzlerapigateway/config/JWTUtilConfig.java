package com.sazzler.ecommerce.sazzlerapigateway.config;

import com.sazzler.ecommerce.util.JWTutil.JWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JWTUtilConfig {
    @Value("${jwt.secret}" )
    private String secretkry;

    @Value("${jwt.expiration}")
    private Long jwtExpirationInMs;

    @Bean
    public JWTUtil jwtUtil() {
        return new JWTUtil(secretkry, jwtExpirationInMs);
    }

}
