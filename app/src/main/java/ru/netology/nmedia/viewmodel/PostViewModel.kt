package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedState
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
    var newPostsCount :Int = 0
        get() = field
        set(value)  {field = value}
    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(application).postDao())
    private val _data = MutableLiveData<FeedState>(FeedState.Success)
    val data: LiveData<FeedState>
        get() = _data

    val newer: LiveData<Int> =  repository.data.flatMapLatest {
        val lastId = it.firstOrNull()?.id ?: 0L
//        val countNew =
            repository.getNewerCount(lastId)
//           newPostsCount = repository.countNew
//        countNew.map{value ->  newPostsCount.plus(value.singleOrNull() ?: 0)}
//        countNew
    }
        .catch{e-> e.printStackTrace()}
        .asLiveData(Dispatchers.Default)


    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated
    var posts: Flow<FeedModel> = repository.data.map(::FeedModel)

    init {
           loadPosts()
         }

    fun loadPosts() {
        viewModelScope.launch {
            _data.value = FeedState.Loading
            try {
                repository.getAll()
                _data.value = FeedState.Success
            } catch (e: Exception) {
                myError(e)
                _data.value = FeedState.Error
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
                _data.value = FeedState.Success
            } catch (e: Exception) {
                myError(e)
                _data.value = FeedState.Error
            }
        }
    }

    fun removeById(id: Long) {
        val old = posts
        try {
            posts.asLiveData(Dispatchers.Default).value?.posts?.filter {
                it.id != id
            }
            viewModelScope.launch {
                try {
                    repository.removeById(id)
                } catch (e: Exception) {
                    myError(e)
                    _data.value = FeedState.Error
                }
            }
        } catch (e: IOException) {
            posts = old
        }
    }

    private fun myError(e: Exception) {
        e.message?.let { it ->
            _data.postValue(FeedState.Error)
            errorMessage = when (it) {
                "500" -> "Ошибка сервера"
                "404","HTTP 404 " -> "Страница/пост не найдены"
                else -> "Ошибка соединения"
            }
        }
    }
}