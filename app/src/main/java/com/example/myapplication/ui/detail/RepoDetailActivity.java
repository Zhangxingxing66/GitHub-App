package com.example.myapplication.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.FavoriteRepoEntity;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * 仓库详情页：展示仓库基础信息、README WebView，以及收藏/取消收藏按钮。
 *
 * 职责边界：
 * - Activity 负责接收 Intent 参数、初始化控件、渲染 ViewState。
 * - ViewModel 负责请求详情接口和操作收藏数据。
 */
public class RepoDetailActivity extends AppCompatActivity {
    // Intent extra key 集中定义，避免调用方和接收方使用散落的字符串。
    private static final String EXTRA_REPO_ID = "extra_repo_id";
    private static final String EXTRA_OWNER = "extra_owner";
    private static final String EXTRA_REPO = "extra_repo";
    private static final String EXTRA_FULL_NAME = "extra_full_name";
    private static final String EXTRA_DESCRIPTION = "extra_description";
    private static final String EXTRA_LANGUAGE = "extra_language";
    private static final String EXTRA_STARS = "extra_stars";
    private static final String EXTRA_FORKS = "extra_forks";
    private static final String EXTRA_HTML_URL = "extra_html_url";

    private long repoId;
    private String owner;
    private String repoName;
    private String fallbackFullName;
    private String fallbackDescription;
    private String fallbackLanguage;
    private int fallbackStars;
    private int fallbackForks;
    private String fallbackHtmlUrl;

    private TextView tvFullName;
    private TextView tvDescription;
    private TextView tvOwner;
    private TextView tvLanguage;
    private TextView tvStars;
    private TextView tvForks;
    private TextView tvError;
    private ProgressBar progressBar;
    private WebView webView;
    private Button favoriteButton;
    private RepoDetailViewState currentState;
    private boolean currentFavorite;
    private RepoDetailViewModel viewModel;

    /**
     * 统一创建详情页 Intent，调用方不需要知道具体 extra key。
     */
    public static Intent newIntent(
            @NonNull Context context,
            long repoId,
            @NonNull String owner,
            @NonNull String repo,
            @NonNull String fullName,
            String description,
            String language,
            int stars,
            int forks,
            @NonNull String htmlUrl
    ) {
        Intent intent = new Intent(context, RepoDetailActivity.class);
        intent.putExtra(EXTRA_REPO_ID, repoId);
        intent.putExtra(EXTRA_OWNER, owner);
        intent.putExtra(EXTRA_REPO, repo);
        intent.putExtra(EXTRA_FULL_NAME, fullName);
        intent.putExtra(EXTRA_DESCRIPTION, description);
        intent.putExtra(EXTRA_LANGUAGE, language);
        intent.putExtra(EXTRA_STARS, stars);
        intent.putExtra(EXTRA_FORKS, forks);
        intent.putExtra(EXTRA_HTML_URL, htmlUrl);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repo_detail);

