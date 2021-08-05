package ru.netology.nmedia.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.AuthState
import ru.netology.nmedia.model.FeedState
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.util.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val repository: PostRepository,
                                        private val auth: AppAuth) : ViewModel() {

    val data: LiveData<AuthState> = auth
        .authStateFlow
        .asLiveData(Dispatchers.Default)

    val authenticated: Boolean
        get() = auth.authStateFlow.value.id != 0L

    private var errorMessage: String = ""

    private val _authCreated = SingleLiveEvent<Unit>()
    val authCreated: LiveData<Unit>
        get() = _authCreated

    private val _dataState = MutableLiveData<FeedState>()
    val dataState: LiveData<FeedState>
        get() = _dataState
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
                      auth.setAuth(it.id, it.token)}
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
                    auth.setAuth(it.id, it.token)}
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
        _authCreated.value = Unit
    }
}