package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.IOException

private val empty = Post(
    id = 0,
    content = "",
    authorAvatar = "404.png",
    author = "",
    likedByMe = false,
    likes = 0,
    published = System.currentTimeMillis()
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositoryImpl()
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel>
        get() = _data
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
    }

    fun loadPosts() {
        _data.value = FeedModel(loading = true)
        repository.getAllAsync(object : PostRepository.GetAllCallback {
            override fun onSuccess(posts: List<Post>) {
                _data.postValue(
                    FeedModel(
                        posts = posts,
                        empty = posts.isEmpty()
                    )
                )
            }

            override fun onError(e: Exception) {
                myError(e)
            }
        })
    }



    fun save() {
        edited.value?.let {
            repository.saveAsync(object : PostRepository.SaveCallback {
                override fun onSuccess(post: Post) {
                    val posts = _data.value?.posts.orEmpty().plus(post)
                    _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
                    _postCreated.postValue(Unit)
                }

                override fun onError(e: Exception) {
                    _data.postValue(FeedModel(error = true))
                }
            }, it)
        }

    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun likeById(id: Long) {
        repository.likeByIdASync(object : PostRepository.LikeIdCallback {
            override fun onSuccess(post: Post) {
                val posts =
                    data.value?.posts.orEmpty().map { if (it.id == post.id) post else it }
                _data.postValue(
                    FeedModel(
                        posts = posts,
                        empty = posts.isEmpty()
                    )
                )
                _postCreated.postValue(Unit)
            }

            override fun onError(e: Exception) {
                _data.postValue(FeedModel(error = true))
            }
        }, id)
    }

    fun removeById(id: Long) {
        val old = _data.value?.posts.orEmpty()
        try {
            val posts = _data.value?.posts.orEmpty().filter { it.id != id }
            repository.removeByIdAsync(object : PostRepository.RemoveIdCallback {
                override fun onSuccess() {
                    _data.postValue(
                        FeedModel(
                            posts = posts,
                            empty = posts.isEmpty()
                        )
                    )
                    _postCreated.postValue(Unit)
                }

                override fun onError(e: Exception) {
                    _data.postValue(FeedModel(error = true))
                }
            }, id)
        } catch (e: IOException) {
            _data.postValue(_data.value?.copy(posts = old))
        }
    }

    private fun myError(e: Exception) {
        e.message?.let { it ->
            if (it in "300".."599") {
                _data.postValue(FeedModel(error = true, message = errorMessage(it)))
            } else
                _data.postValue(FeedModel(error = true))
        }
    }
    private fun errorMessage(message: String): String {
        return when(message) {
            "500" -> "Ошибка сервера"
            "404" -> "Страница не найдена"
            else -> "Ошибка соединения"
        }

    }
}

