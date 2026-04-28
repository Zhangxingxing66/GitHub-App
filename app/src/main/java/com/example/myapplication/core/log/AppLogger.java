package com.example.myapplication.core.log;

import android.util.Log;

/**
 * 日志工具类。
 * 当前只是对 Android Log 做一层很薄的封装，后续如果接入文件日志或远程日志，只需要改这里。
 */
public final class AppLogger {
    private AppLogger() {
        // 工具类不允许实例化。
    }

    public static void d(String tag, String message) {
        Log.d(tag, message);
    }

    public static void i(String tag, String message) {
        Log.i(tag, message);
    }

    public static void w(String tag, String message) {
        Log.w(tag, message);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }
}
