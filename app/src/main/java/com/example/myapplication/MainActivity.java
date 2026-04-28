package com.example.myapplication;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.CombinedLoadStates;
import androidx.paging.LoadState;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.core.error.ErrorMapper;
import com.example.myapplication.core.log.AppLogger;
import com.example.myapplication.data.local.entity.RepoEntity;
import com.example.myapplication.data.local.entity.SearchHistoryEntity;
import com.example.myapplication.ui.MainViewModel;
import com.example.myapplication.ui.RepoLoadStateAdapter;
import com.example.myapplication.ui.RepoPagingAdapter;
import com.example.myapplication.ui.SearchHistoryAdapter;
import com.example.myapplication.ui.detail.RepoDetailActivity;
import com.example.myapplication.ui.favorite.FavoriteActivity;

import java.util.List;

import kotlin.Unit;

/**
 * 应用首页：负责把搜索框、搜索历史、仓库列表、加载状态和错误状态串起来。
 *
 * 重点：
 * - Activity 属于 View 层，只处理控件事件和页面跳转。
 * - 数据来自 MainViewModel，不在这里直接请求网络或操作数据库。
 * - 列表状态来自 Paging 的 LoadState，用它区分首屏加载、刷新、错误和空状态。
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // ViewModel 持有页面数据和搜索条件，Activity 负责观察并刷新 UI。
    private MainViewModel viewModel;
    // 主列表使用 PagingDataAdapter，支持按页加载和自动差分刷新。
    private RepoPagingAdapter adapter;
    // 搜索历史数据量小，用普通 RecyclerView.Adapter 即可。
    private SearchHistoryAdapter historyAdapter;
    // 以下字段缓存页面控件，方便在不同状态下统一更新可见性和文本。
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar loadingView;
    private View errorContainer;
    private TextView errorText;
    private TextView emptyView;
    private EditText searchEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ViewModelProvider 会在配置变化后复用同一个 ViewModel，避免搜索状态立即丢失。
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // 初始化顺序：先拿控件，再准备列表和历史列表，最后订阅 ViewModel 数据。
        initViews();
        initList();
        initHistory();
        bindViewModel();
    }

    /**
     * 初始化页面顶部控件，并绑定搜索、刷新、重试、收藏入口等用户事件。
     */
    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        loadingView = findViewById(R.id.progressInitial);
        errorContainer = findViewById(R.id.errorContainer);
        errorText = findViewById(R.id.tvErrorMessage);
        emptyView = findViewById(R.id.tvEmpty);
        Button retryButton = findViewById(R.id.btnRetry);
        Button searchButton = findViewById(R.id.btnSearch);
        Button favoritesButton = findViewById(R.id.btnFavorites);
        searchEditText = findViewById(R.id.etSearch);

        // 下拉刷新交给 Paging 重新 refresh，而不是在 Activity 中手写网络请求。
        swipeRefreshLayout.setOnRefreshListener(() -> adapter.refresh());
        // retry 会重试上一次失败的 Paging 请求。
        retryButton.setOnClickListener(v -> adapter.retry());
        searchButton.setOnClickListener(v -> submitSearch());
        favoritesButton.setOnClickListener(v -> openFavorites());
        // 同时支持软键盘“搜索”和物理键盘回车。
        searchEditText.setOnEditorActionListener(this::onSearchEditorAction);
        // 回填当前搜索词，保证页面重建后输入框和 ViewModel 状态一致。
        searchEditText.setText(viewModel.currentQuery());
    }

    /**
     * 初始化仓库分页列表。
     * withLoadStateFooter 会在列表底部追加“加载更多/加载失败重试”的状态行。
     */
    private void initList() {
        RecyclerView recyclerView = findViewById(R.id.recyclerRepos);
        adapter = new RepoPagingAdapter(this::openRepoDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter.withLoadStateFooter(new RepoLoadStateAdapter(() -> adapter.retry())));

        adapter.addLoadStateListener(loadStates -> {
            renderLoadState(loadStates);
            return Unit.INSTANCE;
        });
    }

    /**
     * 初始化横向搜索历史列表，点击历史词会重新发起搜索。
     */
    private void initHistory() {
        RecyclerView historyRecycler = findViewById(R.id.recyclerHistory);
        historyAdapter = new SearchHistoryAdapter(this::searchFromHistory);
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        historyRecycler.setLayoutManager(layoutManager);
        historyRecycler.setAdapter(historyAdapter);
        new LinearSnapHelper().attachToRecyclerView(historyRecycler);
    }

    /**
     * 处理输入法 actionSearch 和键盘回车，返回 true 表示事件已经被消费。
     */
    private boolean onSearchEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean imeSearch = actionId == EditorInfo.IME_ACTION_SEARCH;
        boolean enterKey = event != null
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
        if (imeSearch || enterKey) {
            submitSearch();
            return true;
        }
        return false;
    }

    private void submitSearch() {
        // 这里不直接处理空字符串，统一交给 ViewModel/Repository 做规范化。
        String query = searchEditText.getText().toString();
        viewModel.search(query);
    }

    private void searchFromHistory(String keyword) {
        // 点击历史词后同步更新输入框，避免 UI 展示和真实搜索条件不一致。
        searchEditText.setText(keyword);
        searchEditText.setSelection(keyword.length());
        viewModel.search(keyword);
    }

    private void openRepoDetail(RepoEntity repo) {
        // 详情页会先使用列表已有字段兜底展示，再请求 GitHub 详情接口刷新。
        startActivity(RepoDetailActivity.newIntent(
                this,
                repo.id,
                repo.ownerLogin,
                repo.name,
                repo.fullName,
                repo.description,
                repo.language,
                repo.stars,
                repo.forks,
                repo.htmlUrl
        ));
    }

    private void openFavorites() {
        startActivity(new android.content.Intent(this, FavoriteActivity.class));
    }

    private void bindViewModel() {
        // PagingData 提交给 PagingDataAdapter 后，适配器内部会处理差分和加载更多。
        viewModel.repos().observe(this, pagingData -> adapter.submitData(getLifecycle(), pagingData));
        // 搜索历史来自 Room 的 LiveData，表数据变化时会自动回调到这里。
        viewModel.searchHistory().observe(this, this::renderSearchHistory);
    }

    private void renderSearchHistory(List<SearchHistoryEntity> history) {
        historyAdapter.submitList(history);
    }

    private void renderLoadState(CombinedLoadStates loadStates) {
        // refresh 是当前查询的主加载状态；append/prepend 的加载更多状态交给 footer 展示。
        LoadState refreshState = loadStates.getRefresh();
        boolean isLoading = refreshState instanceof LoadState.Loading;
        boolean isError = refreshState instanceof LoadState.Error;
        boolean isEmpty = !isLoading && !isError && adapter.getItemCount() == 0;

        // 首屏加载用居中的 ProgressBar；已有数据时刷新用下拉刷新控件的顶部转圈。
        swipeRefreshLayout.setRefreshing(isLoading && adapter.getItemCount() > 0);
        loadingView.setVisibility(isLoading && adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        errorContainer.setVisibility(isError && adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);

        if (isError) {
            Throwable throwable = ((LoadState.Error) refreshState).getError();
            // 用户看到友好提示，日志保留更具体的调试信息，方便排查线上问题。
            String message = ErrorMapper.userMessage(throwable);
            errorText.setText(message);
            AppLogger.w(TAG, "List load error: " + ErrorMapper.map(throwable).debugMessage);
        }
    }
}
