package ru.netology.nmedia.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import ru.netology.nmedia.repository.PostRepository

class SavePostsFactory(private val repository: PostRepository): WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker?=
    if(workerClassName == SavePostWorker::class.java.name){
        SavePostWorker(repository, appContext, workerParameters)
    } else {null}
}
