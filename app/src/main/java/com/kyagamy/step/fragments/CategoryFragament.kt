package com.kyagamy.step.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.kyagamy.step.views.MainActivity
import com.kyagamy.step.adapters.CategoryAdapter
import com.kyagamy.step.databinding.FragmentCategoryFragamentBinding
import com.kyagamy.step.room.entities.Category
import com.kyagamy.step.viewmodels.CategoryViewModel
import java.io.File


private const val POSITION = "position"


class CategoryFragament : Fragment() {
    private var position = 0
    private lateinit var categoryModel: CategoryViewModel

    private lateinit var myDataSet: List<Category>

    private val binding: FragmentCategoryFragamentBinding by lazy {
        FragmentCategoryFragamentBinding.inflate(LayoutInflater.from(context), null, false)
    }

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
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Inflate the layout for this fragment
        categoryModel = ViewModelProvider(this).get(CategoryViewModel::class.java)
        val list = ArrayList<Category>()
        val adapter = CategoryAdapter(list, this.requireContext())
        binding.cycle.adapter = adapter

        binding.cycle.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(pos: Int) {
                // Check if this is the page you want.
                myDataSet = adapter.myDataSet
                position = pos
                playSound()
            }
        }

        )
        categoryModel.allCategory.observe(viewLifecycleOwner) { words ->
            words?.let { adapter.setSongs(it) }
        }
        val mainActivity  = activity as MainActivity
        binding.cycle.run {
            setOnTouchListener(object : OnTouchListener {
                var flage = 0
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> flage = 0
                        MotionEvent.ACTION_MOVE -> flage = 1
                        MotionEvent.ACTION_UP -> if (flage == 0) {
                            mainActivity.changeCategory(adapter.myDataSet[currentItem].name,currentItem)
                        }
                    }
                    return false
                }
            })
        }
        binding.button.setOnClickListener{

            mainActivity.changeCategory(adapter.myDataSet[position].name,position)

        }
    }




    override fun onResume() {
        super.onResume()
        try {
            playSound()
        } catch (_: Exception) {

        }

    }

    fun playSound() {
        if (myDataSet[position].music_category != null) {
            try {
                if (File(myDataSet[position].music_category.toString()).exists()) {
                    val mp = MediaPlayer()
                    mp.setDataSource(myDataSet[position].music_category)
                    mp.prepare()
                    mp.start()
                }
            } catch (_: Exception) {
            }
        }
    }

}
