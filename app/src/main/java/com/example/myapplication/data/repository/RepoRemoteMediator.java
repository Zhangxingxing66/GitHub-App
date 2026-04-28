package com.example.myapplication.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.LoadType;
import androidx.paging.PagingState;
import androidx.paging.PagingSource;
import androidx.paging.rxjava2.RxRemoteMediator;

import com.example.myapplication.core.error.ErrorMapper;
import com.example.myapplication.core.log.AppLogger;
import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.dao.RemoteKeysDao;
import com.example.myapplication.data.local.dao.RepoDao;
import com.example.myapplication.data.local.entity.RemoteKeysEntity;
import com.example.myapplication.data.local.entity.RepoEntity;
import com.example.myapplication.data.remote.GithubApi;
import com.example.myapplication.data.remote.dto.RepoDto;
import com.example.myapplication.data.remote.dto.SearchResponseDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;

/**
 * Paging 的远程调解器：负责在列表需要数据时请求 GitHub，并把结果写入 Room。
 *
 * 这个类是“网络分页 + 本地缓存”的关键：
 * - RecyclerView 只读 Room，不直接读网络。
 * - Paging 发现本地数据不够时调用 loadSingle。
 * - loadSingle 请求网络后在事务中同时写入 repos 和 remote_keys。
 */
public class RepoRemoteMediator extends RxRemoteMediator<Integer, RepoEntity> {
    private static final String TAG = "RepoRemoteMediator";
    // GitHub 搜索 API 的页码从 1 开始，不是从 0 开始。
    private static final int STARTING_PAGE_INDEX = 1;
    // GitHub Search API 最多只允许访问前 1000 条搜索结果。
    private static final int GITHUB_SEARCH_RESULT_LIMIT = 1000;

    private final AppDatabase database;
    private final RepoDao repoDao;
    private final RemoteKeysDao remoteKeysDao;
    private final GithubApi githubApi;
    private final String query;

    public RepoRemoteMediator(
            AppDatabase database,
            GithubApi githubApi,
            String query
    ) {
        this.database = database;
        this.repoDao = database.repoDao();
        this.remoteKeysDao = database.remoteKeysDao();
        this.githubApi = githubApi;
        this.query = query;
    }

    @NonNull
    @Override
    public Single<MediatorResult> loadSingle(
            @NonNull LoadType loadType,
            @NonNull PagingState<Integer, RepoEntity> state
    ) {
        final int pageSize = state.getConfig().pageSize;
        return Single.fromCallable(() -> {
                    // 根据 refresh/prepend/append 三种加载类型计算这次应该请求哪一页。
                    int page = getPageForLoadType(loadType, state);
                    if (page == -1) {
                        // -1 表示没有上一页或下一页，Paging 可以停止继续请求。
                        return new MediatorResult.Success(true);
                    }

                    // execute 是同步网络请求，所以必须放在 io 线程中执行。
                    Response<SearchResponseDto> response = githubApi.searchRepositories(
                            query,
                            "stars",
                            "desc",
                            page,
                            pageSize
                    ).execute();
                    return handleResponse(loadType, page, pageSize, response);
                })
                .subscribeOn(Schedulers.io())
                .onErrorReturn(throwable -> {
                    // RemoteMediator 不能让异常直接崩溃，要转换为 MediatorResult.Error 交给 UI 展示。
                    AppLogger.e(TAG, ErrorMapper.map(throwable).debugMessage, throwable);
                    return new MediatorResult.Error(throwable);
                });
    }

    @NonNull
    private MediatorResult handleResponse(
            @NonNull LoadType loadType,
            int page,
            int pageSize,
            @NonNull Response<SearchResponseDto> response
    ) throws IOException {
        if (!response.isSuccessful()) {
            // 抛出带 HTTP 状态码的异常，后续 ErrorMapper 会把它映射成用户可读文案。
            throw new IOException("HTTP " + response.code());
        }

        SearchResponseDto body = response.body();
        List<RepoDto> items = body != null && body.items != null ? body.items : new ArrayList<>();
        List<RepoEntity> repos = mapToEntities(items);
        int totalAvailable = body == null ? 0 : Math.min(body.totalCount, GITHUB_SEARCH_RESULT_LIMIT);
        // 判断是否到达末尾：本页为空，或者当前页覆盖的数量已经达到 GitHub 可返回上限。
        boolean endOfPaginationReached = repos.isEmpty()
                || page * pageSize >= totalAvailable;

        database.runInTransaction(() -> {
            if (loadType == LoadType.REFRESH) {
                // 新查询或刷新时清空旧缓存，避免不同搜索词的数据混在一起。
                remoteKeysDao.clearRemoteKeys();
                repoDao.clearAll();
            }

            // RemoteKeys 记录每个条目对应的上一页/下一页，是 append/prepend 找页码的依据。
            Integer prevKey = page == STARTING_PAGE_INDEX ? null : page - 1;
            Integer nextKey = endOfPaginationReached ? null : page + 1;
            List<RemoteKeysEntity> keys = new ArrayList<>(repos.size());
            for (RepoEntity repo : repos) {
                keys.add(new RemoteKeysEntity(repo.id, prevKey, nextKey));
            }
            remoteKeysDao.insertAll(keys);
            repoDao.insertAll(repos);
        });

        AppLogger.i(TAG, "Page " + page + " synced, items=" + repos.size());
        return new MediatorResult.Success(endOfPaginationReached);
    }

