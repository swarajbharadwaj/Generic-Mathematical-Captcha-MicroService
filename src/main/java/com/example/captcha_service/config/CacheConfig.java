package com.example.captcha_service.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Value("${captcha.ttl.seconds}")
    private int captchaTtl;

    public static final String CAPTCHA_CACHE = "captchaCache";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CAPTCHA_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(captchaTtl, TimeUnit.SECONDS)
                .maximumSize(10000)); // Max 10,000 CAPTCHAs in cache
        return cacheManager;
    }
}
