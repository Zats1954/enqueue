package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post

interface PostRepository {
    var countNew:Int
    val data: Flow<List<Post>>
    suspend fun getAll()
    fun getNewerCount(id:Long): Flow<Int>
    suspend fun save(post:Post)
    suspend fun likeById(id:Long)
    suspend fun removeById(id: Long)
}
