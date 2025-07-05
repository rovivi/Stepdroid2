package com.kyagamy.step.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

data class DrawCommand(
    val textureId: Int,
    val model: FloatArray,
    val uvCoords: UVCoords
)

class SpriteGLRenderer(private val context: Context, private val frames: Array<Bitmap>) : GLSurfaceView.Renderer, ISpriteRenderer {

    private val textureIds = IntArray(frames.size)
    private var program = 0
    private var quadVBO = 0
    private var indexVBO = 0

    // Shader handles
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var mvpMatrixHandle = 0
    private var textureHandle = 0

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private var viewWidth = 0
    private var viewHeight = 0

    // Optimized batching system
    private val drawCommands = mutableListOf<DrawCommand>()
    private var vertexBuffer: FloatBuffer
    private var texBuffer: FloatBuffer
    private var indexBuffer: ShortBuffer
    private var batchActive = false

    // Matrix pool for avoiding allocations
    private val matrixPool = Array(100) { FloatArray(16) }
    private var matrixPoolIndex = 0

    // Animation
    private var frameIndex = 0
    private var lastTime = System.currentTimeMillis()
    private val frameTime = 100L

    init {
        // Preparar buffers básicos
        val vertices = floatArrayOf(
            -1f, 1f,  // Top left
            -1f, -1f,  // Bottom left
            1f, 1f,  // Top right
            1f, -1f   // Bottom right
        )

        val texCoords = floatArrayOf(
            0f, 0f,  // Top left
            0f, 1f,  // Bottom left
            1f, 0f,  // Top right
            1f, 1f   // Bottom right
        )

        val indices = shortArrayOf(0, 1, 2, 1, 3, 2)

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertices).position(0)

        texBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        texBuffer.put(texCoords).position(0)

        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        indexBuffer.put(indices).position(0)

        Matrix.setIdentityM(projectionMatrix, 0)
        Matrix.setIdentityM(viewMatrix, 0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Habilitar transparencias
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Crear programa shader
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program == 0) {
            android.util.Log.e("SpriteGLRenderer", "Failed to create shader program!")
            return
        }

        // Obtener handles
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")

        // Crear VBOs estáticos
        createVBOs()

        // Cargar texturas
        loadTextures()

