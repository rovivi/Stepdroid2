package com.kyagamy.step.room.repos
import androidx.lifecycle.LiveData
import com.kyagamy.step.room.entities.Level
import com.kyagamy.step.room.entities.LevelDao

class LevelRepository(private val levelDao: LevelDao) {

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val alllevel: LiveData<List<Level>> = levelDao.getAll()
    fun  getLevelBySongId (levelId :Int):LiveData<List<Level>> {
        return  levelDao.getLevelBySongId(levelId)
    }

    fun  deleteAll (){
        levelDao.deleteAll()
    }

    suspend fun insert(level: Level) {
        levelDao.insert(level)
    }
}