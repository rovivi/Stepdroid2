package com.kyagamy.step.fragments.songs

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.media.SoundPool
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kyagamy.step.R
import com.kyagamy.step.adapters.LevelAdapter
import com.kyagamy.step.adapters.SongAdapter
import com.kyagamy.step.common.RecyclerItemClickListener
import com.kyagamy.step.common.step.CommonGame.TransformBitmap
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.viewModels.LevelViewModel
import com.kyagamy.step.viewModels.SongViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException

private const val channel = "param1"

class SongsList : Fragment() {
    // TODO: Rename and change types of parameters
    //TEST (you must remove later )





    private var param1: String? = null
    private lateinit var songsModel: SongViewModel
    private lateinit var songsRecyclerView: RecyclerView



    //Old
    var fadeOut: ObjectAnimator? = null
    var spCode = 0
    var spOpenWindow: Int = 0
    var spSelect: Int = 0
    var spSelectSong: Int = 0
    var mediaPlayer: MediaPlayer? = null

    //VideoView bg;
    var changeMusic: SoundPool? = null

    //ThemeElements themeElements;


    private lateinit var preview: VideoView

    var spinner: Spinner? = null

    var errorAuxImage: BitmapDrawable? = null

    var currentSong :Song?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(channel)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        songsModel = ViewModelProvider(this).get(SongViewModel::class.java)

        val songAdapter = SongAdapter(activity!!.applicationContext)


        val view = inflater.inflate(R.layout.fragment_songs_list, container, false)
        songsRecyclerView = view.findViewById(R.id.songs_recyclerView)

        preview = view.findViewById(R.id.videoView3)

        songsRecyclerView.layoutManager = LinearLayoutManager(activity!!.applicationContext)

        songsRecyclerView.adapter = songAdapter
        songsModel.categorySong(param1 ?: "").observe(viewLifecycleOwner, Observer { words ->
//        songsModel.allSong.observe(viewLifecycleOwner, Observer { words ->
            words?.let { songAdapter.setSongs(it) }
        })

        songsRecyclerView.addOnItemTouchListener(
                RecyclerItemClickListener(activity,
                    songsRecyclerView,
                    object : RecyclerItemClickListener.OnItemClickListener {
                        override fun onItemClick(view: View?, position: Int) {
                            lifecycleScope.run {
                                try {
                                    changeSong(songAdapter.getSong(position))
                                    currentSong = songAdapter.getSong(position)
                                     showStartSongFragment(false)
                                }
                                catch (ex:java.lang.Exception){

                                }
                            }
                        }
                        override fun onItemLongClick(
                            view: View?,
                            position: Int
                        ) {
                        }
                    })
                )

        return view
    }


    fun changeSong(song: Song) {
        changeMusic?.play(spCode, 1f, 1f, 1, 0, 1.0f)
        releaseMediaPlayer()
        try {
            val video = File(song.PATH_SONG + "/" + song.PREVIEWVID)
            val bg = File(song.PATH_SONG + "/" + song.BACKGROUND)
            val transparent: Bitmap

            lifecycleScope.launch() {
                playMusicPreview(song)
            }
            if (video.exists() && (video.path.endsWith(".mpg") || video.path
                    .endsWith(".mp4") || video.path.endsWith(".avi"))
            ) {

                preview?.setOnPreparedListener(OnPreparedListener { mediaPlayer: MediaPlayer ->
                    mediaPlayer.isLooping = true
                    mediaPlayer.setVolume(0f, 0f)
                })

                preview?.background = null
                preview?.setVideoPath(video.path)
                preview?.start()
                transparent =
                    TransformBitmap.makeTransparent(BitmapFactory.decodeFile(bg.path), 180)
                this.errorAuxImage = BitmapDrawable(transparent)
            } else {
                if (bg.exists() && bg.isFile) {
                    transparent = TransformBitmap.makeTransparent(
                        BitmapFactory.decodeFile(bg.path),
                        180
                    )
                    this.errorAuxImage = BitmapDrawable(transparent)
                } else {
                    transparent = TransformBitmap.makeTransparent(
                        BitmapFactory.decodeResource(
                            resources,
                            R.drawable.no_banner
                        ), 180
                    )
                    this.errorAuxImage = BitmapDrawable(transparent)
                }
                preview?.background = errorAuxImage
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        releaseMediaPlayer()
        super.onDestroyView()
    }

    private fun releaseMediaPlayer() {
        try {
            while (mediaPlayer != null) {
                //preview?.suspend()
                if (mediaPlayer!!.isPlaying) mediaPlayer!!.stop()
                mediaPlayer!!.release()
                mediaPlayer = null
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    fun showStartSongFragment(type: Boolean) {
        val newFragment = FragmenStartMenu.newInstance(currentSong!!.song_id)
        activity?.supportFragmentManager?.let { newFragment.show(it, "Notice") }
        // playSoundPool(spOpenWindow);
    }

    private suspend fun playMusicPreview(song: Song) {
        val startTime = System.currentTimeMillis()
        releaseMediaPlayer()
        val audio = File(song.PATH_SONG + "/" + song.MUSIC)
        val duration = song.SAMPLELENGTH * 1000 + 3000
        mediaPlayer = MediaPlayer()
        mediaPlayer!!.setVolume(1f, 1f)
        mediaPlayer!!.setDataSource(audio.path)
        mediaPlayer!!.prepare()
        mediaPlayer!!.seekTo(song.SAMPLESTART.toInt() * 1000)
        mediaPlayer!!.start()

        while (mediaPlayer != null) {
            try {
                delay(100)
                val timeLapsed = System.currentTimeMillis() - startTime
                if (timeLapsed >= duration) {
                    releaseMediaPlayer()
                    break
                } else if (timeLapsed >= (duration - 3000)) {
                    val lapset = (100 - ((timeLapsed - (duration - 3000)) / 3000 * 100)) / 100
                    mediaPlayer!!.setVolume(lapset.toFloat(), lapset.toFloat())
                }
            } catch (ex: NullPointerException) {
            }
        }
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SongsList().apply {
                arguments = Bundle().apply {
                    putString(channel, param1)
                }
            }
    }
}
