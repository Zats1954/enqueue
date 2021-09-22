package ru.netology.nmedia.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.Token

interface PostRepository {
    var countNew: Int
    val data: Flow<PagingData<Post>>
    suspend fun getAll()
    suspend fun getById(id: Long): Post
//    fun getNewerCount(id: Long): Flow<Int>
    suspend fun save(post: Post): Response<Post>
    suspend fun likeById(id: Long)
    suspend fun removeById(id: Long): Response<Unit>
    suspend fun showNews()
    suspend fun saveWithAttachment(post: Post, upload: MediaUpload): Response<Post>
    suspend fun upload(upload: MediaUpload): Media
    suspend fun autorization(login: String, pass: String): Token
    suspend fun makeUser(login: String, pass: String, name: String): Token
    suspend fun saveWork(post: Post, upload: MediaUpload?): Long
    suspend fun processWork(id: Long)

}
