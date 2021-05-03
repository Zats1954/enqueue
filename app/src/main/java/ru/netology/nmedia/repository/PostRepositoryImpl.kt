package ru.netology.nmedia.repository

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.entity.PostEntity
import java.io.IOException

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    override var countNew: Int = 0
        get() = field
        set(value) {field = value}

    override val data = dao.getAll().map { it.map(PostEntity::toDto) }

    override suspend fun getAll() {
        val all = PostsApi.service.getAll()
        dao.removeAll()
        dao.insert(all.map { val value = it.copy(newPost =  false )
                            value}
                      .map(PostEntity.Companion::fromDto))
    }

    override fun getNewerCount(id:Long): Flow<Int>  = flow{
        while(true) {
            try{
                val newer = PostsApi.service.getNewer(id).map(PostEntity.Companion::fromDto)
                    dao.insert(newer.map { val value = it.copy(newPost = true)
                                           value })
                countNew = countNew + newer.size
                println("countNew -> ${countNew} --> ${System.currentTimeMillis()}")
                emit(newer.size)

            }catch(e: IOException){}
        }
    }.onEach{
        delay(30_000L)
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

    override suspend fun likeById(id: Long) {
       PostsApi.service.likeById(id)
        dao.likeById(id)
    }
}