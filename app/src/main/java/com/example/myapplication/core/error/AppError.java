package com.example.myapplication.core.error;

import androidx.annotation.NonNull;

/**
 * 应用内部统一错误模型。
 * ErrorMapper 把异常转换成 AppError 后，UI 可以只关心 userMessage。
 */
public class AppError {
    // 错误大类，方便以后按类型做不同 UI 或埋点统计。
    public enum Type {
        NETWORK,
        AUTH,
        RATE_LIMIT,
        SERVER,
        CLIENT,
        UNKNOWN
    }

    public final Type type;
    @NonNull
    // 展示给用户看的友好文案。
    public final String userMessage;
    @NonNull
    // 记录到日志里的调试信息，通常比用户文案更具体。
    public final String debugMessage;

    public AppError(
            @NonNull Type type,
            @NonNull String userMessage,
            @NonNull String debugMessage
    ) {
        this.type = type;
        this.userMessage = userMessage;
        this.debugMessage = debugMessage;
    }
}
