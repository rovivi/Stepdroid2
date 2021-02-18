package com.kyagamy.step.room.entities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*

@Dao
interface SongDao {
    @Query("SELECT * FROM Song order  By TITLE asc")
    fun getAll(): LiveData<List<Song>>


    @Query("SELECT * FROM Song where SONGCATEGORY like:filter order  By TITLE asc")
    fun getByCategory(filter:String): LiveData<List<Song>>


    @Query("SELECT * FROM Song where GENRE like:filter order  By TITLE asc")
    fun getByGenre(filter:String): LiveData<List<Song>>

    @Query("SELECT * FROM Song where SONGTYPE like:filter order  By TITLE asc")
    fun getBySongType(filter:String): LiveData<List<Song>>

    @Query("SELECT * FROM Song where catecatecate like :categoryName order By TITLE asc")
    fun getCategory(categoryName: String ): LiveData<List<Song>>

    @Query("SELECT * FROM Song where catecatecate like :categoryName order By ARTIST asc")
    fun getCategoryByAuthor(categoryName: String ): LiveData<List<Song>>

    @Query("SELECT * FROM Song where catecatecate like :categoryName order By DISPLAYBPM asc")
    fun getCategoryBPM(categoryName: String ): LiveData<List<Song>>

//    @Query("SELECT * FROM Song")
//    fun getList(): MutableLiveData<ArrayList<Song>>

    @Query("SELECT * FROM Song WHERE song_id = :SongsId")
    fun loadAllByIds(SongsId: Int): LiveData<List<Song>>

    @Query("SELECT * FROM Song WHERE TITLE LIKE :first")
    fun findByTitle(first: String): Song

    @Insert
    fun insertAll(vararg Song: Song)

    @Query("delete from Song")
    fun deleteAll()


    @Delete
    fun delete(Song: Song)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(Song: Song)
}