package ru.netology.nmedia.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File
import java.io.IOException


private val empty = Post(
    id = 0,
    content = "",
    authorAvatar = "404.png",
    author = "",
    likedByMe = false,
    likes = 0,
    published = System.currentTimeMillis(),
    newPost = false
)

private val noPhoto = PhotoModel()

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(application).postDao())

    private val _data = MutableLiveData<FeedState>(FeedState.Success)
    val data: LiveData<FeedState>
        get() = _data

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo

    val newer: LiveData<Int> =  repository.data.flatMapLatest {
        val lastId = it.firstOrNull()?.id ?: 0L
        val newPosts = repository.getNewerCount(lastId)
        repository.countNew.also { countNewPosts = it }
        println("countNewPosts = ${countNewPosts} --> ${System.currentTimeMillis()}")
        newPosts
    }.catch{e-> e.printStackTrace()}
     .asLiveData(Dispatchers.Default)

    val edited = MutableLiveData(empty)

    var posts: Flow<FeedModel> = repository.data.map(::FeedModel)
    var errorMessage: String = ""
    var countNewPosts :Int = 0

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
                _postCreated.value = Unit
                try{
                    when(_photo.value){
                        noPhoto -> repository.save(it)
                        else -> _photo.value?.file?.let{ file ->
                            repository.saveWithAttachment(it, MediaUpload(file))
                        }
                    }
                    _data.value = FeedState.Success
                } catch(e:Exception){
                    _data.value = FeedState.Error
                }

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

    fun showNews() {
        viewModelScope.launch {
            try {
                repository.showNews()
            } catch (e: Exception) {
                myError(e)
                _data.value = FeedState.Error
            }
        }

    }

    fun clearCountNews() {
        countNewPosts = 0
        repository.countNew = 0
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
    }

//    fun download(imageName:String): File?{
//        var file:File? =  null
//        viewModelScope.launch {
//          file = repository.download(imageName)?.file
//        }
//        return file
//    }
}