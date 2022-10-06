package com.kyagamy.step.fragments.songs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kyagamy.step.R
import com.kyagamy.step.common.SettingsGameGetter
import com.kyagamy.step.common.step.CommonGame.ParamsSong
import com.kyagamy.step.databinding.FragmentSongOptionsBinding
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MenuOptionFragment : Fragment() {
    private lateinit var _binding: FragmentSongOptionsBinding

    private val binding get() = _binding

    private var param1: String? = null
    private var param2: String? = null

    var skins: ArrayList<*> = ArrayList<Any>()
    var indexNS = 0
    var autoVelocity: Int = ParamsSong.av
    var speed: Float = ParamsSong.speed

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
        _binding = FragmentSongOptionsBinding.inflate(inflater,container,false)

        //val fView = inflater.inflate(R.layout.fragment_song_options, container, false)
        binding.tv1P .setOnClickListener { changeAutoVelocity(1) }
        binding.tv10P .setOnClickListener { changeAutoVelocity(10) }
        binding.tv100P .setOnClickListener { changeAutoVelocity(100) }
        binding.tv1L .setOnClickListener { changeAutoVelocity(-1) }
        binding.tv10L .setOnClickListener { changeAutoVelocity(-10) }
        binding.tv100L .setOnClickListener { changeAutoVelocity(-100) }

        binding.tvP25.setOnClickListener{changeSpeed(0.25F)}
        binding.tvP5.setOnClickListener{changeSpeed(0.5F)}
        binding.tvP1.setOnClickListener{changeSpeed(1F)}
        binding.tvL25.setOnClickListener{changeSpeed(-0.25F)}
        binding.tvL5.setOnClickListener{changeSpeed(-0.5F)}
        binding.tvL1.setOnClickListener{changeSpeed(-1F)}



        //skins = NoteSkin.arraySkin(context)
        settingsGameGetter= SettingsGameGetter(requireActivity().applicationContext)


        autoVelocity = settingsGameGetter.getValueInt(SettingsGameGetter.AV)
        ParamsSong.av=autoVelocity
        ParamsSong.speed = settingsGameGetter.getValueFloat(SettingsGameGetter.SPEED)

        binding.tvAV.text="".plus(autoVelocity)


        indexNS = ParamsSong.skinIndex


        return binding.root
    }


    private fun changeNS() {
        indexNS++
        indexNS %= skins.size
        ParamsSong.skinIndex = indexNS
        ParamsSong.nameNoteSkin = skins[indexNS].toString()
    }


    private fun changeAutoVelocity(plusValue: Int) {
        autoVelocity += plusValue

        if(autoVelocity<200) autoVelocity= 200
        if(autoVelocity>1200) autoVelocity= 1200
        settingsGameGetter.saveSetting(SettingsGameGetter.AV,autoVelocity )

        ParamsSong.av = autoVelocity
        binding.tvAV.text = "$autoVelocity "
    }

    private fun changeSpeed(plusValue: Float) {
        speed += plusValue
        if(speed<0.25) speed= 0.25f
        if(speed>10) speed= 10f
        settingsGameGetter.saveSetting(SettingsGameGetter.SPEED,speed )

        ParamsSong.speed = speed
        binding.speedValue.text = "$speed"
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
