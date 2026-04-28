package com.example.myapplication.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import androidx.paging.PagingSource;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.dao.FavoriteRepoDao;
import com.example.myapplication.data.local.dao.RepoDao;
import com.example.myapplication.data.local.dao.SearchHistoryDao;
import com.example.myapplication.data.local.entity.FavoriteRepoEntity;
import com.example.myapplication.data.local.entity.RepoEntity;
import com.example.myapplication.data.local.entity.SearchHistoryEntity;
import com.example.myapplication.data.remote.ApiClient;
import com.example.myapplication.data.remote.GithubApi;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.jvm.functions.Function0;

/**
 * 仓库层：对上给 ViewModel 提供简单的数据接口，对下组合 Room、Retrofit 和 Paging。
 *
 * 面试表达可以这样说：
 * - Activity/ViewModel 不关心数据来自网络还是数据库。
 * - Repository 负责创建 Pager，并把 RemoteMediator 和 Room 的 PagingSource 连接起来。
 * - 收藏和搜索历史是本地数据，使用单线程 Executor 避免在主线程写数据库。
 */
public class RepoRepository {
    // 默认查询保证应用首次打开时就有内容可展示。
    public static final String DEFAULT_QUERY = "android language:java";

    // GitHub 搜索接口每页请求数量。PagingConfig 和 RemoteMediator 都使用这个值保持一致。
    private static final int NETWORK_PAGE_SIZE = 20;
    // Paging 内存中最多保留的条目数量，避免无限滚动时缓存过大。
    private static final int MAX_PAGING_SIZE = NETWORK_PAGE_SIZE * 5;

    private final AppDatabase appDatabase;
    private final RepoDao repoDao;
    private final SearchHistoryDao searchHistoryDao;
    private final FavoriteRepoDao favoriteRepoDao;
    private final GithubApi githubApi;
    private final ExecutorService diskExecutor;

    public RepoRepository(Context context) {
        // ApplicationContext 可以避免 Repository 持有 Activity 导致内存泄漏。
        appDatabase = AppDatabase.getInstance(context);
        repoDao = appDatabase.repoDao();
        searchHistoryDao = appDatabase.searchHistoryDao();
        favoriteRepoDao = appDatabase.favoriteRepoDao();
        githubApi = ApiClient.githubApi();
        diskExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * 观察某个关键字下的分页仓库列表。
     *
     * 数据流向：
     * GitHub API -> RepoRemoteMediator -> Room(repos 表) -> PagingSource -> RecyclerView。
     */
    public LiveData<PagingData<RepoEntity>> observePagedRepos(String keyword) {
        final String query = normalizeQuery(keyword);
        PagingConfig config = new PagingConfig(
                NETWORK_PAGE_SIZE,
                NETWORK_PAGE_SIZE,
                false,
                NETWORK_PAGE_SIZE * 3,
                MAX_PAGING_SIZE
        );

        // Pager 是 Paging 3 的核心对象：RemoteMediator 负责拉网络，PagingSource 负责读本地库。
        Pager<Integer, RepoEntity> pager = new Pager<>(
                config,
                null,
                new RepoRemoteMediator(appDatabase, githubApi, query),
                new Function0<PagingSource<Integer, RepoEntity>>() {
                    @Override
                    public PagingSource<Integer, RepoEntity> invoke() {
                        return repoDao.pagingSource();
                    }
                }
        );

        return PagingLiveData.getLiveData(pager);
    }

    /**
     * 搜索历史只读本地数据库，并由 Room 自动把表变化通知到 UI。
     */
    public LiveData<List<SearchHistoryEntity>> observeSearchHistory() {
        return searchHistoryDao.observeRecent();
    }

    /**
     * 保存用户主动输入的搜索词。
     * 默认搜索词不写入历史，避免历史列表一打开就出现系统默认值。
     */
    public void saveSearchHistory(String keyword) {
        final String query = normalizeQuery(keyword);
        if (query.equals(DEFAULT_QUERY)) {
            return;
        }
        diskExecutor.execute(() -> searchHistoryDao.insert(
                new SearchHistoryEntity(query, System.currentTimeMillis()))
        );
    }

    /**
     * 收藏列表是本地功能，不依赖网络状态。
     */
    public LiveData<List<FavoriteRepoEntity>> observeFavorites() {
        return favoriteRepoDao.observeAll();
    }

    public LiveData<Boolean> observeIsFavorite(long repoId) {
        return favoriteRepoDao.observeIsFavorite(repoId);
    }

    /**
     * 收藏按钮做成 toggle：已收藏就删除，未收藏就插入。
     */
    public void toggleFavorite(FavoriteRepoEntity favoriteRepo) {
        diskExecutor.execute(() -> {
            boolean isFavorite = favoriteRepoDao.isFavoriteSync(favoriteRepo.id) == 1;
            if (isFavorite) {
                favoriteRepoDao.deleteById(favoriteRepo.id);
            } else {
                favoriteRepoDao.insert(favoriteRepo);
            }
        });
    }

    /**
     * 统一清洗搜索词，保证空输入和 null 都落到同一个默认查询。
     */
    private String normalizeQuery(String raw) {
        if (raw == null) {
            return DEFAULT_QUERY;
        }
        String query = raw.trim();
        if (query.isEmpty()) {
            return DEFAULT_QUERY;
        }
        return query;
    }
}
