package com.example.myapplication.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 仓库列表缓存表。
 *
 * Entity 对应 SQLite 的一张表，字段对应表列。
 * 这个表保存从 GitHub 搜索接口返回的仓库，用于 Paging 本地分页展示。
 */
@Entity(tableName = "repos")
public class RepoEntity {
    // GitHub 仓库 id 全局唯一，适合作为主键。
    @PrimaryKey
    public long id;

    @NonNull
    public String name;

    @NonNull
    public String fullName;

    @Nullable
    public String description;

    @Nullable
    public String language;

    public int stars;
    public int forks;

    @NonNull
    public String ownerLogin;

    @Nullable
    public String ownerAvatarUrl;

    @NonNull
    public String htmlUrl;

    // 当前列表没有直接用这个字段判断收藏，收藏状态独立存在 favorite_repos 表中。
    public boolean isFavorite;
    // 缓存写入时间，便于以后扩展缓存过期策略。
    public long cachedAt;

    public RepoEntity(
            long id,
            @NonNull String name,
            @NonNull String fullName,
            @Nullable String description,
            @Nullable String language,
            int stars,
            int forks,
            @NonNull String ownerLogin,
            @Nullable String ownerAvatarUrl,
            @NonNull String htmlUrl,
            boolean isFavorite,
            long cachedAt
    ) {
        this.id = id;
        this.name = name;
        this.fullName = fullName;
        this.description = description;
        this.language = language;
        this.stars = stars;
        this.forks = forks;
        this.ownerLogin = ownerLogin;
        this.ownerAvatarUrl = ownerAvatarUrl;
        this.htmlUrl = htmlUrl;
        this.isFavorite = isFavorite;
        this.cachedAt = cachedAt;
    }
}
