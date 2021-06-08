package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.Token

interface PostRepository {
    var countNew: Int
    val data: Flow<List<Post>>
    suspend fun getAll()
    fun getNewerCount(id:Long): Flow<Int>
    suspend fun save(post:Post)
    suspend fun likeById(id:Long)
    suspend fun removeById(id: Long)
    suspend fun showNews()
    suspend fun saveWithAttachment(post: Post, upload: MediaUpload)
    suspend fun upload(upload: MediaUpload): Media
    suspend fun autorization(login: String, pass: String): Token
    suspend fun makeUser(login: String, pass: String, name: String): Token
}
