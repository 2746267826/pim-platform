package com.pim.app.di

import androidx.hilt.work.ChildWorkerFactory
import com.pim.app.daemon.UploadWorker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.WorkerKey
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerModule {

    @Binds
    @IntoMap
    @WorkerKey(UploadWorker::class)
    abstract fun bindUploadWorker(factory: UploadWorker.Factory): ChildWorkerFactory
}
