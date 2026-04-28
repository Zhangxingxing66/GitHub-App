package com.example.myapplication.data.local.dao;

import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.data.local.entity.RepoEntity;

/**
 * 仓库分页缓存表的访问接口。
 * Room 根据注解生成实现类，调用方不需要手写 SQLiteOpenHelper。
 */
@Dao
public interface RepoDao {
    // PagingSource 是 Paging 读取本地数据库的入口；Room 表变化后会自动让分页数据失效并刷新。
    @Query("SELECT * FROM repos ORDER BY stars DESC, id DESC")
    PagingSource<Integer, RepoEntity> pagingSource();

    // REPLACE 可以在刷新同一个仓库时覆盖旧缓存，避免主键冲突。
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(java.util.List<RepoEntity> repos);

    // 新搜索或 refresh 时清空旧查询结果，保证列表只展示当前关键字的数据。
    @Query("DELETE FROM repos")
    void clearAll();
}
