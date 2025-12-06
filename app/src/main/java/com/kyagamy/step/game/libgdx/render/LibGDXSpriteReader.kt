package com.kyagamy.step.game.libgdx.render

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array

class LibGDXSpriteReader {
    private var animation: Animation<TextureRegion>
    private var stateTime = 0f

    constructor(texture: Texture, cols: Int, rows: Int, frameDuration: Float) {
        val tmp = TextureRegion.split(
            texture,
            texture.width / cols,
            texture.height / rows
        )

        val frames = Array<TextureRegion>(cols * rows)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                frames.add(tmp[i][j])
            }
        }
        animation = Animation(frameDuration, frames)
        animation.playMode = Animation.PlayMode.LOOP
    }

    constructor(texture: Texture) : this(texture, 1, 1, 0f)

    constructor(frames: kotlin.Array<TextureRegion>, frameDuration: Float) {
        val gdxFrames = Array<TextureRegion>()
        frames.forEach { gdxFrames.add(it) }
        animation = Animation(frameDuration, gdxFrames)
        animation.playMode = Animation.PlayMode.LOOP
    }

    fun update(delta: Float) {
        stateTime += delta
    }

    fun draw(batch: SpriteBatch, x: Float, y: Float, width: Float, height: Float) {
        val currentFrame = animation.getKeyFrame(stateTime, true)
        batch.draw(currentFrame, x, y, width, height)
    }

    fun getKeyFrame(): TextureRegion {
        return animation.getKeyFrame(stateTime, true)
    }
    
    fun getFrame(index: Int): TextureRegion {
        return animation.keyFrames[index % animation.keyFrames.size]
    }
}