        android.util.Log.d(
            "SpriteGLRenderer",
            "Handles: pos=$positionHandle, tex=$texCoordHandle, mvp=$mvpMatrixHandle, texture=$textureHandle"
        )
    }

    private fun createVBOs() {
        val vbos = IntArray(2)
        GLES20.glGenBuffers(2, vbos, 0)

        quadVBO = vbos[0]
        indexVBO = vbos[1]

        // Vertex buffer
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadVBO)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            (vertexBuffer.capacity() + texBuffer.capacity()) * 4,
            null,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexBuffer.capacity() * 4, vertexBuffer)
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER,
            vertexBuffer.capacity() * 4,
            texBuffer.capacity() * 4,
            texBuffer
        )

        // Index buffer
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexVBO)
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER,
            indexBuffer.capacity() * 2,
            indexBuffer,
            GLES20.GL_STATIC_DRAW
        )

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun loadTextures() {
        GLES20.glGenTextures(frames.size, textureIds, 0)
        android.util.Log.d("SpriteGLRenderer", "Loading ${frames.size} textures")

        for (i in frames.indices) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[i])
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, frames[i], 0)
            android.util.Log.d("SpriteGLRenderer", "Loaded texture $i with ID ${textureIds[i]}")
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        GLES20.glViewport(0, 0, width, height)

        // Crear matriz de proyección ortográfica simple
        Matrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f)
        Matrix.setIdentityM(viewMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        // El frame será dibujado llamando end() externamente
    }

    // New optimized interface
    override fun begin() {
        if (batchActive) {
            android.util.Log.w("SpriteGLRenderer", "begin() called while batch is already active")
            return
        }
        batchActive = true
        drawCommands.clear()
        matrixPoolIndex = 0
    }

    override fun drawCommand(textureId: Int, model: FloatArray, uvCoords: UVCoords) {
        if (!batchActive) {
            android.util.Log.w("SpriteGLRenderer", "drawCommand() called outside of begin()/end()")
            return
        }

        // Use matrix pool to avoid allocation
        val pooledMatrix = getPooledMatrix()
        System.arraycopy(model, 0, pooledMatrix, 0, 16)

        drawCommands.add(DrawCommand(textureId, pooledMatrix, uvCoords))
    }

    private fun getPooledMatrix(): FloatArray {
        if (matrixPoolIndex >= matrixPool.size) {
            // Expand pool if needed
            matrixPoolIndex = 0
            android.util.Log.w("SpriteGLRenderer", "Matrix pool exhausted, recycling from start")
        }
        return matrixPool[matrixPoolIndex++]
    }

    override fun end() {
        if (!batchActive) {
            android.util.Log.w("SpriteGLRenderer", "end() called without begin()")
            return
        }

        executeBatch()
        batchActive = false
    }

    private fun executeBatch() {
        if (drawCommands.isEmpty()) return

        android.util.Log.d("SpriteGLRenderer", "Executing batch with ${drawCommands.size} commands")

        GLES20.glUseProgram(program)

        // Bind VBOs
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadVBO)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexVBO)

        // Configure vertex attributes
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, 0)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer.capacity() * 4
        )

        // Configure texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(textureHandle, 0)

        // Group commands by texture for optimal batching
        val groupedCommands = drawCommands.groupBy { it.textureId }
        android.util.Log.d(
            "SpriteGLRenderer",
            "Batched into ${groupedCommands.size} texture groups"
        )

        for ((textureId, commands) in groupedCommands) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

            // Draw each command
            for (command in commands) {
                // Calculate MVP matrix
                Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, command.model, 0)

                // Apply MVP matrix
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

                // Draw
                GLES20.glDrawElements(
                    GLES20.GL_TRIANGLES,
                    6,
                    GLES20.GL_UNSIGNED_SHORT,
                    0
                )
            }
        }

        // Cleanup
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun update(deltaMs: Long) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime > frameTime) {
            frameIndex = (frameIndex + 1) % frames.size
            lastTime = currentTime
        }
    }

    // Backward compatibility methods
    @Deprecated("Use begin()/end() pattern instead")
    override fun flushBatch() {
        if (!batchActive) {
            begin()
        }
        executeBatch()
    }

    @Deprecated("Use begin()/end() pattern instead")
    override fun clearCommands() {
        drawCommands.clear()
    }

    @Deprecated("Use drawCommand with UVCoords instead")
    fun drawCommand(textureId: Int, model: FloatArray, uvOff: FloatArray) {
        val uvCoords = UVCoords(uvOff[0], uvOff[1], uvOff[2], uvOff[3])
        drawCommand(textureId, model, uvCoords)
    }

    // Compatibility methods
    @Deprecated("Use drawCommand instead")
    override fun draw(rect: Rect) {
        // Convertir rect a matriz modelo
        val model = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(
            model,
            0,
            rect.left.toFloat() + rect.width() / 2f,
            rect.top.toFloat() + rect.height() / 2f,
            0f
        )
        Matrix.scaleM(model, 0, rect.width().toFloat() / 2f, rect.height().toFloat() / 2f, 1f)

        // Usar textura actual
        val textureId = if (textureIds.isNotEmpty()) textureIds[frameIndex] else 0
        drawCommand(textureId, model, UVCoords())
    }

    @Deprecated("Use update(deltaMs) instead")
    override fun update() {
        update(16L) // ~60 FPS
    }

    fun getCurrentTextureId(): Int {
        return if (textureIds.isNotEmpty()) textureIds[frameIndex] else 0
    }

    fun getTextureId(frameIndex: Int): Int {
        return if (frameIndex < textureIds.size) textureIds[frameIndex] else 0
    }

    fun getTextureCount(): Int {
        return textureIds.size
    }

    // Utility function to create transformation matrix with rotation
    fun createTransformMatrix(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Float = 0f
    ): FloatArray {
        val model = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, 0f)
        if (rotation != 0f) {
            Matrix.rotateM(model, 0, rotation, 0f, 0f, 1f)
        }
        Matrix.scaleM(model, 0, width, height, 1f)
        return model
    }

    private fun createProgram(vs: String, fs: String): Int {
        val vsId = loadShader(GLES20.GL_VERTEX_SHADER, vs)
        val fsId = loadShader(GLES20.GL_FRAGMENT_SHADER, fs)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vsId)
        GLES20.glAttachShader(program, fsId)
        GLES20.glLinkProgram(program)

        // Verificar errores
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            android.util.Log.e("SpriteGLRenderer", "Program link error: $log")
            GLES20.glDeleteProgram(program)
            return 0
        }

        return program
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)

        // Verificar errores
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            android.util.Log.e("SpriteGLRenderer", "Shader compile error: $log")
            GLES20.glDeleteShader(shader)
            return 0
        }

        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            uniform mat4 uMVPMatrix;
            varying vec2 vTexCoord;
            
            void main() {
                vTexCoord = aTexCoord;
                gl_Position = uMVPMatrix * vec4(aPosition, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }
}
