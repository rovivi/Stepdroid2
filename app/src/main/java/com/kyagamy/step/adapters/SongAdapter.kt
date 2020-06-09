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
import com.kyagamy.step.room.entities.Song
import com.squareup.picasso.Picasso
import java.io.File

class  SongAdapter internal constructor(
    context: Context
) : RecyclerView.Adapter<SongAdapter.songViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var songs = emptyList<Song>() // Cached copy of songs
    var lastPosition = -1

    inner class songViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleItemView: TextView = itemView.findViewById(R.id.songNameAdapter)
        val descriptionItemView: TextView = itemView.findViewById(R.id.songDescriptionAdapter)
        val imageItemView: ImageView = itemView.findViewById(R.id.banner_song_adapter)

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): songViewHolder {
        val itemView = inflater.inflate(R.layout.song_item, parent, false)
        return songViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: songViewHolder, position: Int) {
        val current = songs[position]
        holder.titleItemView.text = current.TITLE
        holder.descriptionItemView.text = "awa"+current.song_id
        val  f = File(current.PATH_SONG+"/"+current.BANNER_SONG)
        Picasso.get().load(f).resize(200, 120).centerInside().into(holder.imageItemView);

        // holder.setSelected( selectedItems.get(position, false));
        setAnimation(holder.itemView, position)


    }

    internal fun setSongs(songs: List<Song>) {
        this.songs = songs
        notifyDataSetChanged()
    }
    public fun getSong (position:Int):Song{
        return songs[position]
    }

    override fun getItemCount() = songs.size


    private fun setAnimation(viewToAnimate: View, position: Int) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            val animation: Animation
            //Animation animation =
            // AnimationUtils.loadAnimation(a, android.R.anim.slide_in_left);
            animation = if (Common.getRandomNumberInRange(0, 10) > 5) {
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
