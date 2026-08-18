package cn.forever24.tutor.android.di

import android.content.Context
import cn.forever24.tutor.android.BuildConfig
import cn.forever24.tutor.android.auth.AuthRepository
import cn.forever24.tutor.android.auth.AuthSessionStore
import cn.forever24.tutor.android.auth.SharedPreferencesAuthSessionStore
import cn.forever24.tutor.android.network.TutorApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTutorApiClient(): TutorApiClient =
        TutorApiClient(BuildConfig.TUTOR_API_BASE_URL)

    @Provides
    @Singleton
    fun provideAuthSessionStore(@ApplicationContext context: Context): AuthSessionStore =
        SharedPreferencesAuthSessionStore(context)

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiClient: TutorApiClient,
        sessionStore: AuthSessionStore,
    ): AuthRepository =
        AuthRepository(apiClient, sessionStore)
}
