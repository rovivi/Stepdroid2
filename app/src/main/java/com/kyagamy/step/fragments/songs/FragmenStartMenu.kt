package com.kyagamy.step.fragments.songs


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kyagamy.step.PlayerBga
import com.kyagamy.step.R
import com.kyagamy.step.adapters.LevelAdapter
import com.kyagamy.step.common.RecyclerItemClickListener
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.viewModels.LevelViewModel
import com.kyagamy.step.viewModels.SongViewModel
import kotlinx.android.synthetic.main.fragment_fragment__start_menu.view.*
import java.util.*

private const val songId = "song"


class FragmenStartMenu : DialogFragment() {

    private var idSong: Int = 0


    //public ArrayList<Level> lista;
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
        //adapterLevel = new AdapterLevel(lista, null, getActivity().getAssets());
        Objects.requireNonNull(dialog?.window)
            ?.attributes!!.windowAnimations = R.style.DialogAnimation
        Objects.requireNonNull(dialog?.window)
            ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val view =
            inflater.inflate(R.layout.fragment_fragment__start_menu, container, false)
        hexagons[0] = view.findViewById(R.id.iv_hexagon1)
        hexagons[1] = view.findViewById(R.id.iv_hexagon2)
        //hexagons[2] = view.findViewById(R.id.iv_hexagon3)
        //percent = view.findViewById(R.id.percent_text_fragment)
        loading = view.findViewById(R.id.loading_text_dialog)
        exit = view.findViewById(R.id.tv_damiss)



        exit.setOnClickListener(View.OnClickListener { v: View? -> dismiss() })
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
            //  getDialog().setCancelable(false);
            loading.visibility = View.VISIBLE
            //    percent.setVisibility(View.VISIBLE);
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
            transaction!!.add(R.id.frame_options, fragmentCategory)
            transaction.addToBackStack(null)
            transaction.commit()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        //songs

        songsModel = ViewModelProvider(this).get(SongViewModel::class.java)
        levelModel = ViewModelProvider(this).get(LevelViewModel::class.java)

        i = Intent(requireActivity(), PlayerBga::class.java)

        levelModel = ViewModelProvider(this).get(LevelViewModel::class.java)

        val levelAdapter = LevelAdapter(requireActivity().applicationContext)

        levelRecyclerView = view.findViewById(R.id.recycler_levels)
        levelRecyclerView.layoutManager = LinearLayoutManager(
            requireActivity().applicationContext,
            LinearLayoutManager.HORIZONTAL,
            false
        )
//        levelRecyclerView.layoutManager = LinearLayoutManager(activity!!.applicationContext)
        levelRecyclerView.adapter = levelAdapter

        var currentSong: Song? = null

        songsModel.songById(idSong)
            .observe(viewLifecycleOwner, androidx.lifecycle.Observer { words ->
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
                                //finish();
                                //finish();


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
            .observe(viewLifecycleOwner, androidx.lifecycle.Observer { level ->
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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        //
//        if (songList!=null){
//            songList.playSoundPool(songList.spSelectSong);
//
//        }
    }


    override fun onStart() {
        super.onStart()
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
            startImage2!!.startAnimation(
                AnimationUtils.loadAnimation(
                    activity?.baseContext,
                    R.anim.fade_half
                )
            )
        }
    }


    override fun onDetach() {
//        if (wanrStartSong) {
//            if (songList!=null){
//            songList.startSong();
//            }
//        }
        super.onDetach()
    }


    companion object {
        var loadingScreen = false

        @JvmStatic
        fun newInstance(idSong: Int) =
            FragmenStartMenu().apply {
                arguments = Bundle().apply {
                    putInt(songId, idSong)

                }
            }
    }


}