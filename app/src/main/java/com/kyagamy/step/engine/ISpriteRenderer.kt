package com.kyagamy.step.engine

import android.graphics.Rect

interface ISpriteRenderer {
    /**
     * Encola un sprite para dibujar:
     * - `textureId`: GL texture
     * - `model`: matriz 4×4 con traslación/rotación/escala
     * - `uvOff`: offset/escala en UV para atlas (x, y, width, height)
     */
    fun drawCommand(
        textureId: Int,
        model: FloatArray,
        uvOff: FloatArray = floatArrayOf(0f, 0f, 1f, 1f)
    )

    /**
     * Avanza la animación (delta en ms)
     */
    fun update(deltaMs: Long)

    /**
     * Ejecuta todos los comandos de dibujo encolados
     */
    fun flushBatch()

    /**
     * Limpia los comandos de dibujo pendientes
     */
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
