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


class DragStepActivity : AppCompatActivity() {
    var windowHeight: Int = 0
    var windowWidth: Int = 0
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
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_drag_step)

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)


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
            save.size = sizeBar.progress + 50
            var positions = ArrayList<Point>()
            arrows.forEach { x ->
                val lp = x.layoutParams as RelativeLayout.LayoutParams
                positions.add(Point(lp.leftMargin, lp.topMargin))
            }
            save.positions = positions.toList().toTypedArray()

            with(sharedPref.edit()) {
                putString(getString(R.string.singleArrowsPos), gson.toJson(save))
                apply()
            }
            Toast.makeText(this,"Saved success!",Toast.LENGTH_SHORT).show()
        }

        drawArrows(false)
        val saveGson = sharedPref.getString(getString(R.string.singleArrowsPos), "")
        if (saveGson!=""){
            val obj: ArrowsPositionPlace = gson.fromJson(saveGson, ArrowsPositionPlace::class.java)
            sizeBar.progress= obj.size-50
            var count = 0
            obj.positions.forEach { pos->
                val lp = arrows.get(count).layoutParams as RelativeLayout.LayoutParams
                lp.leftMargin = pos.x
                lp.topMargin = pos.y
                lp.rightMargin =arrows.get(count).width - lp.leftMargin
                lp.bottomMargin = arrows.get(count).height - lp.topMargin
                arrows.get(count).layoutParams = lp
                sizeText.text = "${lp.leftMargin} ,${lp.topMargin}"
                count++
             }
        }
        else {
            val params= root.layoutParams
            val size =(0.999f*params.height/6).toInt()
            val sizeX = params.width
            val sizeY =  params.height
            //set 1
            val  s1lp =  arrows[0].layoutParams as RelativeLayout.LayoutParams
            s1lp.topMargin =sizeY-size
            s1lp.rightMargin = sizeY-size
            arrows[0].layoutParams = s1lp

            //set 7
            val  s2lp =  arrows[1].layoutParams as RelativeLayout.LayoutParams
            s2lp.bottomMargin =2*size
            s2lp.topMargin =3*size
            s2lp.rightMargin =2*size
            arrows[1].layoutParams = s2lp

            //set 5
            val  s3lp =  arrows[2].layoutParams as RelativeLayout.LayoutParams
            s3lp.bottomMargin =1*size
            s3lp.topMargin =4*size
            s3lp.leftMargin =1*size
            s3lp.rightMargin =1*size
            arrows[2].layoutParams = s3lp
            //set 9
            val  s4lp =  arrows[3].layoutParams as RelativeLayout.LayoutParams
            s4lp.bottomMargin =2*size
            s4lp.topMargin =3*size
            s4lp.leftMargin =2*size
            arrows[3].layoutParams = s4lp

            //set 3
            val  s5lp =  arrows[4].layoutParams as RelativeLayout.LayoutParams
            s5lp.topMargin =5*size
            s5lp.leftMargin =2*size
            arrows[4].layoutParams = s5lp
            sizeBar.progress = pxToDp(size)
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
            val lp = iv.layoutParams as RelativeLayout.LayoutParams
            iv.layoutParams = lp
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
                val lp = v.layoutParams as RelativeLayout.LayoutParams
                lp.leftMargin = X - _xDelta
                lp.topMargin = Y - _yDelta
                lp.rightMargin = v.width - lp.leftMargin - windowWidth
                lp.bottomMargin = v.height - lp.topMargin - windowHeight
                v.layoutParams = lp
                sizeText.text = "${lp.leftMargin} ,${lp.topMargin}"
            }
        }
        true
    }
}
