package ru.netology.nmedia.repository

import androidx.paging.*
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.model.ApiError


@ExperimentalPagingApi
class PostRemoteMediator(
    private val api: ApiService,
    private val dao: PostDao,
): RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val pageSize = state.config.pageSize
            val response = when (loadType) {
                 LoadType.REFRESH  -> {
                     dao.removeAll()
                    api.getLatest(pageSize)}
                 LoadType.APPEND -> {
                     val item = state.lastItemOrNull() ?: return MediatorResult.Success(false)
                     api.getBefore(item.id, pageSize)}
                 LoadType.PREPEND -> {
                     val item = state.firstItemOrNull() ?: return MediatorResult.Success(false)
                     api.getAfter(item.id, pageSize)
                }
            }
            if (!response.isSuccessful){throw ApiError(
                response.code(),
                response.message()
            )}
            val data = response.body() ?: throw ApiError(
                response.code(),
                response.message()
            )
            dao.insert(data.map(PostEntity.Companion::fromDto))
            return MediatorResult.Success(data.isEmpty())
        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}