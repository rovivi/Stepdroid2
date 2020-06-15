    package com.kyagamy.step.adapters

import android.content.Context
import android.opengl.Visibility
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
import java.util.*

    class  LevelAdapter internal constructor(
    context: Context
) : RecyclerView.Adapter<LevelAdapter.songViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var levels = emptyList<Level>() // Cached copy of songs
    private var lastPosition = -1
        private lateinit var context: Context

    inner class songViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val level: TextView = itemView.findViewById(R.id.item_level_text)
        val type_level: ImageView = itemView.findViewById(R.id.item_level_bg)
        val level_glow: ImageView = itemView.findViewById(R.id.item_level_load_animate)
        val level_statoc_glow: ImageView = itemView.findViewById(R.id.level_item_staticBG)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): songViewHolder {
        val itemView = inflater.inflate(R.layout.level_item, parent, false)
        context = parent.context;

        return songViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: songViewHolder, position: Int) {
        val current = levels[position]
        holder.level.text = current.METER

        holder.level_glow.visibility= View.VISIBLE
        holder.level_glow.startAnimation(  AnimationUtils.loadAnimation(
            context,
            R.anim.rotate
        ))


        Picasso.get().load(R.drawable.level_levelblack).into(holder.level_statoc_glow)

        if (current.STEPSTYPE.toLowerCase(Locale.ROOT).contains("pump-single")){
            if (current.DESCRIPTION .toLowerCase(Locale.ROOT).contains("dp"))
                Picasso.get().load(R.drawable.level_single_perf).into(holder.type_level)
            else
                Picasso.get().load(R.drawable.level_single).into(holder.type_level)

        }
        else if (current.STEPSTYPE.toLowerCase(Locale.ROOT).contains("pump-double")){
            if (current.DESCRIPTION.toLowerCase(Locale.ROOT).contains("dp"))
                Picasso.get().load(R.drawable.level_double_perf).into(holder.type_level)
            else
                Picasso.get().load(R.drawable.level_double).into(holder.type_level)
        }




        //holder. descriptionItemView.text = current.STEPSTYPE
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
