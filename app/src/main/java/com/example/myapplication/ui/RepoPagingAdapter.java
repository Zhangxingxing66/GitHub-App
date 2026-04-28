package com.example.myapplication.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.RepoEntity;

/**
 * 首页仓库列表适配器。
 *
 * 继承 PagingDataAdapter 后，不需要自己维护完整列表；
 * Paging 会按需把当前位置附近的数据提交给 Adapter。
 */
public class RepoPagingAdapter extends PagingDataAdapter<RepoEntity, RepoPagingAdapter.RepoViewHolder> {
    // 用接口把“点击 item 后做什么”交给 Activity，Adapter 只负责显示。
    public interface OnRepoClickListener {
        void onRepoClick(@NonNull RepoEntity repo);
    }

    /**
     * DiffUtil 用于判断新旧数据差异，只刷新真正变化的行。
     * 这比 notifyDataSetChanged 更高效，也能保留 RecyclerView 的动画和滚动体验。
     */
    private static final DiffUtil.ItemCallback<RepoEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<RepoEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull RepoEntity oldItem, @NonNull RepoEntity newItem) {
                    // 主键相同表示是同一个仓库。
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull RepoEntity oldItem, @NonNull RepoEntity newItem) {
                    // 内容字段相同表示 UI 不需要重新绑定。
                    return oldItem.fullName.equals(newItem.fullName)
                            && textEquals(oldItem.description, newItem.description)
                            && textEquals(oldItem.language, newItem.language)
                            && oldItem.stars == newItem.stars
                            && oldItem.forks == newItem.forks;
                }

                private boolean textEquals(String left, String right) {
                    if (left == null) {
                        return right == null;
                    }
                    return left.equals(right);
                }
            };

    private final OnRepoClickListener onRepoClickListener;

    public RepoPagingAdapter(@NonNull OnRepoClickListener onRepoClickListener) {
        super(DIFF_CALLBACK);
        this.onRepoClickListener = onRepoClickListener;
    }

    @NonNull
    @Override
    public RepoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_repo, parent, false);
        return new RepoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RepoViewHolder holder, int position) {
        RepoEntity item = getItem(position);
        if (item != null) {
            // Paging 可能在占位符场景返回 null；本项目关闭占位符，这里仍保留防御判断。
            holder.bind(item, onRepoClickListener);
        }
    }

    /**
     * ViewHolder 缓存 item_repo.xml 中的控件引用，避免滚动时重复 findViewById。
     */
    static class RepoViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView description;
        private final TextView meta;

        RepoViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            description = itemView.findViewById(R.id.tvDescription);
            meta = itemView.findViewById(R.id.tvMeta);
        }

        void bind(@NonNull RepoEntity repo, @NonNull OnRepoClickListener listener) {
            title.setText(repo.fullName);

            // GitHub 仓库描述可能为空，UI 上用统一文案兜底。
            if (repo.description == null || repo.description.trim().isEmpty()) {
                description.setText(R.string.repo_no_description);
            } else {
                description.setText(repo.description);
            }

            // language 为空时显示“未知语言”，避免元信息区域出现空白。
            String language = repo.language == null || repo.language.trim().isEmpty()
                    ? itemView.getContext().getString(R.string.unknown_language)
                    : repo.language;

            String metaText = itemView.getContext().getString(
                    R.string.repo_meta_value,
                    language,
                    repo.stars,
                    repo.forks
            );
            meta.setText(metaText);
            itemView.setOnClickListener(v -> listener.onRepoClick(repo));
        }
    }
}
