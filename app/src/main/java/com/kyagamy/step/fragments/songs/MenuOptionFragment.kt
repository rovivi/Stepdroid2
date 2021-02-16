package com.kyagamy.step.fragments.songs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kyagamy.step.R
import com.kyagamy.step.common.SettingsGameGetter
import com.kyagamy.step.common.step.CommonGame.ParamsSong
import com.kyagamy.step.game.newplayer.NoteSkin
import kotlinx.android.synthetic.main.fragment_song_options.view.*
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MenuOptionFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null


    var skins: ArrayList<*> = ArrayList<Any>()
    var indexNS = 0
    var velocity: Float = ParamsSong.speed
    public lateinit var  infoAV :TextView
    lateinit  var settingsGameGetter:SettingsGameGetter





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        val fView = inflater.inflate(R.layout.fragment_song_options, container, false)
        infoAV  =fView.findViewById(R.id.tvAV)

        fView.tv_1P .setOnClickListener { changeVelocity(1) }
        fView.tv_10P .setOnClickListener { changeVelocity(10) }
        fView.tv_100P .setOnClickListener { changeVelocity(100) }
        fView.tv_1L .setOnClickListener { changeVelocity(-1) }
        fView.tv_10L .setOnClickListener { changeVelocity(-10) }
        fView.tv_100L .setOnClickListener { changeVelocity(-100) }

        //skins = NoteSkin.arraySkin(context)
        settingsGameGetter= SettingsGameGetter(requireActivity().applicationContext)
        indexNS = ParamsSong.skinIndex


        return fView
    }


    private fun changeNS() {
        indexNS++
        indexNS %= skins.size
        ParamsSong.skinIndex = indexNS
        ParamsSong.nameNoteSkin = skins[indexNS].toString()

    }



    private fun changeVelocity(plusValue: Int) {
        velocity += plusValue
        settingsGameGetter.saveSetting(SettingsGameGetter.AV,velocity )
        ParamsSong.av = 0
        ParamsSong.speed = velocity
        infoAV.text = "AV: $velocity"
    }


    private fun setTxtJudge() {
        var text = ""
        when (ParamsSong.judgment) {
            0 -> text = "SJ"
            1 -> text = "EJ"
            2 -> text = "NJ"
            3 -> text = "HJ"
            4 -> text = "VJ"
            5 -> text = "XJ"
            6 -> text = "UJ"
        }
        //tvJudge.text = text
    }

    private fun setTxtRush() {
        val x = (ParamsSong.rush * 100).toInt()
        //tvRush.text = x.toString()
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MenuOptionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
