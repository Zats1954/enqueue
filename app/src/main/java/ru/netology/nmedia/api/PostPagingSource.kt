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
                    api.getLatest(params.loadSize)}
                is LoadParams.Append -> {
                    api.getBefore(params.key, params.loadSize)}
                is LoadParams.Prepend -> {
                     return  LoadResult.Page(
                           data = emptyList(),
                           prevKey = params.key,
                           nextKey = null )
                }
            }
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