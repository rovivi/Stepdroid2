package com.kyagamy.step.room.entities

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface LevelDao {
    @Query("SELECT * FROM Level order by STEPSTYPE asc, `index` desc")
    fun getAll(): LiveData<List<Level>>

    @Query("SELECT * FROM Level where song_id = :songId order by STEPSTYPE desc, `index` asc")
    fun getLevelBySongId(songId: Int ): LiveData<List<Level>>

    @Query("delete from Level")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(cate: Level)
}