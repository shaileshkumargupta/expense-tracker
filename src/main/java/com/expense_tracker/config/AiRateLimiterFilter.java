package com.expense_tracker.config;

import com.expense_tracker.dto.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class AiRateLimiterFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_PREFIX = "ratelimit:ai:";
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public AiRateLimiterFilter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/ai");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userIdentifier = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName()
                : request.getRemoteAddr();

        String redisKey = RATE_LIMIT_PREFIX + userIdentifier;

        try {
            Long requestCount = stringRedisTemplate.opsForValue().increment(redisKey);
            if (requestCount != null && requestCount == 1) {
                stringRedisTemplate.expire(redisKey, WINDOW_DURATION);
            }

            long remaining = Math.max(0, MAX_REQUESTS_PER_MINUTE - (requestCount != null ? requestCount : 0));
            response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

            if (requestCount != null && requestCount > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for user: {}. Count: {}/{}", userIdentifier, requestCount, MAX_REQUESTS_PER_MINUTE);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", "60");

                ErrorResponse errorResponse = ErrorResponse.builder()
                        .timeStamp(LocalDateTime.now())
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .error("Too Many Requests")
                        .message("Rate limit quota exceeded. Maximum " + MAX_REQUESTS_PER_MINUTE + " requests per minute allowed for AI Assistant.")
                        .path(request.getRequestURI())
                        .details(List.of("Free tier quota exceeded. Please wait 60 seconds before sending more AI requests."))
                        .build();

                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
        } catch (Exception e) {
            log.error("Redis rate limiter encountered error: {}. Allowing request through fail-safe policy.", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
