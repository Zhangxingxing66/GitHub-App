package com.example.myapplication.core.error;

import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * ErrorMapper 的 JVM 单元测试。
 * 这类测试不需要 Android 设备，运行速度快，适合验证纯 Java 逻辑。
 */
public class ErrorMapperTest {

    @Test
    public void map_unknownHost_returnsNetworkError() {
        // 无法解析域名通常代表断网或 DNS 问题，应归为 NETWORK。
        AppError error = ErrorMapper.map(new UnknownHostException("offline"));
        assertEquals(AppError.Type.NETWORK, error.type);
        assertTrue(error.userMessage.contains("网络"));
    }

    @Test
    public void map_timeout_returnsTimeoutMessage() {
        // 超时仍属于网络类错误，但用户文案应该提示“超时”。
        AppError error = ErrorMapper.map(new SocketTimeoutException("timeout"));
        assertEquals(AppError.Type.NETWORK, error.type);
        assertTrue(error.userMessage.contains("超时"));
    }

    @Test
    public void map_http429_returnsRateLimit() {
        // 429 是限流状态码，GitHub API 高频请求时常见。
        AppError error = ErrorMapper.map(new IOException("HTTP 429"));
        assertEquals(AppError.Type.RATE_LIMIT, error.type);
        assertTrue(error.userMessage.contains("频繁"));
    }

    @Test
    public void map_http500_returnsServerError() {
        // 5xx 代表服务端错误，和本地网络错误区分开。
        AppError error = ErrorMapper.map(new IOException("Request failed: 500"));
        assertEquals(AppError.Type.SERVER, error.type);
        assertTrue(error.userMessage.contains("服务器"));
    }

    @Test
    public void map_unknownRuntime_returnsUnknownError() {
        // 无法识别的异常走 UNKNOWN 兜底，避免 UI 没有提示。
        AppError error = ErrorMapper.map(new RuntimeException("boom"));
        assertEquals(AppError.Type.UNKNOWN, error.type);
        assertTrue(error.userMessage.contains("异常"));
    }

    @Test
    public void map_runtimeHttp403Forbidden_returnsAuthError() {
        // 即使异常类型不是 IOException，只要消息里有 HTTP 403，也应识别为权限/认证问题。
        AppError error = ErrorMapper.map(new RuntimeException("HTTP 403 Forbidden"));
        assertEquals(AppError.Type.AUTH, error.type);
        assertTrue(error.userMessage.contains("拒绝"));
    }

    @Test
    public void map_rateLimitKeyword_returnsRateLimitError() {
        // 有些限流错误不一定带 429，按关键字也能识别。
        AppError error = ErrorMapper.map(new RuntimeException("API rate limit exceeded"));
        assertEquals(AppError.Type.RATE_LIMIT, error.type);
        assertTrue(error.userMessage.contains("频繁"));
    }
}
