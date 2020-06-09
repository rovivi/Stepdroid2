package com.kyagamy.step.room.entities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Category order by name asc")
    fun getAll(): LiveData<List<Category>>

//    @Query("SELECT * FROM Category")
//    fun getList(): MutableLiveData<ArrayList<Category>>


    @Insert
    fun insertAll(vararg cate: Category)

    @Delete
    fun delete(cate: Category)

    @Query("delete from Category")
    fun deleteAll()


    @Query(value = "select  * from Category where name=:arg0")
    fun getByName(arg0:String):Category

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(cate: Category)

}