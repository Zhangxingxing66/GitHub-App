package com.example.myapplication.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.SearchHistoryEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页横向搜索历史适配器。
 * 搜索历史最多 10 条，因此这里使用简单列表并 notifyDataSetChanged 即可。
 */
public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.HistoryViewHolder> {
    // 点击历史词后由 Activity 决定如何触发搜索。
    public interface OnHistoryClickListener {
        void onHistoryClick(@NonNull String keyword);
    }

    private final List<SearchHistoryEntity> items = new ArrayList<>();
    private final OnHistoryClickListener listener;

    public SearchHistoryAdapter(@NonNull OnHistoryClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<SearchHistoryEntity> list) {
        // Adapter 内部持有一份可变列表，外部传入 null 时按空列表处理。
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tvHistoryKeyword);
        }

        void bind(@NonNull SearchHistoryEntity item, @NonNull OnHistoryClickListener listener) {
            textView.setText(item.keyword);
            // 点击 chip 后把 keyword 回调给首页，首页再更新输入框和 ViewModel。
            itemView.setOnClickListener(v -> listener.onHistoryClick(item.keyword));
        }
    }
}
