package com.pim.app.di

import com.pim.app.mobile.diagnostics.DiagnosticExportRepository
import com.pim.app.mobile.diagnostics.DiagnosticOperations
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticModule {

    @Binds
    @Singleton
    abstract fun bindDiagnosticOperations(
        repository: DiagnosticExportRepository
    ): DiagnosticOperations
}
