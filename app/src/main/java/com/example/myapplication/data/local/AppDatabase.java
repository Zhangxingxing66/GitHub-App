package com.example.myapplication.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.myapplication.data.local.dao.FavoriteRepoDao;
import com.example.myapplication.data.local.dao.RemoteKeysDao;
import com.example.myapplication.data.local.dao.RepoDao;
import com.example.myapplication.data.local.dao.SearchHistoryDao;
import com.example.myapplication.data.local.entity.FavoriteRepoEntity;
import com.example.myapplication.data.local.entity.RemoteKeysEntity;
import com.example.myapplication.data.local.entity.RepoEntity;
import com.example.myapplication.data.local.entity.SearchHistoryEntity;

/**
 * Room 数据库入口。
 *
 * Room 会根据这里声明的 entity 和 dao 在编译期生成 AppDatabase_Impl。
 * 应用代码只依赖这个抽象类，真正的 SQLite 操作由 Room 生成代码完成。
 */
@Database(
        // 当前数据库包含：分页缓存、分页 key、搜索历史、收藏仓库。
        entities = {RepoEntity.class, RemoteKeysEntity.class, SearchHistoryEntity.class, FavoriteRepoEntity.class},
        version = 4,
        // 导出 schema 方便查看表结构变化，也方便面试时展示数据库演进意识。
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "github_browser.db";
    // volatile 保证多线程读取 INSTANCE 时能看到最新赋值，配合双重检查锁实现单例。
    private static volatile AppDatabase INSTANCE;

    // 每个 abstract dao 方法都会由 Room 生成具体实现。
    public abstract RepoDao repoDao();
    public abstract RemoteKeysDao remoteKeysDao();
    public abstract SearchHistoryDao searchHistoryDao();
    public abstract FavoriteRepoDao favoriteRepoDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // 使用 applicationContext，避免数据库单例间接持有 Activity。
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            // 学习项目中允许破坏式迁移；生产项目应写 Migration 保留用户数据。
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
