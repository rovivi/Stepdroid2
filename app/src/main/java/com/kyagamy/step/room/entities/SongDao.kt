package com.kyagamy.step.room.entities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*

@Dao
interface SongDao {
    @Query("SELECT * FROM Song")
    fun getAll(): LiveData<List<Song>>


//    @Query("SELECT * FROM Song")
//    fun getList(): MutableLiveData<ArrayList<Song>>

    @Query("SELECT * FROM Song WHERE uid IN (:SongsIds)")
    fun loadAllByIds(SongsIds: IntArray): List<Song>

    @Query("SELECT * FROM Song WHERE TITLE LIKE :first")
    fun findByTitle(first: String): Song

    @Insert
    fun insertAll(vararg Song: Song)

    @Delete
    fun delete(Song: Song)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(Song: Song)
}