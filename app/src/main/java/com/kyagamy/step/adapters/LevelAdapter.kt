package com.kyagamy.step.adapters

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kyagamy.step.R
import com.kyagamy.step.room.entities.Level
import com.squareup.picasso.Picasso
import java.util.*

class LevelAdapter internal constructor(
    context: Context
) : RecyclerView.Adapter<LevelAdapter.songViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var levels = emptyList<Level>() // Cached copy of songs
    private var lastPosition = -1
    private lateinit var context: Context

    inner class songViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val level: TextView = itemView.findViewById(R.id.item_level_text)
        val typeLevel: ImageView = itemView.findViewById(R.id.item_level_bg)
        val levelGlow: ImageView = itemView.findViewById(R.id.item_level_load_animate)
        val levelStaticGlow: ImageView = itemView.findViewById(R.id.level_item_staticBG)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): songViewHolder {
        val itemView = inflater.inflate(R.layout.level_item, parent, false)
        context = parent.context;

        return songViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: songViewHolder, position: Int) {
        val current = levels[position]
        holder.level.text = if (current.METER.length >= 2) current.METER else ("0" + current.METER)

        val anim = AnimationUtils.loadAnimation(
            context,
            R.anim.rotate
        )
        anim.duration = 1000
        Picasso.get().load(R.drawable.level_glow).into(holder.levelGlow)
//        holder.level_glow.visibility= View.VISIBLE
        holder.levelGlow.startAnimation(anim)
        holder.levelGlow.alpha = 0.5f;

        //fuente
        val custom_font = Typeface.createFromAsset(context.assets, "fonts/karnivol.ttf")

        holder.level.setTypeface(custom_font)

        Picasso.get().load(R.drawable.level_levelblack).into(holder.levelStaticGlow)

        if (current.STEPSTYPE.toLowerCase(Locale.ROOT).contains("pump-single")) {
            if (current.DESCRIPTION.toLowerCase(Locale.ROOT).contains("dp"))
                Picasso.get().load(R.drawable.level_single_perf).into(holder.typeLevel)
            else
                Picasso.get().load(R.drawable.level_single).into(holder.typeLevel)

        } else if (current.STEPSTYPE.toLowerCase(Locale.ROOT).contains("pump-double")) {
            if (current.DESCRIPTION.toLowerCase(Locale.ROOT).contains("dp"))
                Picasso.get().load(R.drawable.level_double_perf).into(holder.typeLevel)
            else
                Picasso.get().load(R.drawable.level_double).into(holder.typeLevel)
        }

        setAnimation(holder.itemView, position)
    }

    internal fun setLevels(songs: List<Level>) {
        this.levels = songs
        notifyDataSetChanged()
    }

    public fun getLevel(position: Int): Level {
        return levels[position]
    }

    override fun getItemCount() = levels.size


    private fun setAnimation(viewToAnimate: View, position: Int) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            val animation: Animation =
                AnimationUtils.loadAnimation(
                    viewToAnimate.context,
                    R.anim.bounce
                )
            animation.duration = 400
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }
}
