package com.example.myapplication.ui;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.paging.LoadState;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.error.ErrorMapper;

/**
 * 列表底部加载状态行的 ViewHolder。
 * 根据 LoadState 在“加载中、失败、正常隐藏”三种状态间切换。
 */
public class RepoLoadStateViewHolder extends RecyclerView.ViewHolder {
    private final ProgressBar progressBar;
    private final TextView errorText;
    private final Button retryButton;

    public RepoLoadStateViewHolder(@NonNull View itemView, @NonNull View.OnClickListener retryClickListener) {
        super(itemView);
        progressBar = itemView.findViewById(R.id.loadStateProgress);
        errorText = itemView.findViewById(R.id.loadStateErrorText);
        retryButton = itemView.findViewById(R.id.loadStateRetryButton);
        retryButton.setOnClickListener(retryClickListener);
    }

    public void bind(@NonNull LoadState loadState) {
        // LoadState.Loading 表示正在加载下一页；LoadState.Error 表示加载下一页失败。
        boolean isLoading = loadState instanceof LoadState.Loading;
        boolean isError = loadState instanceof LoadState.Error;

        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        retryButton.setVisibility(isError ? View.VISIBLE : View.GONE);
        errorText.setVisibility(isError ? View.VISIBLE : View.GONE);

        if (isError) {
            Throwable error = ((LoadState.Error) loadState).getError();
            // 复用统一错误映射，保证首屏错误和底部加载错误文案一致。
            errorText.setText(ErrorMapper.userMessage(error));
        }
    }
}
