    package com.kyagamy.step.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kyagamy.step.R
import com.kyagamy.step.common.Common
import com.kyagamy.step.room.entities.Level
import com.squareup.picasso.Picasso
import java.io.File

class  LevelAdapter internal constructor(
    context: Context
) : RecyclerView.Adapter<LevelAdapter.songViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var levels = emptyList<Level>() // Cached copy of songs
    private var lastPosition = -1

    inner class songViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleItemView: TextView = itemView.findViewById(R.id.songNameAdapter)
        val descriptionItemView: TextView = itemView.findViewById(R.id.songDescriptionAdapter)

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): songViewHolder {
        val itemView = inflater.inflate(R.layout.song_item, parent, false)
        return songViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: songViewHolder, position: Int) {
        val current = levels[position]
        holder.titleItemView.text = current.METER
        holder. descriptionItemView.text = current.STEPSTYPE
        // holder.setSelected( selectedItems.get(position, false));
        setAnimation(holder.itemView, position)
    }

    internal fun setLevels(songs: List<Level>) {
        this.levels = songs
        notifyDataSetChanged()
    }
    public fun getLevel (position:Int):Level{
        return levels[position]
    }

    override fun getItemCount() = levels.size


    private fun setAnimation(viewToAnimate: View, position: Int) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            //Animation animation =
            // AnimationUtils.loadAnimation(a, android.R.anim.slide_in_left);
            val animation: Animation = if (Common.getRandomNumberInRange(0, 10) > 5) {
                AnimationUtils.loadAnimation(
                    viewToAnimate.context,
                    R.anim.bounce
                )
            } else {
                AnimationUtils.loadAnimation(
                    viewToAnimate.context,
                    android.R.anim.slide_in_left
                )
            }
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }
}