        parseIntent();
        bindViews();
        setupToolbar();
        setupWebView();
        bindViewModel();
    }

    private void parseIntent() {
        Intent intent = getIntent();
        // 这些 fallback 字段来自列表页缓存，详情接口失败时仍能展示基本信息。
        repoId = intent.getLongExtra(EXTRA_REPO_ID, 0L);
        owner = safeText(intent.getStringExtra(EXTRA_OWNER));
        repoName = safeText(intent.getStringExtra(EXTRA_REPO));
        fallbackFullName = safeText(intent.getStringExtra(EXTRA_FULL_NAME));
        fallbackDescription = intent.getStringExtra(EXTRA_DESCRIPTION);
        fallbackLanguage = intent.getStringExtra(EXTRA_LANGUAGE);
        fallbackStars = intent.getIntExtra(EXTRA_STARS, 0);
        fallbackForks = intent.getIntExtra(EXTRA_FORKS, 0);
        fallbackHtmlUrl = safeText(intent.getStringExtra(EXTRA_HTML_URL));
    }

    private void bindViews() {
        tvFullName = findViewById(R.id.tvDetailFullName);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvOwner = findViewById(R.id.tvDetailOwner);
        tvLanguage = findViewById(R.id.tvDetailLanguage);
        tvStars = findViewById(R.id.tvDetailStars);
        tvForks = findViewById(R.id.tvDetailForks);
        tvError = findViewById(R.id.tvDetailError);
        progressBar = findViewById(R.id.detailProgress);
        webView = findViewById(R.id.webReadme);
        favoriteButton = findViewById(R.id.btnFavorite);
        // 收藏按钮只负责发出动作，真正的插入/删除逻辑在 ViewModel/Repository。
        favoriteButton.setOnClickListener(v -> toggleFavorite());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.detailToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.repo_detail_title);
        }
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        // GitHub README 页面需要 JavaScript/DOM Storage 才能更接近浏览器展示效果。
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
    }

    private void bindViewModel() {
        viewModel = new ViewModelProvider(this).get(RepoDetailViewModel.class);
        // 详情内容、loading、错误信息都从同一个 ViewState 渲染。
        viewModel.state().observe(this, this::renderState);
        // 收藏状态来自 Room 的 LiveData，本地表变化后按钮文案会自动刷新。
        viewModel.isFavorite(repoId).observe(this, isFavorite -> {
            currentFavorite = isFavorite != null && isFavorite;
            updateFavoriteButtonText();
        });
        // 先传入 fallback 数据用于即时展示，再由 ViewModel 请求网络详情。
        viewModel.loadDetail(
                owner,
                repoName,
                fallbackFullName,
                fallbackDescription,
                fallbackLanguage,
                owner,
                fallbackStars,
                fallbackForks,
                fallbackHtmlUrl
        );
    }

    private void renderState(@NonNull RepoDetailViewState state) {
        currentState = state;
        progressBar.setVisibility(state.loading ? View.VISIBLE : View.GONE);

        // Activity 不做复杂业务判断，只把 ViewState 中的数据格式化到控件上。
        tvFullName.setText(state.fullName);
        tvDescription.setText(emptyAsFallback(state.description));
        tvOwner.setText(getString(R.string.repo_owner_value, state.ownerLogin));
        tvLanguage.setText(getString(R.string.repo_language_value, emptyAsUnknown(state.language)));
        tvStars.setText(getString(R.string.repo_stars_value, state.stars));
        tvForks.setText(getString(R.string.repo_forks_value, state.forks));

        if (state.errorMessage == null || state.errorMessage.trim().isEmpty()) {
            tvError.setVisibility(View.GONE);
        } else {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(getString(R.string.repo_detail_error, state.errorMessage));
        }

        if (!state.htmlUrl.trim().isEmpty()) {
            String readmeUrl = state.htmlUrl + "#readme";
            if (!readmeUrl.equals(webView.getUrl())) {
                // URL 没变时不重复 load，避免 LiveData 重放导致 WebView 闪烁。
                webView.loadUrl(readmeUrl);
            }
        }
    }

    private void toggleFavorite() {
        RepoDetailViewState state = currentState;
        if (state == null) {
            // 还没有任何可收藏的数据时忽略点击。
            return;
        }

        // 收藏表保存详情页需要的核心字段，离线打开收藏列表也能展示。
        FavoriteRepoEntity favorite = new FavoriteRepoEntity(
                repoId,
                repoName,
                state.fullName,
                state.description,
                state.language,
                state.stars,
                state.forks,
                state.ownerLogin,
                state.htmlUrl,
                System.currentTimeMillis()
        );
        viewModel.toggleFavorite(favorite);
    }

    private void updateFavoriteButtonText() {
        favoriteButton.setText(currentFavorite
                ? R.string.remove_favorite
                : R.string.add_favorite);
    }

    private String emptyAsFallback(String text) {
        if (text == null || text.trim().isEmpty()) {
            return getString(R.string.repo_no_description);
        }
        return text;
    }

    private String emptyAsUnknown(String text) {
        if (text == null || text.trim().isEmpty()) {
            return getString(R.string.unknown_language);
        }
        return text;
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            // WebView 持有较多资源，页面销毁时主动释放。
            webView.destroy();
        }
    }
}
