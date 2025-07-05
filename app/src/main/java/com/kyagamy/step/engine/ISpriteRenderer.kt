package com.kyagamy.step.engine

import android.graphics.Rect

/**
 * UV coordinates for texture sampling
 */
data class UVCoords(
    val u0: Float = 0f,
    val v0: Float = 0f,
    val u1: Float = 1f,
    val v1: Float = 1f
)

interface ISpriteBatch {
    /**
     * Begin batch drawing
     */
    fun begin()

    /**
     * Encola un sprite para dibujar:
     * - `textureId`: GL texture
     * - `model`: matriz 4×4 con traslación/rotación/escala
     * - `uvCoords`: UV coordinates for texture sampling
     */
    fun drawCommand(
        textureId: Int,
        model: FloatArray,
        uvCoords: UVCoords = UVCoords()
    )

    /**
     * End batch drawing and execute all commands
     */
    fun end()

    /**
     * Avanza la animación (delta en ms)
     */
    fun update(deltaMs: Long)
}

// Backward compatibility interface
interface ISpriteRenderer : ISpriteBatch {
    /**
     * Ejecuta todos los comandos de dibujo encolados
     */
    @Deprecated("Use begin()/end() pattern instead")
    fun flushBatch()

    /**
     * Limpia los comandos de dibujo pendientes
     */
    @Deprecated("Use begin()/end() pattern instead")
    fun clearCommands()

    // Backward compatibility method
    @Deprecated("Use drawCommand instead")
    fun draw(rect: Rect) {
        // Default implementation for backward compatibility
    }

    @Deprecated("Use update(deltaMs) instead")
    fun update() {
        // Default implementation for backward compatibility
    }
}
