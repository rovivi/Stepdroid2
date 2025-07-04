package com.kyagamy.step.room.entities

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface LevelDao {
    @Query("SELECT * FROM Level order by STEPSTYPE asc, `index` desc")
    fun getAll(): LiveData<List<Level>>

    @Query("SELECT * FROM Level where song_fkid = :songId order by STEPSTYPE desc, `index` asc")
    fun getLevelBySongId(songId: Int ): LiveData<List<Level>>

    @Query("delete from Level")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(cate: Level)

    @Query(
        """
        SELECT * FROM Level
        WHERE (:songId IS NULL OR song_fkid = :songId)
          AND (:stepType IS NULL OR STEPSTYPE LIKE '%' || :stepType || '%')
          AND (:minMeter IS NULL OR METER >= :minMeter)
          AND (:maxMeter IS NULL OR METER <= :maxMeter)
        ORDER BY STEPSTYPE ASC, `index` DESC
        """
    )
    fun queryLevels(
        songId: Int?,
        stepType: String?,
        minMeter: Int?,
        maxMeter: Int?
    ): LiveData<List<Level>>
}