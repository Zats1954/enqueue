package ru.netology.nmedia.repository

import androidx.paging.*
import androidx.room.withTransaction
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.model.ApiError


@ExperimentalPagingApi
class PostRemoteMediator(
    private val api: ApiService,
    private val dao: PostDao,
    private val db: AppDb,
    private val postKeyDao: PostKeyDao,
): RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val pageSize = state.config.pageSize
            val response = when (loadType) {
                 LoadType.REFRESH  -> {
//                     dao.removeAll()
                    api.getLatest(pageSize)}
                 LoadType.APPEND -> {
                     val id = postKeyDao.min() ?: return MediatorResult.Success(false)
                     api.getBefore(id, pageSize)}
                 LoadType.PREPEND -> {
//                     val id = postKeyDao.max() ?:
                       return MediatorResult.Success(true)
//                     api.getAfter(id, pageSize)
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

            if(data.isEmpty()){
                return MediatorResult.Success(true)
            }

            db.withTransaction {
                when(loadType){
                    LoadType.REFRESH -> {
                        postKeyDao.insert(
                            listOf(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.Type.PREPEND,
                                    data.first().id
                                ),
                                PostRemoteKeyEntity(
                                        PostRemoteKeyEntity.Type.APPEND,
                                data.last().id
                            )

                            )
                        )
//                        dao.removeAll()
                    }
                    LoadType.PREPEND -> {
                        postKeyDao.insert(
                           PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.Type.PREPEND,
                                    data.first().id
                                )
                        )
                    }
                    LoadType.APPEND -> {
                        postKeyDao.insert(
                            PostRemoteKeyEntity(
                                PostRemoteKeyEntity.Type.APPEND,
                                data.last().id
                            )
                        )
                    }
                }
                dao.insert(data.map(PostEntity.Companion::fromDto))
            }

            return MediatorResult.Success(false)
        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}