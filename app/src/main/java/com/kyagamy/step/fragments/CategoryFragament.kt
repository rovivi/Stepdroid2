package com.kyagamy.step.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.gigamole.infinitecycleviewpager.HorizontalInfiniteCycleViewPager
import com.kyagamy.step.MainActivity
import com.kyagamy.step.R
import com.kyagamy.step.adapters.CategoryAdapter
import com.kyagamy.step.room.entities.Category
import com.kyagamy.step.viewModels.CategoryViewModel
import kotlinx.coroutines.delay
import java.io.File


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val POSITION = "position"

/**
 * A simple [Fragment] subclass.
 * Use the [CategoryFragament.newInstance] factory method to
 * create an instance of this fragment.
 */
class CategoryFragament : Fragment() {
    // TODO: Rename and change types of parameters
    private var position = 0
    private lateinit var categoryModel: CategoryViewModel
    private lateinit var cycle: HorizontalInfiniteCycleViewPager
    private lateinit var  myDataSet : List<Category>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            position = it.getInt(POSITION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_category_fragament, container, false)
        cycle = view.findViewById(R.id.cycle)
        categoryModel = ViewModelProvider(this).get(CategoryViewModel::class.java)
        val list = ArrayList<Category>()
        val adapter = CategoryAdapter(list, this.requireContext())
        cycle.adapter = adapter
        cycle.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
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
                position=pos
                playSound()
            }
        }
        )
        categoryModel.allCategory.observe(viewLifecycleOwner, { words ->
            words?.let { adapter.setSongs(it) }
        })

        val mainActivity = activity as MainActivity


        cycle.run {
            setOnTouchListener(object : OnTouchListener {
                var flage= 0
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
        return  view
    }

    override fun onStart() {
        super.onStart()
        cycle.setCurrentItem(5,true)
        cycle.notifyDataSetChanged()
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()
        try {

            playSound()
        }
        catch (ex:Exception){

        }

    }

    fun playSound () {
        if (myDataSet[position].music_category != null) {
            try {
                if (File(myDataSet[position].music_category).exists()) {
                    val mp = MediaPlayer()
                    mp.setDataSource(myDataSet[position].music_category)
                    mp.prepare()
                    mp.start()
                }
            } catch (ex: Exception) {
            }
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CategoryFragament.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: Int) =
            CategoryFragament().apply {
                arguments = Bundle().apply {
                    putInt(POSITION, param1)
                }
            }
    }
}
