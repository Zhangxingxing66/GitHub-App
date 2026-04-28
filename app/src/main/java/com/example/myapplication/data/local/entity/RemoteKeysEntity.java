package com.example.myapplication.data.local.entity;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Paging RemoteMediator 使用的分页 key 表。
 * 每个仓库条目记录它所在页的上一页和下一页，便于继续加载。
 */
@Entity(tableName = "remote_keys")
public class RemoteKeysEntity {
    // 和 repos 表的 id 对应；这里没有声明外键，代码用事务保证同步写入/清理。
    @PrimaryKey
    public long repoId;

    // null 表示已经没有上一页。
    @Nullable
    public Integer prevKey;

    // null 表示已经没有下一页。
    @Nullable
    public Integer nextKey;

    public RemoteKeysEntity(long repoId, @Nullable Integer prevKey, @Nullable Integer nextKey) {
        this.repoId = repoId;
        this.prevKey = prevKey;
        this.nextKey = nextKey;
    }
}
