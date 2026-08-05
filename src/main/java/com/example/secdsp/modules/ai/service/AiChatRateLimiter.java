package com.example.secdsp.modules.ai.service;

import com.example.secdsp.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight per-user rate limit for AI chat (no external dependency).
 * Default: 20 requests / rolling 60 seconds.
 */
@Component
public class AiChatRateLimiter {

    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_MS = 60_000L;

    private final Map<Long, Deque<Long>> hits = new ConcurrentHashMap<>();

    public void check(Long userId) {
        if (userId == null) {
            throw new BusinessException("Authentication required.", HttpStatus.UNAUTHORIZED);
        }
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) {
                q.pollFirst();
            }
            if (q.size() >= MAX_REQUESTS) {
                throw new BusinessException(
                    "Quá nhiều yêu cầu AI. Vui lòng thử lại sau khoảng 1 phút.",
                    HttpStatus.TOO_MANY_REQUESTS
                );
            }
            q.addLast(now);
        }
    }
}
