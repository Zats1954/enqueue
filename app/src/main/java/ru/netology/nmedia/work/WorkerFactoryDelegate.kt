package ru.netology.nmedia.work

import androidx.work.DelegatingWorkerFactory
import ru.netology.nmedia.repository.PostRepository

class WorkerFactoryDelegate(
    private val repository: PostRepository
): DelegatingWorkerFactory() {
    init{
        addFactory(RefreshPostsFactory(repository))
        addFactory(SavePostsFactory(repository))
        addFactory(RemovePostFactory(repository))
    }

}