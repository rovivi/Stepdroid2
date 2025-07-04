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
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SpriteGLRenderer(private val context: Context, private val frames: Array<Bitmap>) : GLSurfaceView.Renderer, ISpriteRenderer {

    private val textureIds = IntArray(frames.size)
    private var program = 0
    private var positionHandle = 0
    private var texHandle = 0
    private var mvpMatrixHandle = 0
    private var textureHandle = 0
    private var vertexBuffer: FloatBuffer
    private var texBuffer: FloatBuffer
    private val mvpMatrix = FloatArray(16)
    private var viewWidth = 0
    private var viewHeight = 0
    private var rect: Rect = Rect()
    private var frameIndex = 0
    private var lastTime = System.currentTimeMillis()
    private val frameTime = 100L

    init {
        val vertices = floatArrayOf(
            -1f, 1f,
            -1f, -1f,
            1f, 1f,
            1f, -1f
        )
        val tex = floatArrayOf(
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        )
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vertices).position(0)
        texBuffer = ByteBuffer.allocateDirect(tex.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        texBuffer.put(tex).position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Habilitar transparencias
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        GLES20.glGenTextures(frames.size, textureIds, 0)
        for (i in frames.indices) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[i])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, frames[i], 0)
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    // textura en unidad 0
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        GLES20.glViewport(0, 0, width, height)
        Matrix.setIdentityM(mvpMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
       // GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        updateFrameIndex()
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[frameIndex])
        GLES20.glUniform1i(textureHandle, 0)
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)
        texBuffer.position(0)
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer)
        GLES20.glEnableVertexAttribArray(texHandle)
        Matrix.setIdentityM(mvpMatrix,0)
        val left = rect.left.toFloat() / viewWidth * 2f - 1f
        val right = rect.right.toFloat() / viewWidth * 2f - 1f
        val top = 1f - rect.top.toFloat() / viewHeight * 2f
        val bottom = 1f - rect.bottom.toFloat() / viewHeight * 2f
        val scaleX = (right - left) / 2f
        val scaleY = (top - bottom) / 2f
        Matrix.translateM(mvpMatrix,0,left + scaleX, bottom + scaleY,0f)
        Matrix.scaleM(mvpMatrix,0,scaleX,scaleY,1f)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle,1,false,mvpMatrix,0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)


//        //test
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)   // limpia fondo
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)   // unidad 0
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[frameIndex])
    }

    override fun draw(rect: Rect) {
        this.rect = rect
    }

    override fun update() {
        // nothing extra, frame handled each draw
    }

    private fun updateFrameIndex() {
        val now = System.currentTimeMillis()
        if (now - lastTime > frameTime) {
            frameIndex = (frameIndex + 1) % frames.size
            lastTime = now
        }
    }

    private fun createProgram(vs: String, fs: String): Int {
        val vsId = loadShader(GLES20.GL_VERTEX_SHADER, vs)
        val fsId = loadShader(GLES20.GL_FRAGMENT_SHADER, fs)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vsId)
        GLES20.glAttachShader(program, fsId)
        GLES20.glLinkProgram(program)
        return program
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            uniform mat4 uMVPMatrix;
            void main(){
                vTexCoord = aTexCoord;
                gl_Position = uMVPMatrix * vec4(aPosition,0.0,1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main(){
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }
}
