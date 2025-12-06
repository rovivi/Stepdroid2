package com.kyagamy.step.game.interfaces

import com.kyagamy.step.common.step.CommonGame.CustomSprite.SpriteReader

interface IStepsDrawer {
    // We need methods to trigger effects. 
    // In GameState: stepsDrawer?.selectedSkin?.explotions?.get(arrowIndex)?.play()
    // This implies exposing the skin or having a method to play explosion.
    // Exposing skin is tied to implementation details (SpriteReader).
    // Better to have abstract methods.
    
    fun playExplosion(index: Int)
    fun playExplosionTail(index: Int)
    fun stopExplosionTail(index: Int)
}
