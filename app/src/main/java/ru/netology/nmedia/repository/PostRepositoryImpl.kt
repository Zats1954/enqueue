package ru.netology.nmedia.repository


import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostWorkDao
import ru.netology.nmedia.dto.*
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostWorkEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.model.ApiError
import ru.netology.nmedia.model.AppError
import ru.netology.nmedia.model.UnknownError
import ru.netology.nmedia.model.NetworkError
import java.io.IOException

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    override var countNew: Int = 0
        get() = field
        set(value) {field = value}

    override val data = dao.getAll().map(List<PostEntity>::toDto).flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        val all = PostsApi.service.getAll()
        dao.removeAll()
        dao.insert(all.map { val value = it.copy(newPost =  false )
                            value}
                      .map(PostEntity.Companion::fromDto))
    }

    override fun getNewerCount(id:Long): Flow<Int> = flow{
        while(true) {
            try{
                val newer = PostsApi.service.getNewer(id).map(PostEntity.Companion::fromDto)
                    dao.insert(newer.map { val value = it.copy(newPost = true)
                                           value })
                countNew = countNew + newer.size
                emit(newer.size)
                delay(30_000L)
            }catch(e: IOException){}
        }
    }

    override suspend fun save(post: Post) {
        PostsApi.service.save(post)
    }

    override suspend fun removeById(id: Long) {
        PostsApi.service.removeById(id)
        dao.removeById(id)
    }

    override suspend fun showNews() {
       dao.showNews()
    }

    override suspend fun saveWithAttachment(post: Post, upload: MediaUpload) {
        try {
            val media = upload(upload)
            val postWithAttachment = post.copy(attachment = Attachment(media.id, AttachmentType.IMAGE))
            save(postWithAttachment)
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
}