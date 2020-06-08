package com.kyagamy.step.fragments.songs

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.DialogFragment
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import com.kyagamy.step.R
import java.util.*

class FragmenStartMenu : DialogFragment() {
    //public ArrayList<Level> lista;
    var hexagons =
        arrayOfNulls<ImageView>(3)
    var exit: TextView? = null
    var loading: TextView? = null
    var percent: TextView? = null
    var textPercent = ""
    var startImage: ImageView? = null
    var startImage2: ImageView? = null
    var anim: ValueAnimator? = null
    private var wanrStartSong = false
    var handler = Handler()

    //    public void setSongList(SongList s) {
    //        this.songList = s;
    //    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle
    ): View? {
        //adapterLevel = new AdapterLevel(lista, null, getActivity().getAssets());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Objects.requireNonNull(dialog.window)
                .attributes.windowAnimations = R.style.DialogAnimation
            Objects.requireNonNull(dialog.window)
                .setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        val view =
            inflater.inflate(R.layout.fragment_fragment__start_menu, container, false)
        hexagons[0] = view.findViewById(R.id.iv_hexagon1)
        hexagons[1] = view.findViewById(R.id.iv_hexagon2)
        hexagons[2] = view.findViewById(R.id.iv_hexagon3)
        percent = view.findViewById(R.id.percent_text_fragment)
        loading = view.findViewById(R.id.loading_text_dialog)
        exit = view.findViewById(R.id.tv_damiss)
        exit.setOnClickListener(View.OnClickListener { v: View? -> dismiss() })
        startImage = view.findViewById(R.id.start_image)
        startImage2 = view.findViewById(R.id.start_blour)
        if (!loadingScreen) {
            startImage.setOnClickListener(
                View.OnClickListener { v: View? ->
                    anim!!.start()
                    wanrStartSong = true
                }
            )
        } else {
            startImage2.setVisibility(View.INVISIBLE)
            startImage.setVisibility(View.INVISIBLE)
            exit.setVisibility(View.INVISIBLE)
            //  getDialog().setCancelable(false);
            loading.setVisibility(View.VISIBLE)
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

    override fun onActivityCreated(arg0: Bundle?) {
        super.onActivityCreated(arg0)
        dialog.window
            .getAttributes().windowAnimations = R.style.DialogAnimation
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        hexagons[0]!!.startAnimation(
            AnimationUtils.loadAnimation(
                activity.baseContext,
                R.anim.rotate
            )
        )
        hexagons[1]!!.startAnimation(
            AnimationUtils.loadAnimation(
                activity.baseContext,
                R.anim.rotate2
            )
        )
        hexagons[0]!!.startAnimation(
            AnimationUtils.loadAnimation(
                activity.baseContext,
                R.anim.rotate
            )
        )
        if (!loadingScreen) {
            startImage2!!.startAnimation(
                AnimationUtils.loadAnimation(
                    activity.baseContext,
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
    }
}