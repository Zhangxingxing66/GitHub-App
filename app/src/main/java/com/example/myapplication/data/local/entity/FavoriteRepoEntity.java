package com.example.myapplication.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 收藏仓库表。
 * 和 RepoEntity 分开存储，可以避免刷新搜索列表时把用户收藏清掉。
 */
@Entity(tableName = "favorite_repos")
public class FavoriteRepoEntity {
    // 仍然使用 GitHub 仓库 id 作为主键，保证同一个仓库只能收藏一次。
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

    @NonNull
    public String htmlUrl;

    // 收藏时间用于列表排序，也可以在 UI 上扩展展示“收藏于”。
    public long savedAt;

    public FavoriteRepoEntity(
            long id,
            @NonNull String name,
            @NonNull String fullName,
            @Nullable String description,
            @Nullable String language,
            int stars,
            int forks,
            @NonNull String ownerLogin,
            @NonNull String htmlUrl,
            long savedAt
    ) {
        this.id = id;
        this.name = name;
        this.fullName = fullName;
        this.description = description;
        this.language = language;
        this.stars = stars;
        this.forks = forks;
        this.ownerLogin = ownerLogin;
        this.htmlUrl = htmlUrl;
        this.savedAt = savedAt;
    }
}
