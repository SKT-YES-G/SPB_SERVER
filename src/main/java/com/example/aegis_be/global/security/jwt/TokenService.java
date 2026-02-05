package com.example.aegis_be.global.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String REFRESH_TOKEN_PREFIX = "RT:";

    public void saveRefreshToken(String name, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + name;
        long validityInSeconds = jwtTokenProvider.getRefreshTokenValidity() / 1000;
        redisTemplate.opsForValue().set(key, refreshToken, validityInSeconds, TimeUnit.SECONDS);
        log.debug("Saved refresh token for: {}", name);
    }

    public String getRefreshToken(String name) {
        String key = REFRESH_TOKEN_PREFIX + name;
        return redisTemplate.opsForValue().get(key);
    }

    public boolean validateRefreshToken(String name, String refreshToken) {
        String storedToken = getRefreshToken(name);
        return refreshToken.equals(storedToken);
    }

    public void deleteRefreshToken(String name) {
        String key = REFRESH_TOKEN_PREFIX + name;
        redisTemplate.delete(key);
        log.debug("Deleted refresh token for: {}", name);
    }

    public boolean isLoggedIn(String name) {
        String key = REFRESH_TOKEN_PREFIX + name;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
