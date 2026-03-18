package com.couchraoke.tv.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.couchraoke.tv.domain.library.DefaultSongLibrary
import com.couchraoke.tv.domain.library.SongLibrary
import com.couchraoke.tv.domain.session.ISession
import com.couchraoke.tv.domain.session.Session
import com.couchraoke.tv.presentation.songlist.preview.ISongPreviewController
import com.couchraoke.tv.presentation.songlist.preview.SongPreviewController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSongLibrary(): SongLibrary = DefaultSongLibrary()

    @Provides
    @Singleton
    fun provideSession(): ISession = Session(connectionCloser = null)

    @Provides
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer =
        ExoPlayer.Builder(context).build()

    @Provides
    fun provideSongPreviewController(player: ExoPlayer): ISongPreviewController =
        SongPreviewController(player)

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
