package com.kyagamy.step.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.kyagamy.step.room.repos.LevelRepository
import com.kyagamy.step.room.SDDatabase
import com.kyagamy.step.room.entities.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LevelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LevelRepository
    // Using LiveData and caching what getAlphabetizedLevels returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    public  val allLevel: LiveData<List<Level>>

    init {
        val levelsDao = SDDatabase.getDatabase(application, viewModelScope).levelDao()
        repository = LevelRepository(levelsDao)
        allLevel = repository.alllevel
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(Level: Level) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(Level)
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }

    public fun  get (songId: Int):LiveData<List<Level>> {
        return  repository.getLevelBySongId(songId)
    }

    fun queryLevels(
        songId: Int?,
        stepType: String?,
        minMeter: Int?,
        maxMeter: Int?
    ): LiveData<List<Level>> {
        return repository.queryLevels(songId, stepType, minMeter, maxMeter)
    }


}