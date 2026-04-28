package com.example.myapplication.data.remote;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit/OkHttp 的统一创建入口。
 *
 * 使用单例的原因：
 * - OkHttpClient 内部有连接池，复用客户端可以减少重复建连成本。
 * - Retrofit 接口是线程安全的，整个应用共用一个 GithubApi 即可。
 */
public class ApiClient {
    private static final String BASE_URL = "https://api.github.com/";
    // volatile + 双重检查锁，保证多线程下只创建一个 GithubApi 实例。
    private static volatile GithubApi API;

    private ApiClient() {
        // 工具类不允许外部 new。
    }

    public static GithubApi githubApi() {
        if (API == null) {
            synchronized (ApiClient.class) {
                if (API == null) {
                    // BASIC 只打印请求/响应的基本信息，便于调试又不会输出完整 body。
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

                    // GitHub API 推荐携带 Accept 和 User-Agent，否则部分环境可能被拒绝。
                    Interceptor headersInterceptor = new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request request = chain.request()
                                    .newBuilder()
                                    .addHeader("Accept", "application/vnd.github+json")
                                    .addHeader("User-Agent", "GithubBrowserApp")
                                    .build();
                            return chain.proceed(request);
                        }
                    };

                    // 超时时间不能无限等待；失败后交给上层 ErrorMapper 展示友好错误。
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(headersInterceptor)
                            .addInterceptor(loggingInterceptor)
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(10, TimeUnit.SECONDS)
                            .writeTimeout(10, TimeUnit.SECONDS)
                            .build();

                    // GsonConverterFactory 把 JSON 自动反序列化成 dto 包里的 Java 对象。
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    API = retrofit.create(GithubApi.class);
                }
            }
        }
        return API;
    }
}
