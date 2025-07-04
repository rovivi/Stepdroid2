package com.kyagamy.step.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin
import kotlin.random.Random

class TestSongRenderer(private val context: Context) : GLSurfaceView.Renderer, ISpriteRenderer {

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
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.2f, 1.0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        gameTime = System.currentTimeMillis() - startTime

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
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

    override fun draw(rect: Rect) {
        // No necesario para este renderer
    }

    override fun update() {
        // La actualización se hace en onDrawFrame
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
}