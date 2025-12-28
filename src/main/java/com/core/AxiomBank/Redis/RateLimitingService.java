package com.core.AxiomBank.Redis;

import com.core.AxiomBank.Exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;

    // Constants for limits
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int MAX_TRANSACTION_ATTEMPTS = 10;
    private static final long BLOCK_TIME_MINUTES = 5;

    public void checkLoginAttempts(String email) {
        String key = "login_attempts:" + email;
        checkLimit(key, MAX_LOGIN_ATTEMPTS, "Too many login attempts. Try again later.");
    }

    public void checkTransactionFrequency(Long clientId) {
        String key = "tx_frequency:" + clientId;
        checkLimit(key, MAX_TRANSACTION_ATTEMPTS, "You are making transactions too fast.");
    }

    private void checkLimit(String key, int maxLimit, String errorMessage) {
        String currentCountStr = redisTemplate.opsForValue().get(key);
        int currentCount = 0;

        if (currentCountStr != null) {
            currentCount = Integer.parseInt(currentCountStr);
        }

        if (currentCount >= maxLimit) {
            log.warn("Brute force/Spam detected for key: {}", key);
            throw new BadRequestException(errorMessage);
        }

        // Increment counter
        redisTemplate.opsForValue().increment(key);
        // Set expiration if it's a new key (expire after 15 minutes)
        if (currentCount == 0) {
            redisTemplate.expire(key, BLOCK_TIME_MINUTES, TimeUnit.MINUTES);
        }
    }
}
