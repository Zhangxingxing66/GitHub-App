package com.example.myapplication.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * GitHub 仓库 JSON 对应的数据传输对象。
 * DTO 只描述网络返回格式，不直接参与 UI 展示或数据库缓存。
 */
public class RepoDto {
    // @SerializedName 解决 JSON 字段名和 Java 驼峰命名不一致的问题。
    @SerializedName("id")
    public long id;

    @SerializedName("name")
    public String name;

    @SerializedName("full_name")
    public String fullName;

    @SerializedName("description")
    public String description;

    @SerializedName("language")
    public String language;

    @SerializedName("stargazers_count")
    public int stargazersCount;

    @SerializedName("forks_count")
    public int forksCount;

    @SerializedName("html_url")
    public String htmlUrl;

    @SerializedName("owner")
    public OwnerDto owner;
}
