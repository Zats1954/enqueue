package ru.netology.nmedia.api

import androidx.paging.PagingSource
import androidx.paging.PagingState
import ru.netology.nmedia.dto.Post

class PostPagingSource(private val api: ApiService): PagingSource<Long, Post>() {
    override fun getRefreshKey(state: PagingState<Long, Post>): Long? = null

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Post> {
        try {
            val result = when (params) {
                is LoadParams.Refresh -> {
                    println("************************** Refresh ${params.loadSize}")
                    api.getLatest(params.loadSize)}
                is LoadParams.Append -> {
                    println("************************** Append ${params.key} ${params.loadSize}")
                    api.getBefore(params.key, params.loadSize)}
                is LoadParams.Prepend -> {
                    println("************************** Prepend ${params.key}")
                    return  LoadResult.Page(
                           data = emptyList(),
                           prevKey = params.key,
                           nextKey = null )
                }
            }
            println("************************** result ${result.body()}")
            val data = result.body() ?: error("Empty body")
            val key = data.lastOrNull()?.id
            return LoadResult.Page(
                data = data,
                prevKey = params.key,
                nextKey = key,
            )
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }
}