package com.example.myapplication.ui.detail;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 详情页的一次完整 UI 状态。
 *
 * 使用不可变字段的好处：
 * - Activity 每次拿到状态都可以直接整体渲染。
 * - 状态对象创建后不会被别处偷偷修改，调试更容易。
 */
public class RepoDetailViewState {
    // 是否正在加载详情接口；即使 loading 为 true，也可以同时展示 fallback 内容。
    public final boolean loading;
    @NonNull
    public final String fullName;
    @Nullable
    public final String description;
    @Nullable
    public final String language;
    @NonNull
    public final String ownerLogin;
    public final int stars;
    public final int forks;
    @NonNull
    public final String htmlUrl;
    @Nullable
    public final String errorMessage;

    public RepoDetailViewState(
            boolean loading,
            @NonNull String fullName,
            @Nullable String description,
            @Nullable String language,
            @NonNull String ownerLogin,
            int stars,
            int forks,
            @NonNull String htmlUrl,
            @Nullable String errorMessage
    ) {
        this.loading = loading;
        this.fullName = fullName;
        this.description = description;
        this.language = language;
        this.ownerLogin = ownerLogin;
        this.stars = stars;
        this.forks = forks;
        this.htmlUrl = htmlUrl;
        this.errorMessage = errorMessage;
    }
}
