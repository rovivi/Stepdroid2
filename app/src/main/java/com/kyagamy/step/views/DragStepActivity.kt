package com.kyagamy.step.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import com.google.gson.Gson
import com.kyagamy.step.R
import com.kyagamy.step.common.step.CommonGame.ArrowsPositionPlace
import com.kyagamy.step.databinding.ActivityDragStepBinding


class DragStepActivity : FullScreenActivity() {

    private lateinit var binding: ActivityDragStepBinding

    private var _xDelta = 0
    private var _yDelta = 0
    private var stepInfo: List<Int> = listOf(
        R.drawable.selector_down_left,
        R.drawable.selector_up_left,
        R.drawable.selector_center,
        R.drawable.selector_up_right,
        R.drawable.selector_down_right
    )
    private var arrows: ArrayList<ImageView> = ArrayList()

    enum class Preset {
        PUMP,
        GUITAR,
        PAD
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDragStepBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //initUI()
    }

    override fun onStart() {
        super.onStart()
        initUI()
    }

    private fun initUI() {
        val sharedPref = this.getSharedPreferences(
            getString(R.string.singleArrowsPos), Context.MODE_PRIVATE
        )
        val gson = Gson()

        with(binding) {
            sizeBar.max = 230
            sizeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(seekBar: SeekBar, value: Int, b: Boolean) {
                    //val value = 50 + i;
                    sizeText.text = "Progress : $value dp"
                    resizeArrows(value)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })
            saveArrows.setOnClickListener {
                val save = ArrowsPositionPlace()
                save.size = sizeBar.progress + 50 + 20
                val positions = ArrayList<Point>()
                arrows.forEach { x ->
                    positions.add(Point(x.x.toInt(), x.y.toInt()))
                }
                save.positions = positions.toList().toTypedArray()

                with(sharedPref.edit()) {
                    putString(
                        getString(R.string.singleArrowsPos),
                        gson.toJson(save)
                    )
                    apply()
                }
                Toast.makeText(this@DragStepActivity, "Saved success!", Toast.LENGTH_SHORT).show()
            }

            buttonReset.setOnClickListener { resetPanel() }

            buttonPresetPump.setOnClickListener { applyPreset(Preset.PUMP) }
            buttonPresetGuitar.setOnClickListener { applyPreset(Preset.GUITAR) }
            buttonPresetPad.setOnClickListener { applyPreset(Preset.PAD) }

            drawArrows(false)
            val saveGson = sharedPref.getString(getString(R.string.singleArrowsPos), "")
            if (saveGson != "") {
                val obj: ArrowsPositionPlace =
                    gson.fromJson(saveGson, ArrowsPositionPlace::class.java)
                sizeBar.progress = obj.size - 50
                obj.positions.forEachIndexed { index, pos ->
                    arrows[index].x = pos.x.toFloat()
                    arrows[index].y = pos.y.toFloat()
                }
            } else {
                resetPanel()
            }
        }
    }

    private fun resetPanel() {
        applyPreset(Preset.PUMP)
    }

    private fun pxToDp(px: Int): Int {
        return (px / Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun applyPreset(preset: Preset) {
        val layout = binding.relativeLayoutToDrag
        val width = layout.width
        val height = layout.height
        if (width == 0 || height == 0) {
            layout.post { applyPreset(preset) }
            return
        }
        when (preset) {
            Preset.PUMP -> {
                val sizeSquare = width / 3f
                arrows[0].x = 0f
                arrows[0].y = height - sizeSquare

                arrows[1].x = 0f
                arrows[1].y = height / 2f

                arrows[2].x = (width - sizeSquare) / 2f
                arrows[2].y = (height / 2f - sizeSquare) / 2f + height / 2f

                arrows[3].x = width - sizeSquare
                arrows[3].y = height / 2f

                arrows[4].x = width - sizeSquare
                arrows[4].y = height - sizeSquare

                binding.sizeBar.progress = pxToDp((sizeSquare * 2).toInt())
            }

            Preset.GUITAR -> {
                val size = width / 6f
                val startX = (width - size * 5) / 2f
                val posY = height - size * 1.2f
                for (i in 0 until 5) {
                    arrows[i].x = startX + i * size
                    arrows[i].y = posY
                }
                binding.sizeBar.progress = pxToDp(size.toInt())
            }

            Preset.PAD -> {
                val sizeSquare = width / 3f
                arrows[0].x = 0f
                arrows[0].y = height - sizeSquare

                arrows[1].x = 0f
                arrows[1].y = 0f

                arrows[2].x = (width - sizeSquare) / 2f
                arrows[2].y = (height - sizeSquare) / 2f

                arrows[3].x = width - sizeSquare
                arrows[3].y = 0f

                arrows[4].x = width - sizeSquare
                arrows[4].y = height - sizeSquare

                binding.sizeBar.progress = pxToDp((sizeSquare * 2).toInt())
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun drawArrows(isDouble: Boolean) {

        stepInfo.forEach { x ->

            val iv = ImageView(this)

// Puedes definir la posición inicial aquí si lo deseas, por ejemplo:
// params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE)
            //iv.layoutParams = params
            iv.setImageResource(x)
            iv.setOnTouchListener(move)
            arrows.add(iv)
            binding.relativeLayoutToDrag.addView(iv)

        }
        //drawArrows(false)
    }


    private fun resizeArrows(value: Int) {
        val pixel = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(), resources.displayMetrics
        ).toInt()

        arrows.forEach { arrow ->
            val lp = arrow.layoutParams as RelativeLayout.LayoutParams
            lp.width = value
            lp.height = value
            arrow.layoutParams = lp
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    val move = View.OnTouchListener { view, event ->
        val x = event.rawX.toInt()
        val y = event.rawY.toInt()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                _xDelta = (event.rawX - view.x).toInt()
                _yDelta = (event.rawY - view.y).toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                val newX = event.rawX - _xDelta
                val newY = event.rawY - _yDelta
                val maxX = binding.relativeLayoutToDrag.width - view.width
                val maxY = binding.relativeLayoutToDrag.height - view.height
                view.x = newX.coerceIn(0f, maxX.toFloat())
                view.y = newY.coerceIn(0f, maxY.toFloat())
            }
        }
        true
    }
}
