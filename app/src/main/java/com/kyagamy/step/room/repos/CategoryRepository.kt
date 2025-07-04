package com.kyagamy.step.room.repos

import androidx.lifecycle.LiveData
import com.kyagamy.step.room.entities.Category
import com.kyagamy.step.room.entities.CategoryDao
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.room.entities.SongDao

class CategoryRepository(private val cateDao: CategoryDao) {

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val allCategory: LiveData<List<Category>> = cateDao.getAll()

    suspend fun insert(cate: Category) {
        cateDao.insert(cate)
    }

    fun searchByName(filter: String): LiveData<List<Category>> {
        return cateDao.searchByName(filter)
    }

    suspend fun deleteAll() {
        cateDao.deleteAll()
    }


}