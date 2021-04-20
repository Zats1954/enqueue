package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedState
import ru.netology.nmedia.model.FeedState.*
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
    var errorMessage: String = ""
    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(application).postDao())
    private val _data = MutableLiveData<FeedState>(Success)
    val data: LiveData<FeedState>
        get() = _data
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated
    var posts: LiveData<List<Post>> = repository.data

    init {
           loadPosts()
         }

    fun loadPosts() {
        viewModelScope.launch {
            _data.value = Loading
            try {
                repository.getAll()
                _data.value = Success
            } catch (e: Exception) {
                myError(e)
                _data.value = Error
            }
        }
    }


    fun save() {
        viewModelScope.launch {
            edited.value?.let {
                repository.save(it)
                _postCreated.value = Unit
            }
            edited.value = empty
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
        viewModelScope.launch {
            try {
                repository.likeById(id)
                _data.value = Success
            } catch (e: Exception) {
                myError(e)
                _data.value = Error
            }
        }
    }

    fun removeById(id: Long) {
        val old = posts
        try {
            posts.value?.filter {
                it.id != id
            }
            viewModelScope.launch {
                try {
                    repository.removeById(id)
                } catch (e: Exception) {
                    myError(e)
                    _data.value = Error
                }
            }
        } catch (e: IOException) {
            posts = old
        }
    }

    private fun myError(e: Exception) {
        e.message?.let { it ->
            _data.postValue(Error)
            errorMessage = when (it) {
                "500" -> "Ошибка сервера"
                "404","HTTP 404 " -> "Страница/пост не найдены"
                else -> "Ошибка соединения"
            }
        }
    }
}