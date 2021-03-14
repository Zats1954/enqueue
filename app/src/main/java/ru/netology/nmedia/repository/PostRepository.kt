package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun likeByIdASync(callback: LikeIdCallback,id:Long)
    fun getAllAsync(callback: GetAllCallback)
    fun saveAsync(callback: SaveCallback, post:Post)
    fun removeByIdAsync(callback:RemoveIdCallback,id: Long)

    interface GetAllCallback {
        fun onSuccess(posts: List<Post>) {}
        fun onError(e: Exception) {}
    }

    interface SaveCallback {
        fun onSuccess(post: Post) {}
        fun onError(e: Exception) {}
    }

    interface RemoveIdCallback {
        fun onSuccess() {}
        fun onError(e: Exception) {}
    }

    interface LikeIdCallback {
        fun onSuccess(post:Post) {}
        fun onError(e: Exception) {}
    }
}
