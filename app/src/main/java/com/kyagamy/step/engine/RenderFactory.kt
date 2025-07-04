package com.kyagamy.step.engine

import android.content.Context

fun createRenderer(context: Context, useGL: Boolean): SpriteView {
    return if (useGL) {
        OpenGLSpriteView(context)
    } else {
        CanvasSpriteView(context)
    }
}
