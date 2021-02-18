package com.kyagamy.step.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kyagamy.step.R
import com.kyagamy.step.common.Common
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.viewModels.LevelViewModel
import com.squareup.picasso.Picasso
import java.io.File


class  SongAdapter internal constructor(
    val context: Context,
    private val levelModel: LevelViewModel,
    val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<SongAdapter.songViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var songs = emptyList<Song>() // Cached copy of songs
    var lastPosition = -1



    inner class songViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleItemView: TextView = itemView.findViewById(R.id.songNameAdapter)
        val index: TextView = itemView.findViewById(R.id.index)
        val bpm: TextView=itemView.findViewById(R.id.tvBpm)
        val descriptionItemView: TextView = itemView.findViewById(R.id.songDescriptionAdapter)
        val imageItemView: ImageView = itemView.findViewById(R.id.banner_song_adapter)
        val levelRecyclerView :RecyclerView=  itemView.findViewById(R.id.levelRV)

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): songViewHolder {
        val itemView = inflater.inflate(R.layout.song_item, parent, false)
        return songViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: songViewHolder, position: Int) {
        //recyvler
        val levelAdapter = LevelAdapterPreview(context)
        holder.levelRecyclerView.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        holder. levelRecyclerView.adapter = levelAdapter

        levelModel.get(songs[position].song_id)
            .observe(lifecycleOwner, { level ->
                level?.let { levelAdapter.setLevels(it) }
            })


        val current = songs[position]
        holder.titleItemView.text = current.TITLE
        holder.descriptionItemView.text =current.ARTIST
        holder.bpm.text="BPM: ${current.DISPLAYBPM}"
        holder.index.text= (position+1).toString().plus("/").plus(songs.size)
        val  f = File(current.PATH_SONG + "/" + current.BANNER_SONG)
        Picasso.get().load(f).resize(250, 160).centerInside().into(holder.imageItemView);
        // holder.setSelected( selectedItems.get(position, false));
        setAnimation(holder.itemView, position)
    }

    internal fun setSongs(songs: List<Song>) {
        this.songs = songs
        notifyDataSetChanged()
    }
    public fun getSong(position: Int):Song{
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
