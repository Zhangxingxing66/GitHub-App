package com.example.myapplication.data.local.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.data.local.entity.RemoteKeysEntity;

import java.util.List;

/**
 * Paging 远程分页 key 表的访问接口。
 * remote_keys 用来记录每个仓库条目对应的上一页和下一页，帮助 RemoteMediator 续页。
 */
@Dao
public interface RemoteKeysDao {
    // 每次网络返回一页数据时，同时写入这一页所有 item 的 prevKey/nextKey。
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RemoteKeysEntity> remoteKeys);

    @Nullable
    // 根据列表中的某个 repoId 找到它的分页 key，用于计算 append/prepend 应请求哪一页。
    @Query("SELECT * FROM remote_keys WHERE repoId = :repoId")
    RemoteKeysEntity remoteKeysByRepoId(long repoId);

    // refresh 时和 repos 表一起清空，避免旧查询的 key 影响新查询。
    @Query("DELETE FROM remote_keys")
    void clearRemoteKeys();
}
