package com.example.myapplication.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.data.local.entity.SearchHistoryEntity;

import java.util.List;

/**
 * 搜索历史表的访问接口。
 * 这里用 keyword 做主键，同一个关键字重复搜索时会覆盖 searchedAt。
 */
@Dao
public interface SearchHistoryDao {
    // 只展示最近 10 条历史，避免首页横向列表过长。
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 10")
    LiveData<List<SearchHistoryEntity>> observeRecent();

    // REPLACE 可以把重复关键字的时间更新为最近搜索时间。
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SearchHistoryEntity entity);
}
