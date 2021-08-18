package ru.netology.nmedia.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import ru.netology.nmedia.repository.PostRepository

class RemovePostFactory(private val repository: PostRepository) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? =
        if (workerClassName == RemovePostWorker::class.java.name) {
            RemovePostWorker(repository, appContext, workerParameters)
        } else {
            null
        }
}