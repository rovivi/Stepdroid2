package com.kyagamy.step.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.kyagamy.step.room.SDDatabase
import com.kyagamy.step.room.entities.Category
import com.kyagamy.step.room.repos.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CategoryRepository
    public  val allCategory: LiveData<List<Category>>

    init {
        val categoryDao = SDDatabase.getDatabase(application, viewModelScope).categoryDao()
        repository = CategoryRepository(categoryDao)
        allCategory = repository.allCategory
    }

    fun insert(cate: Category) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(cate)
    }

    fun deleteAll () =  viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }

}