    private int getPageForLoadType(
            @NonNull LoadType loadType,
            @NonNull PagingState<Integer, RepoEntity> state
    ) throws Exception {
        if (loadType == LoadType.REFRESH) {
            // refresh 尽量围绕用户当前看到的位置刷新；没有 anchor 时从第一页开始。
            @Nullable RemoteKeysEntity remoteKeys = getRemoteKeyClosestToCurrentPosition(state);
            return remoteKeys != null && remoteKeys.nextKey != null
                    ? remoteKeys.nextKey - 1
                    : STARTING_PAGE_INDEX;
        }

        if (loadType == LoadType.PREPEND) {
            // 本项目主要向下滚动，prevKey 为空时说明没有更前面的页。
            @Nullable RemoteKeysEntity remoteKeys = getRemoteKeyForFirstItem(state);
            if (remoteKeys == null || remoteKeys.prevKey == null) {
                return -1;
            }
            return remoteKeys.prevKey;
        }

        // append 是最常见的加载更多：用当前最后一个条目的 nextKey 请求下一页。
        @Nullable RemoteKeysEntity remoteKeys = getRemoteKeyForLastItem(state);
        if (remoteKeys == null || remoteKeys.nextKey == null) {
            return -1;
        }
        return remoteKeys.nextKey;
    }

    @Nullable
    private RemoteKeysEntity getRemoteKeyForLastItem(
            @NonNull PagingState<Integer, RepoEntity> state
    ) {
        // 从最后一页往前找第一个非空页面，再拿该页面最后一个 item 的分页 key。
        List<PagingSource.LoadResult.Page<Integer, RepoEntity>> pages = state.getPages();
        for (int i = pages.size() - 1; i >= 0; i--) {
            List<RepoEntity> data = pages.get(i).getData();
            if (!data.isEmpty()) {
                RepoEntity lastRepo = data.get(data.size() - 1);
                return remoteKeysDao.remoteKeysByRepoId(lastRepo.id);
            }
        }
        return null;
    }

    @Nullable
    private RemoteKeysEntity getRemoteKeyForFirstItem(
            @NonNull PagingState<Integer, RepoEntity> state
    ) {
        // 从前往后找第一个非空页面，再拿该页面第一个 item 的分页 key。
        List<PagingSource.LoadResult.Page<Integer, RepoEntity>> pages = state.getPages();
        for (PagingSource.LoadResult.Page<Integer, RepoEntity> page : pages) {
            List<RepoEntity> data = page.getData();
            if (!data.isEmpty()) {
                RepoEntity firstRepo = data.get(0);
                return remoteKeysDao.remoteKeysByRepoId(firstRepo.id);
            }
        }
        return null;
    }

    @Nullable
    private RemoteKeysEntity getRemoteKeyClosestToCurrentPosition(
            @NonNull PagingState<Integer, RepoEntity> state
    ) {
        // anchorPosition 是 RecyclerView 当前最接近视口中心的位置，用它可以让刷新后位置更稳定。
        Integer anchorPosition = state.getAnchorPosition();
        if (anchorPosition == null) {
            return null;
        }
        RepoEntity anchorRepo = state.closestItemToPosition(anchorPosition);
        if (anchorRepo == null) {
            return null;
        }
        return remoteKeysDao.remoteKeysByRepoId(anchorRepo.id);
    }

    @NonNull
    private List<RepoEntity> mapToEntities(@NonNull List<RepoDto> dtos) {
        long now = System.currentTimeMillis();
        List<RepoEntity> entities = new ArrayList<>(dtos.size());
        for (RepoDto dto : dtos) {
            // API 字段可能为空，入库前做兜底，避免 @NonNull 字段出现 null。
            String name = safeText(dto.name);
            String fullName = safeText(dto.fullName);
            if (fullName.isEmpty()) {
                fullName = name;
            }
            String ownerLogin = dto.owner != null ? safeText(dto.owner.login) : "";
            String ownerAvatar = dto.owner != null ? dto.owner.avatarUrl : null;
            entities.add(new RepoEntity(
                    dto.id,
                    name,
                    fullName,
                    dto.description,
                    dto.language,
                    dto.stargazersCount,
                    dto.forksCount,
                    ownerLogin,
                    ownerAvatar,
                    safeText(dto.htmlUrl),
                    false,
                    now
            ));
        }
        return entities;
    }

    @NonNull
    private String safeText(@Nullable String value) {
        return value == null ? "" : value;
    }
}
