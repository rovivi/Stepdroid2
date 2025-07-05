package com.kyagamy.step.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.kyagamy.step.views.MainActivity
import com.kyagamy.step.room.entities.Category
import com.kyagamy.step.viewmodels.CategoryViewModel
import com.kyagamy.step.viewmodels.SongViewModel
import com.kyagamy.step.views.CategoryCarousel
import java.io.File

private const val POSITION = "position"

class CategoryFragament : Fragment() {
    private var position = 0
    private lateinit var categoryModel: CategoryViewModel
    private lateinit var songViewModel: SongViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            position = it.getInt(POSITION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        categoryModel = ViewModelProvider(this)[CategoryViewModel::class.java]
        songViewModel = ViewModelProvider(this)[SongViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                CategoryCarousel(
                    categoryViewModel = categoryModel,
                    songViewModel = songViewModel,
                    initialPosition = position,
                    onCategorySelected = { category, pos ->
                        position = pos
                        val mainActivity = activity as MainActivity
                        mainActivity.changeCategory(category.name, pos)
                    },
                    onCategoryChanged = { category ->
                        playSound(category)
                    }
                )
            }
        }
    }

    private fun playSound(category: Category) {
        if (category.music_category != null) {
            try {
                if (File(category.music_category.toString()).exists()) {
                    val mp = MediaPlayer()
                    mp.setDataSource(category.music_category)
                    mp.prepare()
                    mp.start()
                }
            } catch (_: Exception) {
            }
        }
    }
}
