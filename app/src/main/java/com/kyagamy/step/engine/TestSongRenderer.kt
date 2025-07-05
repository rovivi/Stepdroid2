package com.kyagamy.step.engine

import android.content.Context
import android.graphics.Point
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.kyagamy.step.common.step.Game.GameRow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class TestSongRenderer(private val context: Context) : GLSurfaceView.Renderer, ISpriteRenderer {

    // Test modes
    enum class TestMode {
        SKIN_VARIANTS,      // prueba() - All skin variants in rows
        SKIN_GRID,          // pruebaGrid() - All sprites in grid
        SPECIFIC_SKIN,      // pruebaSpecificSkin() - Only one skin type
        NORMAL_GAME,        // drawGame() - Normal game rendering
        LEGACY_NOTES        // Legacy rendering system
    }

    private var testMode = TestMode.SKIN_VARIANTS

    private val notes = mutableListOf<Note>()
    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpMatrixHandle = 0
    private var vertexBuffer: FloatBuffer
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private var viewWidth = 0
    private var viewHeight = 0
    private var lastNoteTime = 0L
    private val noteInterval = 500L // Nueva nota cada 500ms
    private var gameTime = 0L
    private var startTime = 0L

    // StepsDrawerGL integration
    private var stepsDrawer: StepsDrawerGL? = null
    private val gameRows = ArrayList<GameRow>()

    data class Note(
        var x: Float,
        var y: Float,
        var color: FloatArray,
        var lane: Int // 0-4 para 5 carriles
    )

    init {
        val vertices = floatArrayOf(
            -0.05f, 0.05f,  // Top left
            -0.05f, -0.05f, // Bottom left
            0.05f, 0.05f,   // Top right
            0.05f, -0.05f   // Bottom right
        )

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertices).position(0)

        startTime = System.currentTimeMillis()

        // Initialize StepsDrawerGL
        initializeStepsDrawer()
    }

    // Method to change test mode
    fun setTestMode(mode: TestMode) {
        testMode = mode
    }

    // Method to cycle through test modes
    fun cycleTestMode() {
        val modes = TestMode.values()
        val currentIndex = modes.indexOf(testMode)
        val nextIndex = (currentIndex + 1) % modes.size
        testMode = modes[nextIndex]
    }

    private fun initializeStepsDrawer() {
        val screenSize =
            Point(1080, 1920) // Default screen size, will be updated in onSurfaceChanged
        stepsDrawer = StepsDrawerGL(
            context,
            "pump-single", // Game mode for testing
            "16:9",
            false, // Portrait mode
            screenSize
        )
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.2f, 1.0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Initialize basic renderer program
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")

        // Initialize StepsDrawerGL program
        stepsDrawer?.initializeGLProgram()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Update StepsDrawerGL viewport
        stepsDrawer?.setViewport(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        gameTime = System.currentTimeMillis() - startTime

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Draw all test modes simultaneously by dividing screen into sections
        drawAllTestModes()

        // Update game logic
        updateGame()
    }

    private fun drawLegacyNotes() {
        GLES20.glUseProgram(program)

        // Generar nuevas notas
        if (gameTime - lastNoteTime > noteInterval) {
            generateNote()
            lastNoteTime = gameTime
        }

        // Actualizar y dibujar notas
        updateNotes()
        drawNotes()

        // Dibujar receptores (líneas donde deben caer las notas)
        drawReceptors()
    }

    private fun updateGame() {
        // Update sprites
        stepsDrawer?.update()
    }

    private fun generateNote() {
        val lane = Random.nextInt(5) // 5 carriles (0-4)
        val x = -0.8f + (lane * 0.4f) // Posicionar en carriles
        val y = 1.2f // Empezar arriba de la pantalla
        val colors = arrayOf(
            floatArrayOf(1.0f, 0.2f, 0.2f, 1.0f), // Rojo
            floatArrayOf(0.2f, 1.0f, 0.2f, 1.0f), // Verde
            floatArrayOf(0.2f, 0.2f, 1.0f, 1.0f), // Azul
            floatArrayOf(1.0f, 1.0f, 0.2f, 1.0f), // Amarillo
            floatArrayOf(1.0f, 0.2f, 1.0f, 1.0f)  // Magenta
        )

        notes.add(Note(x, y, colors[lane], lane))
    }

    private fun updateNotes() {
        val speed = 0.02f // Velocidad de caída
        val iterator = notes.iterator()

        while (iterator.hasNext()) {
            val note = iterator.next()
            note.y -= speed

            // Remover notas que salieron de la pantalla
            if (note.y < -1.5f) {
                iterator.remove()
            }
        }
    }

    private fun drawNotes() {
        val modelMatrix = FloatArray(16)
        val tempMatrix = FloatArray(16)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)

        for (note in notes) {
            // Crear matriz de transformación para cada nota
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, note.x, note.y, 0f)

            // Aplicar transformación
            Matrix.multiplyMM(tempMatrix, 0, mvpMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, tempMatrix, 0)

            // Establecer color
            GLES20.glUniform4fv(colorHandle, 1, note.color, 0)

            // Dibujar nota
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun drawReceptors() {
        val modelMatrix = FloatArray(16)
        val tempMatrix = FloatArray(16)
        val receptorColor = floatArrayOf(1.0f, 1.0f, 1.0f, 0.8f)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)

        // Dibujar receptores en la parte inferior
        for (i in 0 until 5) {
            val x = -0.8f + (i * 0.4f)
            val y = -0.8f

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, y, 0f)
            Matrix.scaleM(modelMatrix, 0, 1.2f, 0.5f, 1.0f)

            Matrix.multiplyMM(tempMatrix, 0, mvpMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, tempMatrix, 0)

            GLES20.glUniform4fv(colorHandle, 1, receptorColor, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    // Draw all test modes simultaneously by dividing screen into sections
    private fun drawAllTestModes() {
        // Save original viewport
        val originalViewport = IntArray(4)
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, originalViewport, 0)

        // Divide screen into 5 sections (one for each test mode)
        val sectionHeight = viewHeight / 5
        val sectionWidth = viewWidth

        // Clear background with dark color
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        // Section 1: SKIN_VARIANTS (top)
        GLES20.glViewport(0, viewHeight - sectionHeight, sectionWidth, sectionHeight)
        stepsDrawer?.setViewport(sectionWidth, sectionHeight)
        stepsDrawer?.prueba()

        // Section 2: SKIN_GRID
        GLES20.glViewport(0, viewHeight - sectionHeight * 2, sectionWidth, sectionHeight)
        stepsDrawer?.setViewport(sectionWidth, sectionHeight)
        stepsDrawer?.pruebaGrid()

        // Section 3: SPECIFIC_SKIN
        GLES20.glViewport(0, viewHeight - sectionHeight * 3, sectionWidth, sectionHeight)
        stepsDrawer?.setViewport(sectionWidth, sectionHeight)
        stepsDrawer?.pruebaSpecificSkin(StepsDrawerGL.SkinType.SELECTED)

        // Section 4: NORMAL_GAME
        GLES20.glViewport(0, viewHeight - sectionHeight * 4, sectionWidth, sectionHeight)
        stepsDrawer?.setViewport(sectionWidth, sectionHeight)
        stepsDrawer?.drawGame(gameRows)

        // Section 5: LEGACY_NOTES (bottom)
        GLES20.glViewport(0, 0, sectionWidth, sectionHeight)
        setupLegacyViewport(sectionWidth, sectionHeight)
        drawLegacyNotes()

        // Draw separators between sections
        drawSectionSeparators(sectionHeight)

        // Restore original viewport
        GLES20.glViewport(
            originalViewport[0],
            originalViewport[1],
            originalViewport[2],
            originalViewport[3]
        )
        stepsDrawer?.setViewport(viewWidth, viewHeight)
    }

    private fun drawSectionSeparators(sectionHeight: Int) {
        // Restore full viewport for drawing separators
        GLES20.glViewport(0, 0, viewWidth, viewHeight)
        GLES20.glUseProgram(program)

        // Set up orthographic projection for 2D drawing
        val orthoMatrix = FloatArray(16)
        Matrix.orthoM(orthoMatrix, 0, 0f, viewWidth.toFloat(), 0f, viewHeight.toFloat(), -1f, 1f)

        val separatorColor = floatArrayOf(1.0f, 1.0f, 1.0f, 0.5f) // Semi-transparent white
        val thickness = 3

        // Draw horizontal separators between sections
        for (i in 1 until 5) {
            val y = i * sectionHeight
            val vertices = floatArrayOf(
                0f, y.toFloat(),
                0f, (y + thickness).toFloat(),
                viewWidth.toFloat(), y.toFloat(),
                viewWidth.toFloat(), (y + thickness).toFloat()
            )

            val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            buffer.put(vertices).position(0)

            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, buffer)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, orthoMatrix, 0)
            GLES20.glUniform4fv(colorHandle, 1, separatorColor, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(positionHandle)
        }
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * vec4(aPosition, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                gl_FragColor = uColor;
            }
        """
    }

    // Set up the OpenGL matrices for legacy notes rendering in a section viewport
    private fun setupLegacyViewport(width: Int, height: Int) {
        // Adjust matrices for legacy rendering in smaller viewport
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    override fun drawCommand(
        textureId: Int,
        model: FloatArray,
        uvOff: FloatArray
    ) {
        TODO("Not yet implemented")
    }

    override fun update(deltaMs: Long) {
        TODO("Not yet implemented")
    }

    override fun flushBatch() {
        TODO("Not yet implemented")
    }

    override fun clearCommands() {
        TODO("Not yet implemented")
    }

    // ISpriteRenderer interface methods
    override fun draw(rect: android.graphics.Rect) {
        // This method is required by ISpriteRenderer but our rendering is handled in onDrawFrame
        // We can use this for any additional drawing if needed
    }

    override fun update() {
        // Update sprites - this is called by the OpenGLSpriteView
        stepsDrawer?.update()
    }
}