package com.kyagamy.step.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kyagamy.step.R
import com.kyagamy.step.room.entities.Song

class SongAdapter internal constructor(
    context: Context
) : RecyclerView.Adapter<SongAdapter.songViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var songs = emptyList<Song>() // Cached copy of songs

    inner class songViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songItemView: TextView = itemView.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): songViewHolder {
        val itemView = inflater.inflate(R.layout.recyclerview_item, parent, false)
        return songViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: songViewHolder, position: Int) {
        val current = songs[position]
        holder.songItemView.text = current.TITLE
    }

    internal fun setSongs(songs: List<Song>) {
        this.songs = songs
        notifyDataSetChanged()
    }

    override fun getItemCount() = songs.size
}
