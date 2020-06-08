package com.kyagamy.step.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kyagamy.step.room.repos.SongRepository
import com.kyagamy.step.room.SDDatabase
import com.kyagamy.step.room.entities.Category
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.room.repos.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CategoryRepository
    // Using LiveData and caching what getAlphabetizedsongs returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    public  val allCategory: LiveData<List<Category>>

    init {
        val categoryDao = SDDatabase.getDatabase(application, viewModelScope).categoryDao()
        repository = CategoryRepository(categoryDao)
        allCategory = repository.allCategory
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(cate: Category) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(cate)
    }

    fun deleteAll () =  viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }
}