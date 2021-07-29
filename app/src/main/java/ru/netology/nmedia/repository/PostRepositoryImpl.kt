package ru.netology.nmedia.repository


import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostWorkDao
import ru.netology.nmedia.dto.*
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostWorkEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.model.ApiError
import ru.netology.nmedia.model.AppError
import ru.netology.nmedia.model.UnknownError
import ru.netology.nmedia.model.NetworkError
import java.io.IOException

class PostRepositoryImpl(private val dao: PostDao,
                         private val postWorkDao: PostWorkDao) : PostRepository {
    override var countNew: Int = 0
        get() = field
        set(value) {field = value}

    override val data = dao.getAll().map {
        it.map(PostEntity::toDto)}.flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        try{
        dao.removeAll()
        val response = PostsApi.service.getAll()
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

    override fun getNewerCount(id:Long): Flow<Int>  = flow{
        while(true) {
            try{
                val newer = PostsApi.service.getNewer(id).map(PostEntity.Companion::fromDto)
                    dao.insert(newer.map { val value = it.copy(newPost = true)
                                           value })
                countNew = newer.size
                emit(newer.size)
                delay(30_000L)
            }catch(e: IOException){}
        }
    }

    override suspend fun save(post: Post): Response<Post> {
       return  PostsApi.service.save(post)
      }

    override suspend fun removeById(id: Long):Response<Unit> {
        val response = PostsApi.service.removeById(id)
        dao.removeById(id)
        return response
    }

    override suspend fun showNews() {
       dao.showNews()
    }

    override suspend fun saveWithAttachment(post: Post, upload: MediaUpload): Response<Post> {
        try {
            val media = upload(upload)
            val postWithAttachment = post.copy(attachment = Attachment(media.id, AttachmentType.IMAGE))
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

            val response = PostsApi.service.upload(media)
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
       PostsApi.service.likeById(id)
        dao.likeById(id)
    }

    override suspend fun autorization(login: String, pass: String): Token {
        try {
            val response = PostsApi.service.autorization(login, pass)
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

    override suspend fun makeUser(login: String, pass: String, name:String): Token {
        try {
             val response = PostsApi.service.registration(login, pass, name)
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
            return postWorkDao.insert(entity)
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun processWork(id: Long) {
        try {
            // TODO: handle this in homework
            val entity = postWorkDao.getById(id)
            val response = if (entity.uri != null) {
                val upload = MediaUpload(Uri.parse(entity.uri).toFile())
                saveWithAttachment(entity.toDto().copy(id = 0L), upload)
            }
            else{save(entity.toDto().copy(id = 0L))}

            if(response.isSuccessful){postWorkDao.removeById(id)}
            else{return@processWork}

        } catch (e: Exception) {
            throw UnknownError
        }
    }
}