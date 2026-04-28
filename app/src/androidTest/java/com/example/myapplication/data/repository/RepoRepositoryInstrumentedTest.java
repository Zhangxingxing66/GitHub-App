package com.example.myapplication.data.repository;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.data.local.entity.FavoriteRepoEntity;
import com.example.myapplication.testing.LiveDataTestUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Repository 收藏逻辑的 instrumentation 测试。
 * 因为它会访问 Room 数据库，所以需要在 Android 环境中运行。
 */
@RunWith(AndroidJUnit4.class)
public class RepoRepositoryInstrumentedTest {

    @Test
    public void toggleFavorite_addThenRemove_updatesFavoriteState() throws Exception {
        // 使用目标应用 Context 创建真实 Repository 和 Room 数据库。
        RepoRepository repository = new RepoRepository(
                InstrumentationRegistry.getInstrumentation().getTargetContext());

        // 用当前时间生成临时 id，降低和已有收藏数据冲突的概率。
        long repoId = System.currentTimeMillis();
        FavoriteRepoEntity favorite = new FavoriteRepoEntity(
                repoId,
                "okhttp",
                "square/okhttp",
                "HTTP client",
                "Java",
                1,
                1,
                "square",
                "https://github.com/square/okhttp",
                System.currentTimeMillis()
        );

        // 第一次 toggle：未收藏 -> 插入收藏表。
        repository.toggleFavorite(favorite);
        Boolean isFavorite = LiveDataTestUtil.getOrAwaitValue(
                repository.observeIsFavorite(repoId),
                value -> value != null && value,
                5
        );
        assertTrue(isFavorite);

        // 第二次 toggle：已收藏 -> 从收藏表删除。
        repository.toggleFavorite(favorite);
        Boolean notFavorite = LiveDataTestUtil.getOrAwaitValue(
                repository.observeIsFavorite(repoId),
                value -> value != null && !value,
                5
        );
        assertFalse(notFavorite);
    }
}
