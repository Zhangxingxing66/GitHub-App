package com.example.myapplication.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 搜索历史表。
 * keyword 作为主键，因此同一个关键字重复搜索会更新搜索时间而不是新增重复记录。
 */
@Entity(tableName = "search_history")
public class SearchHistoryEntity {
    @PrimaryKey
    @NonNull
    public String keyword;

    // 最近搜索时间，用于按时间倒序展示历史记录。
    public long searchedAt;

    public SearchHistoryEntity(@NonNull String keyword, long searchedAt) {
        this.keyword = keyword;
        this.searchedAt = searchedAt;
    }
}
