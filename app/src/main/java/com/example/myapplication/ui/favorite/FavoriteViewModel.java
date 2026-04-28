package com.example.myapplication.ui.favorite;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.myapplication.data.local.entity.FavoriteRepoEntity;
import com.example.myapplication.data.repository.RepoRepository;

import java.util.List;

/**
 * 收藏页 ViewModel。
 * 它只暴露收藏列表 LiveData，让 Activity 不需要知道 DAO 或 Repository 的细节。
 */
public class FavoriteViewModel extends AndroidViewModel {
    private final LiveData<List<FavoriteRepoEntity>> favorites;

    public FavoriteViewModel(@NonNull Application application) {
        super(application);
        RepoRepository repoRepository = new RepoRepository(application);
        // 收藏表变化时，Room 会自动把新的列表推送给页面。
        favorites = repoRepository.observeFavorites();
    }

    public LiveData<List<FavoriteRepoEntity>> favorites() {
        return favorites;
    }
}
