package ru.netology.nmedia.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.*
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.util.SingleLiveEvent
import ru.netology.nmedia.work.RemovePostWorker
import ru.netology.nmedia.work.SavePostWorker
import java.io.File
import java.io.IOException

private val noPhoto = PhotoModel()

class PostViewModel(application: Application) : AndroidViewModel(application) {
    val empty = Post(
        id = 0,
        content = "",
        authorId = 0,
        authorAvatar = "404.png",
        author = "",
        likedByMe = false,
        likes = 0,
        published = System.currentTimeMillis(),
        newPost = false
    )


    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(application).postDao(),
            AppDb.getInstance(context = application).postWorkDao())

    private val workManager: WorkManager =
        WorkManager.getInstance(application)

    val data: LiveData<FeedState> = AppAuth.getInstance()
              .authStateFlow
              .flatMapLatest {(myId, _) ->
                  repository.data
                      .map{ posts ->
                          FeedModel(
                              posts.map{it.copy(ownedByMe = it.authorId == myId)},
                              posts.isEmpty()
                          )
                          FeedState.Success
                      }
              }.asLiveData(Dispatchers.Default)

    private val _dataState = MutableLiveData<FeedState>()
    val dataState: LiveData<FeedState>
      get() = _dataState

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
        newPosts
    }.catch{e-> e.printStackTrace()}
     .asLiveData(Dispatchers.Default)

    val edited = MutableLiveData(empty)
    var posts: Flow<FeedModel> = repository.data.map(::FeedModel)
    var errorMessage: String = ""
    var countNewPosts :Int = 0

    init {
           loadPosts()
                posts.asLiveData().value?.posts?.map{
                    it.copy(newPost = false)
                }
         }

    fun loadPosts() {
        viewModelScope.launch {
            _dataState.value = FeedState.Loading
            try {
                repository.getAll()
                _dataState.value = FeedState.Success
            } catch (e: Exception) {
                myError(e)
                _dataState.value = FeedState.Error
            }
        }
    }

    fun refreshPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedState.Refreshing
            repository.getAll()
            _dataState.value = FeedState.Success
        } catch (e: Exception) {
            _dataState.value = FeedState.Error
        }
    }

    fun save() {
        viewModelScope.launch {
            edited.value?.let {
                try{
                    val id = repository.saveWork(
                        it, _photo.value?.uri?.let { MediaUpload(it.toFile()) }
                    )
                    val data = workDataOf(SavePostWorker.postKey to id)
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                    val request = OneTimeWorkRequestBuilder<SavePostWorker>()
                        .setInputData(data)
                        .setConstraints(constraints)
                        .build()
                    workManager.enqueue(request)
                    _dataState.value = FeedState.Success
                } catch(e:Exception){
                    _dataState.value = FeedState.Error
                }
                _postCreated.value = Unit
            }
            edited.value = empty
            _photo.value = noPhoto
        }
    }

    fun edit(post: Post) {
        edited.value = post
       post.attachment?.let{
        changePhoto(Uri.parse(post.attachment?.url), File(post.attachment?.url) )}
    }

    fun changePost(post: Post) {
        val text = post.content.trim()
        var newAttachment: Attachment? = null
        if(photo.value != noPhoto){
            newAttachment = Attachment(photo.value?.uri.toString(), AttachmentType.IMAGE)}
        if (edited.value?.content == text
            && edited.value?.attachment == post.attachment) {
            return
        }
        edited.value = edited.value?.copy(content = text, attachment = newAttachment)

    }

    fun likeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.likeById(id)
                _dataState.value = FeedState.Success
            } catch (e: Exception) {
                myError(e)
                _dataState.value = FeedState.Error
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
                    val data = workDataOf(RemovePostWorker.postKey to id)
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                    val request = OneTimeWorkRequestBuilder<RemovePostWorker>()
                        .setInputData(data)
                        .setConstraints(constraints)
                        .build()
                    workManager.enqueue(request)
                } catch (e: Exception) {
                    myError(e)
                    _dataState.value = FeedState.Error
                }
            }
        } catch (e: IOException) {
            posts = old
        }
    }

    private fun myError(e: Exception) {
        e.message?.let { it ->
            _dataState.postValue(FeedState.Error)
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
                _dataState.value = FeedState.Error
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
}