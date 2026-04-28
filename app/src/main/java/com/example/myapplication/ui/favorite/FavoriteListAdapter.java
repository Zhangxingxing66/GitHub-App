package com.example.myapplication.ui.favorite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.FavoriteRepoEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 收藏列表适配器。
 * 收藏数据量通常不大，所以这里使用普通 RecyclerView.Adapter，而不是 PagingDataAdapter。
 */
public class FavoriteListAdapter extends RecyclerView.Adapter<FavoriteListAdapter.FavoriteViewHolder> {
    // 点击收藏项后由 Activity 负责打开详情页。
    public interface OnFavoriteClickListener {
        void onFavoriteClick(@NonNull FavoriteRepoEntity repo);
    }

    private final List<FavoriteRepoEntity> items = new ArrayList<>();
    private final OnFavoriteClickListener listener;

    public FavoriteListAdapter(@NonNull OnFavoriteClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<FavoriteRepoEntity> favorites) {
        // 简化实现：整体替换本地列表。数据量大时可以改成 ListAdapter + DiffUtil。
        items.clear();
        if (favorites != null) {
            items.addAll(favorites);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_repo, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView description;
        private final TextView meta;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            description = itemView.findViewById(R.id.tvDescription);
            meta = itemView.findViewById(R.id.tvMeta);
        }

        void bind(@NonNull FavoriteRepoEntity repo, @NonNull OnFavoriteClickListener listener) {
            title.setText(repo.fullName);
            // 收藏表中也可能保存空描述，展示时用统一兜底文案。
            if (repo.description == null || repo.description.trim().isEmpty()) {
                description.setText(R.string.repo_no_description);
            } else {
                description.setText(repo.description);
            }
            String language = repo.language == null || repo.language.trim().isEmpty()
                    ? itemView.getContext().getString(R.string.unknown_language)
                    : repo.language;
            meta.setText(itemView.getContext().getString(
                    R.string.repo_meta_value,
                    language,
                    repo.stars,
                    repo.forks
            ));
            itemView.setOnClickListener(v -> listener.onFavoriteClick(repo));
        }
    }
}
