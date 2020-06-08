package com.kyagamy.step

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import com.kyagamy.step.common.step.CommonGame.ParamsSong
import com.kyagamy.step.game.newplayer.NoteSkin
import java.util.*


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class menu_option_fragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var msj2: TextView
    private lateinit var msj: TextView
    private lateinit var pp1: ImageView
    private lateinit var pp0_5: ImageView
    private lateinit var ll1: ImageView
    private lateinit var ll0_5: ImageView
    private lateinit var noteButton: ImageView
    private lateinit var noteImage: ImageView
    private lateinit var tvRush: TextView
    private lateinit var tvJudge: TextView
    private lateinit var tvFD: TextView
    private lateinit var switchAutoplay: Switch
    private lateinit var avBar: SeekBar
    var skins: ArrayList<*> = ArrayList<Any>()
    var indexNS = 0
    var velocity: Float = ParamsSong.speed

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
        // Inflate the layout for this fragment
        val fView = inflater.inflate(R.layout.fragment_song_options, container, false)

        val view: View =
            inflater.inflate(R.layout.fragment_song_options, container, false)
        msj = view.findViewById(R.id.tv_msj_level_modal)
        msj2 = view.findViewById(R.id.tv_msj_level_modal2)

        pp1 = view.findViewById(R.id.iv1pp)
        pp0_5 = view.findViewById(R.id.iv0_5pp)
        ll1 = view.findViewById(R.id.iv1ll)
        ll0_5 = view.findViewById(R.id.iv0_5ll)
        noteButton = view.findViewById(R.id.iv2)
        noteImage = view.findViewById(R.id.image_skin)
        switchAutoplay = view.findViewById(R.id.switch_autoplay)
        avBar = view.findViewById(R.id.seekBar_av)
        tvFD = view.findViewById(R.id.tv_fd)

        tvRush = view.findViewById(R.id.tvrush)
        tvJudge = view.findViewById(R.id.tv_judge)

        ll1 = view.findViewById(R.id.iv1ll)

        val title3 = view.findViewById<TextView>(R.id.tv_msj_level_modal3)
        val tv5 = view.findViewById<TextView>(R.id.title_apear)
        val customFont = Typeface.createFromAsset(activity!!.assets, "fonts/font.ttf")

        msj.typeface = customFont
        msj2.typeface = customFont
        title3.typeface = customFont
        tv5.typeface = customFont
        tvRush.typeface = customFont
        tvRush.typeface = customFont
        tvJudge.typeface = customFont
        switchAutoplay.typeface = customFont

        tvRush.setOnClickListener {
            ParamsSong.rush += 0.1f
            if (ParamsSong.rush > 1.5f)
                ParamsSong.rush = 0.8f
            setTxtRush()
        }
        switchAutoplay.isChecked = ParamsSong.autoplay
        switchAutoplay.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            ParamsSong.autoplay = isChecked
        }
        tvJudge.setOnClickListener {
            ParamsSong.judgment = (ParamsSong.judgment + 1) % 7
            setTxtJudge()
        }

        setTxtJudge()
        setTxtRush()

        pp1.setOnClickListener {changeVelocity(1f)}
        pp0_5.setOnClickListener {changeVelocity(0.5f)}
        ll1.setOnClickListener {changeVelocity(-1f)}
        ll0_5.setOnClickListener {changeVelocity(-0.5f)
        }
        tvFD.setOnClickListener {
            ParamsSong.FD = !ParamsSong.FD
            setFreedom()
        }

        skins = NoteSkin.arraySkin(context)
        indexNS = ParamsSong.skinIndex
        setImageNS()

        avBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                val av = progress * 10
                ParamsSong.av = av
                if (av == 0) {
                    msj.text = "AV OFF"
                    msj2.text = "Velocity  " + ParamsSong.speed
                } else
                    setAv()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        noteButton.setOnClickListener { changeNS() }
        noteImage.setOnClickListener { changeNS() }

        if (ParamsSong.av > 0) {
            avBar.progress = ParamsSong.av / 10
            setAv()
        } else
            changeVelocity(0.0f)
        setFreedom()
        return fView
    }

    private fun setFreedom() {
        if (ParamsSong.FD)
            tvFD.setTextColor(Color.rgb(255, 255, 255))
        else
            tvFD.setTextColor(Color.rgb(120, 120, 120))
    }

    private fun changeNS() {
        indexNS++
        indexNS %= skins.size
        ParamsSong.skinIndex = indexNS
        ParamsSong.nameNoteSkin = skins[indexNS].toString()
        setImageNS()
    }

    private fun setImageNS() {
        noteImage.setImageBitmap(NoteSkin.maskImage(ParamsSong.nameNoteSkin, context))

    }

    private fun changeVelocity(plusValue:Float) {
        velocity+=plusValue
        avBar.progress = 0
        ParamsSong.av = 0
        msj.text = "AV OFF"
        ParamsSong.speed = velocity
        msj2.text = "Velocity  $velocity"
    }

    private fun setAv() {
        msj.text = "AV " + ParamsSong.av
        msj2.text = "Velocity OFF"
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
        tvJudge.text = text
    }

    private fun setTxtRush() {
        val x = (ParamsSong.rush * 100).toInt()
        tvRush.text = x.toString()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment menu_option_fragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            menu_option_fragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
