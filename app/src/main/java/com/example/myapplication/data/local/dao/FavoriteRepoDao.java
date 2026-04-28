package com.example.myapplication.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.data.local.entity.FavoriteRepoEntity;

import java.util.List;

/**
 * 收藏仓库表的访问接口。
 * 收藏是纯本地数据，所以不需要 RemoteMediator。
 */
@Dao
public interface FavoriteRepoDao {
    // 按保存时间倒序展示，让最近收藏的仓库排在前面。
    @Query("SELECT * FROM favorite_repos ORDER BY savedAt DESC")
    LiveData<List<FavoriteRepoEntity>> observeAll();

    // 详情页观察这个 LiveData，用来实时更新“收藏/取消收藏”按钮文案。
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_repos WHERE id = :repoId)")
    LiveData<Boolean> observeIsFavorite(long repoId);

    // Repository 的 toggle 操作在后台线程同步查询当前是否已收藏。
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_repos WHERE id = :repoId)")
    int isFavoriteSync(long repoId);

    // REPLACE 让重复收藏同一个仓库时可以刷新 savedAt 等字段。
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteRepoEntity entity);

    // 取消收藏只需要按 GitHub 仓库 id 删除。
    @Query("DELETE FROM favorite_repos WHERE id = :repoId")
    void deleteById(long repoId);
}
