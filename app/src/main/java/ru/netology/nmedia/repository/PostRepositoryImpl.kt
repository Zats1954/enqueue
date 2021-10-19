package ru.netology.nmedia.repository


import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.asLiveData
import androidx.paging.*
import com.google.firebase.messaging.ktx.remoteMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostKeyDao
import ru.netology.nmedia.dao.PostWorkDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.*
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostWorkEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.model.ApiError
import ru.netology.nmedia.model.AppError
import ru.netology.nmedia.model.NetworkError
import ru.netology.nmedia.model.UnknownError
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val dao: PostDao,
    private val postWorkDao: PostWorkDao,
    private val service: ApiService,
    private val db: AppDb,
    private val postKeyDao: PostKeyDao
) : PostRepository {
    @ExperimentalPagingApi
    override val data: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = false),
        remoteMediator = PostRemoteMediator(service, dao, db, postKeyDao),
        pagingSourceFactory = dao::getPagingSource
    ).flow.map{
        it.map (PostEntity::toDto)
    }

    override suspend fun getAll() {
        try {
            dao.removeAll()
            val response = service.getLatest(10)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getById(id: Long): Post {
        return dao.getById(id).toDto()
    }

//    override fun getNewerCount(): Flow<Int> = flow {
//        while (true) {
//            try {
//                val lastId = postKeyDao.max() ?: 0L
//                val newer = service.getNewer(lastId).map(PostEntity.Companion::fromDto)
//                dao.insert(newer.map {
//                    val value = it.copy(newPost = true)
//                    value
//                })
//                emit(newer.size)
//                delay(30_000L)
//            } catch (e: IOException) {
//            }
//        }
//    }

    override suspend fun save(post: Post): Response<Post> {
        return service.save(post)
    }

    override suspend fun removeById(id: Long): Response<Unit> {
        val response = service.removeById(id)
        dao.removeById(id)
        return response
    }

    override suspend fun showNews() {
        dao.showNews()
    }

    override suspend fun saveWithAttachment(post: Post, upload: MediaUpload): Response<Post> {
        try {
            val media = upload(upload)
            val postWithAttachment =
                post.copy(attachment = Attachment(media.id, AttachmentType.IMAGE))
            return save(postWithAttachment)
        } catch (e: AppError) {
            throw e
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun upload(upload: MediaUpload): Media {
        try {
            val media = MultipartBody.Part.createFormData(
                "file", upload.file.name, upload.file.asRequestBody()
            )

            val response = service.upload(media)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeById(id: Long) {
        service.likeById(id)
        dao.likeById(id)
    }

    override suspend fun autorization(login: String, pass: String): Token {
        try {
            val response = service.autorization(login, pass)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun makeUser(login: String, pass: String, name: String): Token {
        try {
            val response = service.registration(login, pass, name)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveWork(post: Post, upload: MediaUpload?): Long {
        try {
            val entity = PostWorkEntity.fromDto(post).apply {
                if (upload != null) {
                    this.uri = upload.file.toUri().toString()
                }
            }
            val tmpPost = postWorkDao.insert(entity)
            return tmpPost
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun processWork(id: Long) {
        try {
            val entity = postWorkDao.getById(id)
            println("processWork ${entity}")
            val response = if (entity.uri != null) {
                val upload = MediaUpload(Uri.parse(entity.uri).toFile())
                saveWithAttachment(entity.toDto().copy(id = 0L), upload)
            } else {
                save(entity.toDto().copy(id = 0L))
            }

            if (response.isSuccessful) {
                postWorkDao.removeById(id)
            } else {
                return@processWork
            }
        } catch (e: Exception) {
            throw UnknownError
        }
    }
}