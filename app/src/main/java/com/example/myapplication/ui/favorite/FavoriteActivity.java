package com.example.myapplication.ui.favorite;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.FavoriteRepoEntity;
import com.example.myapplication.ui.detail.RepoDetailActivity;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

/**
 * 收藏列表页：展示本地 favorite_repos 表中的仓库。
 * 这个页面不需要网络，请求失败不会影响已收藏内容的查看。
 */
public class FavoriteActivity extends AppCompatActivity {
    private FavoriteListAdapter adapter;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        setupToolbar();
        setupList();
        bindViewModel();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.favoriteToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.favorite_title);
        }
    }

    private void setupList() {
        RecyclerView recyclerView = findViewById(R.id.recyclerFavorites);
        emptyView = findViewById(R.id.tvFavoriteEmpty);
        adapter = new FavoriteListAdapter(this::openDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void bindViewModel() {
        FavoriteViewModel viewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        // Room LiveData 在收藏表变化时自动通知页面刷新。
        viewModel.favorites().observe(this, this::renderFavorites);
    }

    private void renderFavorites(List<FavoriteRepoEntity> favorites) {
        adapter.submitList(favorites);
        // 空状态和列表共用同一块页面空间，通过可见性切换。
        boolean isEmpty = favorites == null || favorites.isEmpty();
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void openDetail(@NonNull FavoriteRepoEntity repo) {
        // 收藏列表也复用详情页，并把本地保存的字段作为 fallback 数据传过去。
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
