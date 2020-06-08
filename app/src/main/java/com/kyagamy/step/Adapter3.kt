package com.kyagamy.step

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter


class MyAdapter3(private val myDataset: ArrayList<Model>,val context: Context) : PagerAdapter()
{
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = LayoutInflater.from(context)
        val layout =  inflater.inflate(R.layout.item, container, false) as ViewGroup
        //se rellena el adapdador
        val imageView:ImageView =layout.findViewById(R.id.image)

        imageView.setImageResource(myDataset[position].image)

        container.addView(layout)

        return layout
    }

    override fun getCount(): Int {
        return myDataset.size
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        //container.removeView()

    }





}