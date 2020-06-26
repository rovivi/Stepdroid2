package com.kyagamy.step

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.gigamole.infinitecycleviewpager.HorizontalInfiniteCycleViewPager


private const val NUM_PAGES = 5

class CardActivity :  FragmentActivity() {

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private lateinit var viewPager: ViewPager2
    private lateinit var cycle :HorizontalInfiniteCycleViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card)

        cycle = findViewById(R.id.cycle)

        val arrayModel =ArrayList<Model>()


        val adapter22 = MyAdapter(arrayModel)
        val adapter33 =MyAdapter3(arrayModel,this)
        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.viewpager2Category)

        // The pager adapter, which provides the pages to the view pager widget.

        cycle.adapter=adapter33
        cycle.minPageScaleOffset=4f

        viewPager.adapter = adapter22
        viewPager.offscreenPageLimit=3
    }

    override fun onBackPressed() {
        if (viewPager.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            viewPager.currentItem = viewPager.currentItem - 1
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment = DetailsFragment()
    }
}
