package com.example.myapplication.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.paging.LoadState;
import androidx.paging.LoadStateAdapter;

import com.example.myapplication.R;

/**
 * Paging 列表底部的加载状态适配器。
 * 它只负责 append/prepend 这类“加载更多”的状态行，首屏状态由 MainActivity 单独渲染。
 */
public class RepoLoadStateAdapter extends LoadStateAdapter<RepoLoadStateViewHolder> {
    // 点击重试按钮时回调 PagingDataAdapter.retry。
    private final Runnable retry;

    public RepoLoadStateAdapter(@NonNull Runnable retry) {
        this.retry = retry;
    }

    @NonNull
    @Override
    public RepoLoadStateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, @NonNull LoadState loadState) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_load_state, parent, false);
        // ViewHolder 不直接依赖 Adapter，只拿到一个点击监听。
        return new RepoLoadStateViewHolder(view, v -> retry.run());
    }

    @Override
    public void onBindViewHolder(@NonNull RepoLoadStateViewHolder holder, @NonNull LoadState loadState) {
        holder.bind(loadState);
    }
}
