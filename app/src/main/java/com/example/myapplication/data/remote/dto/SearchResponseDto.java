package com.example.myapplication.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * GitHub 搜索接口的顶层响应。
 * total_count 用来判断是否还有下一页，items 是当前页的仓库列表。
 */
public class SearchResponseDto {
    @SerializedName("total_count")
    public int totalCount;

    @SerializedName("items")
    public List<RepoDto> items;
}
