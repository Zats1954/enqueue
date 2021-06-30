package ru.netology.nmedia.application

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth

class NMediaApplication : Application() {
    private val appScope = CoroutineScope(Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        setupAuth()
    }

    private fun setupAuth() {
        appScope.launch {
            AppAuth.initApp(this@NMediaApplication)
        }
    }
}