package com.example.myapplication.ui.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.core.error.ErrorMapper;
import com.example.myapplication.core.log.AppLogger;
import com.example.myapplication.data.local.entity.FavoriteRepoEntity;
import com.example.myapplication.data.repository.RepoRepository;
import com.example.myapplication.data.remote.ApiClient;
import com.example.myapplication.data.remote.dto.RepoDto;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

/**
 * 详情页 ViewModel：负责加载单个仓库详情，并处理收藏状态。
 *
 * 这里没有使用 Paging，因为详情页只请求一个仓库。
 * 先把列表页传来的 fallback 数据展示出来，再异步请求 GitHub 最新详情，用户体验会更快。
 */
public class RepoDetailViewModel extends AndroidViewModel {
    private static final String TAG = "RepoDetailViewModel";

    // 页面状态统一收敛到一个 ViewState，Activity 只需要按状态渲染 UI。
    private final MutableLiveData<RepoDetailViewState> state = new MutableLiveData<>();
    private final RepoRepository repoRepository;

    // Java 项目中使用单线程 Executor 执行同步 Retrofit 请求，避免阻塞主线程。
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public RepoDetailViewModel(@NonNull Application application) {
        super(application);
        repoRepository = new RepoRepository(application);
    }

    public LiveData<RepoDetailViewState> state() {
        return state;
    }

    public LiveData<Boolean> isFavorite(long repoId) {
        return repoRepository.observeIsFavorite(repoId);
    }

    public void toggleFavorite(FavoriteRepoEntity entity) {
        repoRepository.toggleFavorite(entity);
    }

    public void loadDetail(
            @NonNull String owner,
            @NonNull String repo,
            @NonNull String fallbackFullName,
            String fallbackDescription,
            String fallbackLanguage,
            @NonNull String fallbackOwner,
            int fallbackStars,
            int fallbackForks,
            @NonNull String fallbackHtmlUrl
    ) {
        // 先用列表页已有数据构造一个 loading 状态，让页面立即有内容可看。
        RepoDetailViewState current = new RepoDetailViewState(
                true,
                fallbackFullName,
                fallbackDescription,
                fallbackLanguage,
                fallbackOwner,
                fallbackStars,
                fallbackForks,
                fallbackHtmlUrl,
                null
        );
        state.setValue(current);

        executor.execute(() -> {
            try {
                // Retrofit 的 execute 是同步调用，必须在后台线程中执行。
                Response<RepoDto> response = ApiClient.githubApi()
                        .getRepositoryDetail(owner, repo)
                        .execute();

                if (!response.isSuccessful() || response.body() == null) {
                    // 统一抛 IOException，交给 ErrorMapper 转成用户提示。
                    throw new IOException("HTTP " + response.code());
                }

                RepoDto dto = response.body();
                // 接口字段可能为空，关键展示字段用 fallback 兜底。
                String fullName = textOrDefault(dto.fullName, fallbackFullName);
                String ownerLogin = dto.owner != null ? textOrDefault(dto.owner.login, fallbackOwner) : fallbackOwner;
                String htmlUrl = textOrDefault(dto.htmlUrl, fallbackHtmlUrl);
                RepoDetailViewState newState = new RepoDetailViewState(
                        false,
                        fullName,
                        dto.description,
                        dto.language,
                        ownerLogin,
                        dto.stargazersCount,
                        dto.forksCount,
                        htmlUrl,
                        null
                );
                state.postValue(newState);
                AppLogger.i(TAG, "Repo detail loaded for " + fullName);
            } catch (Exception e) {
                // 失败时保留 fallback 内容，只显示错误提示，不让详情页变成空白。
                String message = ErrorMapper.userMessage(e);
                AppLogger.e(TAG, "Repo detail load failed: " + ErrorMapper.map(e).debugMessage, e);
                RepoDetailViewState errorState = new RepoDetailViewState(
                        false,
                        fallbackFullName,
                        fallbackDescription,
                        fallbackLanguage,
                        fallbackOwner,
                        fallbackStars,
                        fallbackForks,
                        fallbackHtmlUrl,
                        message
                );
                state.postValue(errorState);
            }
        });
    }

    @NonNull
    private String textOrDefault(String text, @NonNull String fallback) {
        if (text == null || text.trim().isEmpty()) {
            return fallback;
        }
        return text;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // ViewModel 销毁时停止后台任务，避免页面关闭后继续持有资源。
        executor.shutdownNow();
    }
}
