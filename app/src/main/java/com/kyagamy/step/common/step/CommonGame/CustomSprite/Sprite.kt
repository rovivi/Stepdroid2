package com.kyagamy.step.Common.step.CommonGame.CustomSprite

import android.content.Context
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Author Rodrigo Vidal
 */
class Sprite : SurfaceView, SurfaceHolder.Callback {
    var image: SpriteReader? = null
    var staticDraw = false

    constructor(context: Context?) : super(context) {
        holder.setFormat(PixelFormat.TRANSPARENT)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        holder.setFormat(PixelFormat.TRANSPARENT)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        setWillNotDraw(false)
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        pause()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        //destroy stuff
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        //if (getHolde)
        if (staticDraw) {
            image!!.staticDraw(canvas, Rect(0, 0, width, height))
        } else {
            image!!.draw(canvas, Rect(0, 0, width, height))
        }
    }

    fun update() {
        image!!.update()
    }

    fun pause() {}

    /*public void play() {
        if (image!=null){
            threadSprite.running=true;
            threadSprite.start();
        }
    }*/
    fun create(sprite: SpriteReader) {
        image = sprite
        sprite.play()
    }
}