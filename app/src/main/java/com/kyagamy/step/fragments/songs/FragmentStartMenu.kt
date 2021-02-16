package com.kyagamy.step.fragments.songs


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kyagamy.step.PlayerBga
import com.kyagamy.step.R
import com.kyagamy.step.adapters.LevelAdapter
import com.kyagamy.step.common.RecyclerItemClickListener
import com.kyagamy.step.common.step.CommonGame.TransformBitmap
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.viewModels.LevelViewModel
import com.kyagamy.step.viewModels.SongViewModel
import kotlinx.android.synthetic.main.fragment_fragment__start_menu.view.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.util.*

private const val songId = "song"

class FragmentStartMenu : DialogFragment() {
    private var idSong: Int = 0
    private var hexagons =
        arrayOfNulls<ImageView>(2)
    private lateinit var exit: TextView
    private lateinit var loading: TextView
    private lateinit var percent: TextView
    private lateinit var startImage: ImageView
    private lateinit var startImage2: ImageView
    var anim: ValueAnimator? = null
    private var warningStartSong = false

    //
    private lateinit var levelModel: LevelViewModel
    private lateinit var songsModel: SongViewModel

    private lateinit var levelRecyclerView: RecyclerView
    var i: Intent? = null

    var currentSong: Song? = null
    lateinit var preview: VideoView

    var errorAuxImage: BitmapDrawable? = null
    var changeMusic: SoundPool? = null
    var mediaPlayer: MediaPlayer? = MediaPlayer()
    var spCode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            idSong = it.getInt(songId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Objects.requireNonNull(dialog?.window)
            ?.attributes!!.windowAnimations = R.style.DialogAnimation
        Objects.requireNonNull(dialog?.window)
            ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val view =
            inflater.inflate(R.layout.fragment_fragment__start_menu, container, false)
        hexagons[0] = view.findViewById(R.id.iv_hexagon1)
        hexagons[1] = view.findViewById(R.id.iv_hexagon2)
        preview = view.findViewById(R.id.videoPreview)

        loading = view.findViewById(R.id.loading_text_dialog)
        exit = view.findViewById(R.id.tv_damiss)

        exit.setOnClickListener { dismiss() }
        startImage = view.findViewById(R.id.start_image)
        startImage2 = view.findViewById(R.id.start_blour)
        if (!loadingScreen) {
            startImage.setOnClickListener { v: View? ->
                anim!!.start()
                warningStartSong = true
            }
        } else {
            startImage2.visibility = View.INVISIBLE
            startImage.visibility = View.INVISIBLE
            exit.visibility = View.INVISIBLE
            loading.visibility = View.VISIBLE
        }
        val from = Color.argb(100, 0, 0, 0)
        val to = Color.argb(100, 255, 255, 255)
        anim = ValueAnimator()
        anim!!.setIntValues(from, to)
        anim!!.setEvaluator(ArgbEvaluator())
        anim!!.addUpdateListener { valueAnimator: ValueAnimator ->
            view.setBackgroundColor(
                (valueAnimator.animatedValue as Int)
            )
        }
        anim!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                dismiss()
            }
        })
        anim!!.duration = 250

        //setSettingsFragment
        try {
            val transaction = childFragmentManager.beginTransaction()
            val fragmentCategory = MenuOptionFragment()
            transaction.add(R.id.frame_options, fragmentCategory)
            transaction.commit()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        //songs

        songsModel = ViewModelProvider(this).get(SongViewModel::class.java)
        levelModel = ViewModelProvider(this).get(LevelViewModel::class.java)


        i = Intent(requireActivity(), PlayerBga::class.java)


        val levelAdapter = LevelAdapter(requireActivity().applicationContext)

        levelRecyclerView = view.findViewById(R.id.recycler_levels)
        levelRecyclerView.layoutManager = LinearLayoutManager(
            requireActivity().applicationContext,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        levelRecyclerView.adapter = levelAdapter
        songsModel.songById(idSong)
            .observe(viewLifecycleOwner, { words ->
                words?.let {
                    currentSong = it[0]
                }
            })
        levelRecyclerView.addOnItemTouchListener(
            RecyclerItemClickListener(activity,
                levelRecyclerView,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View?, position: Int) {
                        lifecycleScope.run {
                            try {
                                // root.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.fade_out));

                                i!!.putExtra("ssc", currentSong?.PATH_File)
                                i!!.putExtra("nchar", levelAdapter.getLevel(position).index)
                                i!!.putExtra("path", currentSong?.PATH_SONG)
                                i!!.putExtra(
                                    "pathDisc",
                                    currentSong?.PATH_SONG + currentSong?.BANNER_SONG
                                )
                                startActivity(i)
                            } catch (ex: Exception) {
                                ex.printStackTrace()
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

        levelModel.get(idSong)
            .observe(viewLifecycleOwner, { level ->
                level?.let { levelAdapter.setLevels(it) }
            })

        //animaciones
        view.image_arrow_l.startAnimation(
            AnimationUtils.loadAnimation(
                context,
                R.anim.translate_left
            )
        )
        view.image_arrow_r.startAnimation(
            AnimationUtils.loadAnimation(
                context,
                R.anim.translate_right
            )
        )


        return view
    }


    override fun onDestroyView() {
        releaseMediaPlayer()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        hexagons[0]!!.startAnimation(
            AnimationUtils.loadAnimation(
                activity?.baseContext,
                R.anim.rotate
            )
        )
        hexagons[1]!!.startAnimation(
            AnimationUtils.loadAnimation(
                activity?.baseContext,
                R.anim.rotate2
            )
        )
        hexagons[0]!!.startAnimation(
            AnimationUtils.loadAnimation(
                activity?.baseContext,
                R.anim.rotate
            )
        )
        if (!loadingScreen) {
            startImage2.startAnimation(
                AnimationUtils.loadAnimation(
                    activity?.baseContext,
                    R.anim.fade_half
                )
            )
        }
        songsModel.songById(idSong)
            .observe(viewLifecycleOwner, { words ->
                words?.let {
                    changeSong( it[0])
                }
            })
    }


    private fun changeSong(song: Song?) {
        if (song == null) return
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

                preview.setOnPreparedListener { mediaPlayer: MediaPlayer ->
                    mediaPlayer.isLooping = true
                    mediaPlayer.setVolume(0f, 0f)
                }

                preview.background = null
                preview.setVideoPath(video.path)
                preview.start()
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
                preview.background = errorAuxImage
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        var loadingScreen = false

        @JvmStatic
        fun newInstance(idSong: Int) =
            FragmentStartMenu().apply {
                arguments = Bundle().apply {
                    putInt(songId, idSong)
                }
            }
    }
}