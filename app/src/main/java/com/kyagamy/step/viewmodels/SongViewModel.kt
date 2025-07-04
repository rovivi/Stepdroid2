package com.kyagamy.step.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.kyagamy.step.room.repos.SongRepository
import com.kyagamy.step.room.SDDatabase
import com.kyagamy.step.room.entities.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SongViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SongRepository

    // Using LiveData and caching what getAlphabetizedsongs returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    public val allSong: LiveData<List<Song>>

    init {
        val songsDao = SDDatabase.getDatabase(application, viewModelScope).songsDao()
        repository = SongRepository(songsDao)
        allSong = repository.allSong
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(song: Song) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(song)
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }


    fun songByCategory(nameCategory: String): LiveData<List<Song>> {
        return repository.songByCategory(nameCategory)
    }

    fun songByCategoryAuthor(nameCategory: String): LiveData<List<Song>> {
        return repository.songByCategoryAuthor(nameCategory)
    }

    fun songByCategoryBPM(nameCategory: String): LiveData<List<Song>> {
        return repository.songByBPM(nameCategory)
    }


    fun songByGenre(nameCategory: String): LiveData<List<Song>> {
        return repository.songByGenre(nameCategory)
    }

    fun songBySongType(nameCategory: String): LiveData<List<Song>> {
        return repository.songBySongType(nameCategory)
    }

    fun songById(id: Int): LiveData<List<Song>> {
        return repository.idSong(id)
    }

    suspend fun randomSongs(
        category: String?,
        stepType: String?,
        minLevel: Int?,
        maxLevel: Int?,
        title: String?,
        artist: String?,
        bpm: String?,
        limit: Int
    ): List<Song> {
        return repository.getRandomSongs(
            category,
            stepType,
            minLevel,
            maxLevel,
            title,
            artist,
            bpm,
            limit
        )
    }

}