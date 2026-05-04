package dev.francescolofranco.gymtracker.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.francescolofranco.gymtracker.data.prefs.UserPrefs
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PrefsModule {

    @Provides
    @Singleton
    fun provideUserPrefs(@ApplicationContext ctx: Context): UserPrefs = UserPrefs(ctx)
}
