package ru.netology.nmedia.work

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.netology.nmedia.repository.PostRepository
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class WorkManagerModule {

    @Singleton
    @Provides
    fun provideWorkManager(
        @ApplicationContext
        context: Context, repository: PostRepository
    ): WorkManager {
        WorkManager.initialize(
            context, Configuration.Builder()
                .setWorkerFactory(WorkerFactoryDelegate(repository))
                .build()
        )
        return WorkManager.getInstance(context)
    }
}