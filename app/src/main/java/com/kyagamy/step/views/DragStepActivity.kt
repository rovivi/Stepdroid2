package com.kyagamy.step.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Bundle
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

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDragStepBinding.inflate(layoutInflater)
        setContentView(binding.root)
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

                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    val value = 50 + i;
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
        val params = binding.relativeLayoutToDrag.layoutParams
        val sizeX = params.width
        val size = (sizeX / 3).toInt()
        val sizeY = params.height
        //set 1
        arrows[0].y = (sizeY - size).toFloat()
        arrows[0].x = 0f
        //set 7
        arrows[1].y = (sizeY / 2).toFloat()
        arrows[1].x = 0f
        //set 5
        arrows[2].y = ((sizeY / 2 - size) / 2 + sizeY / 2).toFloat()
        arrows[2].x = ((sizeX - size) / 2).toFloat()
        //set 9
        arrows[3].y = (sizeY / 2).toFloat()
        arrows[3].x = ((sizeX - size).toFloat())
        //set 3
        arrows[4].y = (sizeY - size).toFloat()
        arrows[4].x = ((sizeX - size).toFloat())
        //resizeArrows(pxToDp(size))
        binding.sizeBar.progress = pxToDp(size) - 50

    }

    private fun pxToDp(px: Int): Int {
        return (px / Resources.getSystem().displayMetrics.density).toInt()
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun drawArrows(isDouble: Boolean) {

        stepInfo.forEach { x ->

            val iv = ImageView(this)

            val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
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
            lp.width = pixel
            lp.height = pixel
            arrow.layoutParams = lp
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    val move = View.OnTouchListener { view, event ->
        val x = event.rawX.toInt()
        val y = event.rawY.toInt()


        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                _xDelta = x - view.left
                _yDelta = y - view.top
                Log.d("owo", "action DOWN-- xDelta: ${_xDelta}, yDelta: ${_yDelta}")
            }

            MotionEvent.ACTION_MOVE -> {
                val valueX = x - _xDelta + _xDelta
                val valueY = y - _yDelta

                view.x = valueX.toFloat()
                view.y = valueY.toFloat()
//                val lp = v.layoutParams as RelativeLayout.LayoutParams
//                lp.leftMargin = X - _xDelta
//                lp.topMargin = Y - _yDelta
//                lp.rightMargin = v.width - lp.leftMargin - windowWidth
//                lp.bottomMargin = v.height - lp.topMargin - windowHeight
//                v.layoutParams = lp
                binding.sizeText.text = "${x - _xDelta + 0f} ,${y - _yDelta + 0f}"

                Log.d("owo", "action MOVE-- xDelta: ${valueX}, yDelta: ${valueY}")

            }
        }
        true
    }
}
