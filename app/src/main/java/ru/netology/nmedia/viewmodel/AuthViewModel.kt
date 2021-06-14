package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.AuthState
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.model.FeedState
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    val data: LiveData<AuthState> = AppAuth.getInstance()
        .authStateFlow
        .asLiveData(Dispatchers.Default)

    val authenticated: Boolean
        get() = AppAuth.getInstance().authStateFlow.value.id != 0L

    private var errorMessage: String = ""

    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(application).postDao())

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _dataState = MutableLiveData<FeedState>()

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

    fun signUser(login: String, pass: String) {
        viewModelScope.launch {
            _dataState.value = FeedState.Loading
            try {
                repository.autorization(login, pass).let{
                      AppAuth.getInstance().setAuth(it.id, it.token)}
                _dataState.value = FeedState.Success
            } catch (e: Exception) {
                myError(e)
                _dataState.value = FeedState.Error
            }
        }
    }

    fun signUp(login: String, pass: String, name: String) {
        viewModelScope.launch {
            _dataState.value = FeedState.Loading
            try {
                repository.makeUser(login, pass, name).let{
                    AppAuth.getInstance().setAuth(it.id, it.token)}
                _dataState.value = FeedState.Success
            } catch (e: Exception) {
                myError(e)
                _dataState.value = FeedState.Error
            }
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

    fun save() {
        _postCreated.value = Unit
    }
}