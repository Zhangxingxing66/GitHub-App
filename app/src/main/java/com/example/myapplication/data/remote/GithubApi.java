package com.example.myapplication.data.remote;

import com.example.myapplication.data.remote.dto.SearchResponseDto;
import com.example.myapplication.data.remote.dto.RepoDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit 接口定义。
 *
 * 每个方法描述一个 HTTP API：
 * - 注解决定路径和参数放在哪里。
 * - 返回 Call<T>，调用方可以选择同步 execute 或异步 enqueue。
 */
public interface GithubApi {
    // GET /search/repositories?q=...&sort=stars&order=desc&page=1&per_page=20
    @GET("search/repositories")
    Call<SearchResponseDto> searchRepositories(
            @Query("q") String query,
            @Query("sort") String sort,
            @Query("order") String order,
            @Query("page") int page,
            @Query("per_page") int perPage
    );

    // GET /repos/{owner}/{repo}，用于详情页获取单个仓库的最新信息。
    @GET("repos/{owner}/{repo}")
    Call<RepoDto> getRepositoryDetail(
            @Path("owner") String owner,
            @Path("repo") String repo
    );
}
