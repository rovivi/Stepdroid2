package com.kyagamy.step.adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import com.kyagamy.step.R
import com.kyagamy.step.room.entities.Category


class CategoryAdapter(var myDataSet: List<Category>, val context: Context) :
    PagerAdapter() {
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    public fun list():List<Category>{
        return myDataSet
    }




    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.item, container, false) as ViewGroup
        //se rellena el adapdador
        val imageView: ImageView = layout.findViewById(R.id.image)
        val textTitle: TextView = layout.findViewById(R.id.titleCate)

        if (myDataSet[position].banner != null) {
            val img = BitmapFactory.decodeFile(myDataSet[position].banner)
            imageView.setImageBitmap(img)

        }

        textTitle.text = myDataSet[position].name
        container.addView(layout)
        Log.println(Log.ASSERT, "awa$position", "" + position + "awa")

        return layout
    }

    override fun getCount(): Int {
        return myDataSet.size
    }


    internal fun setSongs(cate: List<Category>) {
        this.myDataSet = cate
        notifyDataSetChanged()
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View?)

    }


}