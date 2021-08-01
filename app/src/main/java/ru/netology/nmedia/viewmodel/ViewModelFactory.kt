package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.repository.PostRepository

class ViewModelFactory(private val repository: PostRepository,
                       private val workManager: WorkManager,
                       private val auth: AppAuth): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        when {
            @Suppress("UNCHECKED_CAST")
            modelClass.isAssignableFrom(PostViewModel::class.java) -> {
                PostViewModel(repository, workManager, auth) as T
            }
            @Suppress("UNCHECKED_CAST")
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel( repository, auth) as T
            }
            else -> error("Unknown view model clas ${modelClass.name}")
        }
}