package ru.netology.nmedia.repository

import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dto.Post
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PostRepositoryImpl : PostRepository {
    override fun getAllAsync(callback: PostRepository.GetAllCallback) {
       PostApi.service.getAll()
           .enqueue(object : Callback<List<Post>> {
                override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                    if (response.isSuccessful) {
                        callback.onSuccess(response.body().orEmpty())
                    } else {
                        if(response.code() in  300..599)
                            callback.onError(RuntimeException(response.code().toString()))
                        else
                            callback.onError(RuntimeException(response.message()))
                    }
                }

                override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                    callback.onError(RuntimeException(t))
                }
            })
    }



    override fun saveAsync(callback: PostRepository.SaveCallback, post: Post) {
        PostApi.service.save(post)
                .enqueue(object : Callback<Post> {
                    override fun onResponse(call: Call<Post>, response: Response<Post>) {
                        if (response.isSuccessful) {
                            callback.onSuccess(response.body() ?: throw java.lang.RuntimeException("body is null"))
                        } else {
                            if(response.code() in  300..599)
                                callback.onError(RuntimeException(response.code().toString()))
                            else
                                callback.onError(RuntimeException(response.message()))
                        }
                    }
                    override fun onFailure(call: Call<Post>, t: Throwable) {
                          callback.onError(RuntimeException(t))
                    }
                })
    }

    override fun removeByIdAsync(callback: PostRepository.RemoveIdCallback, id: Long) {
        PostApi.service.removeById(id)
            .enqueue(object : Callback<Unit> {
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    if (response.isSuccessful) {
                        callback.onSuccess( )
                    } else {
                        if(response.code() in  300..599)
                            callback.onError(RuntimeException(response.code().toString()))
                        else
                            callback.onError(RuntimeException(response.message()))
                    }
                }
                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    callback.onError(RuntimeException(t))
                }
            })
    }
    override fun likeByIdASync(callback: PostRepository.LikeIdCallback, id: Long) {
        PostApi.service.likeById(id)
            .enqueue(object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    if (response.isSuccessful) {
                        callback.onSuccess(response.body() ?: throw java.lang.RuntimeException("body is null") )
                    } else {
                        if(response.code() in  300..599)
                            callback.onError(RuntimeException(response.code().toString()))
                        else
                            callback.onError(RuntimeException(response.message()))
                    }
                }
                override fun onFailure(call: Call<Post>, t: Throwable) {
                    callback.onError(RuntimeException(t))
                }
            })
    }
}
