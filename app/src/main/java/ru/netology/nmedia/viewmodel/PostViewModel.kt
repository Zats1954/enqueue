package ru.netology.nmedia.viewmodel

import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import androidx.work.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.model.FeedState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.util.SingleLiveEvent
import ru.netology.nmedia.work.RemovePostWorker
import ru.netology.nmedia.work.SavePostWorker
import java.io.File
import java.io.IOException
import javax.inject.Inject

private val noPhoto = PhotoModel()

@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val workManager: WorkManager,
    private val dao: PostDao,
    auth: AppAuth
) : ViewModel() {
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

    private val _authChanged = SingleLiveEvent<Unit>()
    val authChanged: LiveData<Unit>
        get() = _authChanged

    private val cached = repository.data.cachedIn(viewModelScope)

    val data: Flow<PagingData<Post>> = auth
        .authStateFlow
        .flatMapLatest { (myId, _) ->
            _authChanged.value = Unit   // вызов перезагрузки постов с сервера
            val answer = cached.map { posts ->
                posts.map { it.copy(ownedByMe = it.authorId == myId) }
            }
            FeedState.Success
            answer
        }

    private val _dataState = MutableLiveData<FeedState>()
    val dataState: LiveData<FeedState>
        get() = _dataState

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo

    var newer = repository.getNewerCount().asLiveData()
    var countNewPosts = repository.countNew
    val edited = MutableLiveData(empty)
    var errorMessage: String = ""

    init {
        loadPosts()
        viewModelScope.launch {
            repository.showNews()
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
            showNews()
            _dataState.value = FeedState.Success
        } catch (e: Exception) {
            _dataState.value = FeedState.Error
        }
    }

    fun save() {
        viewModelScope.launch {
            edited.value?.let {
                try {
                    val id = repository.saveWork(
                        it, _photo.value?.uri?.let { uri ->
                            MediaUpload(uri.toFile())
                        }
                    )
                    val saveData = workDataOf(SavePostWorker.postKey to id)
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                    val request = OneTimeWorkRequestBuilder<SavePostWorker>()
                        .setInputData(saveData)
                        .setConstraints(constraints)
//                        .setInitialDelay(10, TimeUnit.SECONDS)
                        .build()
                    workManager.enqueue(request)
                    _dataState.value = FeedState.Success
                } catch (e: Exception) {
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
        post.attachment?.let {
            changePhoto(Uri.parse(post.attachment.url), File(post.attachment.url))
        }
    }

    fun changePost(post: Post) {
        val text = post.content.trim()
        var newAttachment: Attachment? = null
        if (photo.value != noPhoto) {
            newAttachment = Attachment(photo.value?.uri.toString(), AttachmentType.IMAGE)
        }
        if (edited.value?.content == text
            && edited.value?.attachment == post.attachment
        ) {
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
        try {
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
        }
    }

    private fun myError(e: Exception) {
        e.message?.let { it ->
            _dataState.postValue(FeedState.Error)
            errorMessage = when (it) {
                "500" -> "Ошибка сервера"
                "404", "HTTP 404 " -> "Страница/пост не найдены"
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

