package com.example.myapplication.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.PagingData;

import com.example.myapplication.data.local.entity.RepoEntity;
import com.example.myapplication.data.local.entity.SearchHistoryEntity;
import com.example.myapplication.data.repository.RepoRepository;

import java.util.List;

/**
 * 首页 ViewModel：保存当前搜索词，并把 Repository 的数据流暴露给 Activity。
 *
 * AndroidViewModel 可以拿到 Application，适合这里创建 Repository。
 * Activity 销毁重建时 ViewModel 不会立刻销毁，因此搜索条件和 LiveData 可以继续复用。
 */
public class MainViewModel extends AndroidViewModel {
    private final RepoRepository repoRepository;

    // 当前搜索条件。改变它会触发 switchMap 重新创建分页数据流。
    private final MutableLiveData<String> queryLiveData = new MutableLiveData<>(RepoRepository.DEFAULT_QUERY);
    private final LiveData<PagingData<RepoEntity>> repos;
    private final LiveData<List<SearchHistoryEntity>> searchHistory;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repoRepository = new RepoRepository(application);
        // switchMap 的作用：当 queryLiveData 变化时，取消旧查询并订阅新查询的分页结果。
        repos = Transformations.switchMap(queryLiveData, repoRepository::observePagedRepos);
        searchHistory = repoRepository.observeSearchHistory();
    }

    public LiveData<PagingData<RepoEntity>> repos() {
        return repos;
    }

    public LiveData<List<SearchHistoryEntity>> searchHistory() {
        return searchHistory;
    }

    public void search(String query) {
        String normalized = normalizeQuery(query);
        // 只有搜索词真的变化时才触发新一轮 Paging 查询，避免重复刷新。
        if (!normalized.equals(queryLiveData.getValue())) {
            queryLiveData.setValue(normalized);
        }
        // 即使搜索词没变，也可以更新时间戳，让历史记录排序更符合用户最近操作。
        repoRepository.saveSearchHistory(normalized);
    }

    public String currentQuery() {
        return normalizeQuery(queryLiveData.getValue());
    }

    private String normalizeQuery(String raw) {
        if (raw == null) {
            return RepoRepository.DEFAULT_QUERY;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return RepoRepository.DEFAULT_QUERY;
        }
        return trimmed;
    }
}
