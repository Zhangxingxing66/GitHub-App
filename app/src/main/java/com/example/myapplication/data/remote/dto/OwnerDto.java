package com.example.myapplication.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * GitHub owner 字段对应的 DTO。
 * 这里只保留当前项目真正用到的登录名和头像地址。
 */
public class OwnerDto {
    @SerializedName("login")
    public String login;

    @SerializedName("avatar_url")
    public String avatarUrl;
}
