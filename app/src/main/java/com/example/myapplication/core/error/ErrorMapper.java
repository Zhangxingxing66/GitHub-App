package com.example.myapplication.core.error;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把底层异常转换成用户可理解的错误信息。
 *
 * 好处：
 * - UI 不需要到处判断 UnknownHostException、HTTP 500 等细节。
 * - 所有页面的错误文案保持一致。
 * - 日志仍保留 debugMessage，方便定位真实原因。
 */
public final class ErrorMapper {
    // 从类似 "HTTP 403" 的异常信息中提取状态码。
    private static final Pattern HTTP_CODE_PATTERN = Pattern.compile("\\b([1-5][0-9]{2})\\b");

    private ErrorMapper() {
        // 工具类不允许实例化。
    }

    @NonNull
    public static AppError map(@NonNull Throwable throwable) {
        // DNS 失败、无网络、连接失败统一归为网络不可用。
        if (throwable instanceof UnknownHostException || throwable instanceof ConnectException) {
            return new AppError(
                    AppError.Type.NETWORK,
                    "网络不可用，请检查连接后重试。",
                    throwable.toString()
            );
        }

        // 超时通常说明网络慢或服务器响应慢，给用户重试建议。
        if (throwable instanceof SocketTimeoutException) {
            return new AppError(
                    AppError.Type.NETWORK,
                    "请求超时，请重试。",
                    throwable.toString()
            );
        }

        // Retrofit/Repository 抛出的 IOException 里可能带 HTTP 状态码。
        Integer statusCode = extractHttpCode(throwable.getMessage());
        if (statusCode != null) {
            return mapByStatusCode(statusCode, throwable);
        }

        String message = throwable.getMessage();
        String lowerMessage = message == null ? "" : message.toLowerCase(Locale.US);
        // GitHub 搜索接口容易触发限流，单独映射成更明确的提示。
        if (lowerMessage.contains("rate limit")) {
            return new AppError(
                    AppError.Type.RATE_LIMIT,
                    "请求过于频繁，请稍后重试。",
                    throwable.toString()
            );
        }

        // 其他 IOException 也大多与网络请求有关。
        if (throwable instanceof IOException) {
            return new AppError(
                    AppError.Type.NETWORK,
                    "网络请求失败，请下拉刷新后重试。",
                    throwable.toString()
            );
        }

        return new AppError(
                AppError.Type.UNKNOWN,
                "出现异常，请重试。",
                throwable.toString()
        );
    }

    @NonNull
    public static String userMessage(@NonNull Throwable throwable) {
        // UI 常用这个便捷方法直接拿用户文案。
        return map(throwable).userMessage;
    }

    @NonNull
    private static AppError mapByStatusCode(int statusCode, @NonNull Throwable throwable) {
        if (statusCode == 401 || statusCode == 403) {
            return new AppError(
                    AppError.Type.AUTH,
                    "请求被服务器拒绝，请稍后再试。",
                    "HTTP " + statusCode + " | " + throwable
            );
        }

        if (statusCode == 429) {
            return new AppError(
                    AppError.Type.RATE_LIMIT,
                    "请求过于频繁，请稍后重试。",
                    "HTTP 429 | " + throwable
            );
        }

        if (statusCode >= 500) {
            return new AppError(
                    AppError.Type.SERVER,
                    "服务器暂不可用，请稍后再试。",
                    "HTTP " + statusCode + " | " + throwable
            );
        }

        if (statusCode >= 400) {
            return new AppError(
                    AppError.Type.CLIENT,
                    "请求参数无效，请调整后重试。",
                    "HTTP " + statusCode + " | " + throwable
            );
        }

        return new AppError(
                AppError.Type.UNKNOWN,
                "请求失败，请重试。",
                "HTTP " + statusCode + " | " + throwable
        );
    }

    private static Integer extractHttpCode(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = HTTP_CODE_PATTERN.matcher(message);
        Integer statusCode = null;
        // 如果信息里出现多个三位码，取最后一个，通常更接近真正的 HTTP 状态。
        while (matcher.find()) {
            statusCode = Integer.parseInt(matcher.group(1));
        }
        return statusCode;
    }
}
