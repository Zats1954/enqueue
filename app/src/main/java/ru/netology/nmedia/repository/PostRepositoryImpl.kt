package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.entity.PostEntity

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {

    override val data: LiveData<List<Post>> = dao.getAll().map { it.map(PostEntity::toDto) }

    override suspend fun getAll() {
        val all = PostsApi.service.getAll()
        dao.insert(all.map(PostEntity.Companion::fromDto))
    }

    override suspend fun save(post: Post) {
        PostsApi.service.save(post)
    }

    override suspend fun removeById(id: Long) {
        PostsApi.service.removeById(id)
        dao.removeById(id)
    }

    override suspend fun likeById(id: Long) {
       PostsApi.service.likeById(id)
        dao.likeById(id)
    }
}