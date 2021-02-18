package com.kyagamy.step

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.kyagamy.step.common.step.CommonGame.ArrowsPositionPlace
import kotlinx.android.synthetic.main.activity_drag_step.*


class DragStepActivity : FullScreenActivity() {
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
        val sharedPref = this.getSharedPreferences(
            getString(R.string.singleArrowsPos), Context.MODE_PRIVATE
        )
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_drag_step)




        sizeBar.max = 200
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
        val gson = Gson()
        saveArrows.setOnClickListener {
            val save = ArrowsPositionPlace()
            save.size = sizeBar.progress + 50+20
            var positions = ArrayList<Point>()
            arrows.forEach { x ->
                positions.add(Point(x.x.toInt(), x.y.toInt()))
            }
            save.positions = positions.toList().toTypedArray()

            with(sharedPref.edit()) {
                putString(getString(R.string.singleArrowsPos), gson.toJson(save))
                apply()
            }
            Toast.makeText(this, "Saved success!", Toast.LENGTH_SHORT).show()
        }

        drawArrows(false)
        val saveGson = sharedPref.getString(getString(R.string.singleArrowsPos), "")
        if (saveGson != "") {
            val obj: ArrowsPositionPlace = gson.fromJson(saveGson, ArrowsPositionPlace::class.java)
            sizeBar.progress = obj.size - 50
            obj.positions.forEachIndexed { index, pos ->
                arrows[index].x = pos.x.toFloat()
                arrows[index].y = pos.y.toFloat()
            }
        } else {
            val params = root.layoutParams
            val sizeX = params.width
            val size = (sizeX / 3).toInt()
            val sizeY = params.height
            //set 1
            arrows[0].y = (sizeY - size).toFloat()
            arrows[0].x = 0f
            //set 7
            arrows[1].y = (sizeY/2).toFloat()
            arrows[1].x = 0f

            //set 5

            arrows[2].y = ((sizeY/2-size)/2+sizeY/2).toFloat()
            arrows[2].x = ((sizeX-size)/2).toFloat()

            //set 9
            arrows[3].y = (sizeY/2).toFloat()
            arrows[3].x = ((sizeX-size).toFloat())
            //set 3
            arrows[4].y =(sizeY - size).toFloat()
            arrows[4].x = ((sizeX-size).toFloat())
            //resizeArrows(pxToDp(size))
            sizeBar.progress =pxToDp(size)-50
        }

    }

    fun pxToDp(px: Int): Int {
        return (px / Resources.getSystem().displayMetrics.density).toInt()
    }


    private fun drawArrows(isDouble: Boolean) {

        stepInfo.forEach { x ->
            var iv = ImageView(this)
            iv.setImageResource(x)
            iv.setOnTouchListener(move)
            arrows.add(iv)
            root.addView(iv)

        }
        if (isDouble) drawArrows(false)
    }

    private fun resizeArrows(value: Int) {
        val pixel = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value + 0f, resources.displayMetrics
        ).toInt()

        arrows.forEach { arrow ->
            val lp = arrow.layoutParams as RelativeLayout.LayoutParams
            lp.width = pixel
            lp.height = pixel
            arrow.layoutParams = lp
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    val move = View.OnTouchListener { v, event ->
        val X = event.rawX.toInt()
        val Y = event.rawY.toInt()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                _xDelta = X - v.left
                _yDelta = Y - v.top
            }
            MotionEvent.ACTION_MOVE -> {
                v.x = X - _xDelta + 0f
                v.y = Y - _yDelta + 0f
//                val lp = v.layoutParams as RelativeLayout.LayoutParams
//                lp.leftMargin = X - _xDelta
//                lp.topMargin = Y - _yDelta
//                lp.rightMargin = v.width - lp.leftMargin - windowWidth
//                lp.bottomMargin = v.height - lp.topMargin - windowHeight
//                v.layoutParams = lp
                sizeText.text = "${X - _xDelta + 0f} ,${Y - _yDelta + 0f}"
            }
        }
        true
    }
}
