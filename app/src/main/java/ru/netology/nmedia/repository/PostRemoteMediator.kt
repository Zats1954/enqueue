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
//            val maxId = postKeyDao.max() ?: 0L
            val response = when (loadType) {
                LoadType.REFRESH -> {
//                    if (maxId.equals(0L)) {

                       val res = api.getLatest(pageSize)
                       if(res.isSuccessful){dao.removeAll()}
                       res
//                    } else {
//                        api.getNewer(maxId)
//                    }
                }
                LoadType.APPEND -> {
                    val id = postKeyDao.min() ?: return MediatorResult.Success(false)
                    println("********************** APPEND id ${id}")
                    api.getBefore(id, pageSize)
                }
                LoadType.PREPEND -> {
                    val id = postKeyDao.max() ?: return MediatorResult.Success(false)
                    api.getAfter(id, pageSize)
//                    return MediatorResult.Success(false)
                }
            }
            if (!response.isSuccessful) {
                throw ApiError(
                    response.code(),
                    response.message()
                )
            }
            val data = response.body() ?: throw ApiError(
                response.code(),
                response.message()
            )
            println("************************** data ${data}")
            println("************************** data*****************")
            if (data.isEmpty()) {
                return MediatorResult.Success(true)
            }

            db.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
//                        if(maxId.equals(0L)) {
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
//                        }
//                        else {  postKeyDao.insert(
//                            listOf(
//                                PostRemoteKeyEntity(
//                                    PostRemoteKeyEntity.Type.PREPEND,
//                                    data.last().id
//                                )))}
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