package com.kyagamy.step.common.step.commonGame.customSprite;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import com.kyagamy.step.engine.ISpriteRenderer;
import com.kyagamy.step.engine.SpriteGLRenderer;

import com.kyagamy.step.common.step.CommonGame.TransformBitmap;
import com.kyagamy.step.engine.UVCoords;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Created by Rodrigo Vidal February 2018
 */
public  class SpriteReader implements ISpriteRenderer {
    public Bitmap[] frames;
    private int frameIndex;
    private float frameTime;
    private long lastFrame;
    private boolean isPlaying = false;
    private float lapsedtime;
    private int interpolateIndex;
    boolean rotate = false;

    double seconds;
    Paint paint, painShader;
    ArrayList<String[]> attacksList = new ArrayList<String[]>();

    private boolean useCanvas = true;
    private Canvas currentCanvas;
    private SpriteGLRenderer glRenderer;

    // Simple command storage without DrawCommand class
    private ArrayList<CommandData> drawCommands = new ArrayList<>();

    @Override
    public void begin() {

    }

    @Override
    public void drawCommand(int textureId, float @NotNull [] model, @NotNull UVCoords uvCoords) {

    }

    @Override
    public void end() {

    }

    // Simple data holder for draw commands
    private static class CommandData {
        int textureId;
        float[] model;
        float[] uvOff;

        CommandData(int textureId, float[] model, float[] uvOff) {
            this.textureId = textureId;
            this.model = model.clone();
            this.uvOff = uvOff.clone();
        }
    }

    /**
     * This constructor request bitmap array for each frame in the sprite animation
     * @param frames Frame as bitmap
     * @param timeFrame Time the bitmap will apear in the screen
     */
    public SpriteReader(Bitmap[] frames, float timeFrame) {
        this.frameIndex = 0;
        this.frames = frames;
        frameTime = timeFrame / frames.length;
    }

    /***
     * Create sprite since a long bitmap resource and create square whit the parameter size X and size Y
     * @param sprite bitmap of sprite image
     * @param sizeX number of sprite squares horizontally
     * @param sizeY nombre of sprite squares vertically
     * @param timeFrame Time the bitmap will appears in the screen
     */
    public SpriteReader(Bitmap sprite, int sizeX, int sizeY, float timeFrame) {
        paint = new Paint();
        painShader = new Paint();

        painShader.setAntiAlias(true);
        painShader.setDither(true);
        paint.setAntiAlias(true);
        paint.setDither(true);
        frames = new Bitmap[sizeX * sizeY];
        int frameWhith = (sprite.getWidth() / sizeX);
        int frameHeight = (sprite.getHeight() / sizeY);
        int count = 0;
        try {
            for (int y = 0; y < sizeY; y++) {
                for (int x = 0; x < sizeX; x++) {
                    frames[count] = Bitmap.createBitmap(sprite, x * frameWhith, y * frameHeight, frameWhith, frameHeight);
                    count++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.frameIndex = 0;
        frameTime = timeFrame / frames.length;
    }

    public void play() {
        isPlaying = true;
        frameIndex = 0;
        lastFrame = System.currentTimeMillis();
    }

    public void stop() {
        isPlaying = false;
    }

    /**
     * Draw the sprite in the canvas
     * @param canvas destinated canvas
     * @param destiny sprite position as rect
     */
    public void draw(Canvas canvas, Rect destiny) {
        if (!isPlaying)
            return;
        if (rotate) {
            canvas.drawBitmap(TransformBitmap.RotateBitmap(frames[frameIndex], (float) (45)), null, destiny, paint);
        } else if (attacksList.size() < 1)
            canvas.drawBitmap(frames[frameIndex], null, destiny, paint);
    }

    public void opacityDraw(Canvas canvas, Rect destiny, int transparency) {
        if (transparency == 0)
            draw(canvas, destiny);
        else {
            paint.setAlpha((int) (transparency * 2.55));
            draw(canvas, destiny);
            paint.setAlpha(0);
        }
    }

    public void drawWhitShader(Canvas canvas, Rect destino, int percent) {
        Bitmap backing = Bitmap.createBitmap(frames[frameIndex].getWidth(), frames[frameIndex].getHeight(), Bitmap.Config.ARGB_8888);
        Canvas offscreen = new Canvas(backing);
        offscreen.drawBitmap(frames[frameIndex], 0, 0, null);
        Paint paint2 = new Paint();
        paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        paint2.setShader(new LinearGradient(0, 0, 0, frames[0].getHeight(), 0x15000000, 0x00000000, Shader.TileMode.CLAMP));
        offscreen.drawRect(0, 0, frames[0].getWidth(), frames[0].getHeight(), paint2);
        canvas.drawBitmap(backing, null, destino, paint2);
    }

    /**
     * Draw only a one time the sprite
     * @param canvas
     * @param destiny
     */
    public void staticDraw(Canvas canvas, Rect destiny) {
        if (!isPlaying) {
            return;
        }
        if ((1 + frameIndex) == frames.length) {
            isPlaying = false;
        } else {
            canvas.drawBitmap(frames[frameIndex], null, destiny, paint);
        }
    }

    // region ISpriteRenderer implementation
    @Override
    public void draw(Rect rect) {
        if (useCanvas) {
            if (currentCanvas != null) {
                draw(currentCanvas, rect);
            }
        } else if (glRenderer != null) {
            glRenderer.draw(rect);
        }
    }

    @Override
    public void update() {
        updateFrame();
        if (!useCanvas && glRenderer != null) {
            glRenderer.update();
        }
    }

    private void updateFrame() {
        lapsedtime = System.currentTimeMillis() - lastFrame;
        seconds += lapsedtime;
        if (lapsedtime > frameTime * 1000) {
            frameIndex++;
            if (frameIndex == frames.length) {
                frameIndex = 0;
            }
            lastFrame = System.currentTimeMillis();
        }
    }

    public void setCanvas(Canvas canvas) {
        this.currentCanvas = canvas;
    }

    public void setUseCanvas(boolean value) {
        this.useCanvas = value;
    }

    public void setGlRenderer(SpriteGLRenderer renderer) {
        this.glRenderer = renderer;
    }


    public void drawCommand(int textureId, float @NotNull [] model, float @NotNull [] uvOff) {
        drawCommands.add(new CommandData(textureId, model, uvOff));
    }

    @Override
    public void update(long deltaMs) {
        updateFrame();
        if (!useCanvas && glRenderer != null) {
            glRenderer.update(deltaMs);
        }
    }

    @Override
    public void flushBatch() {
        if (glRenderer != null) {
            for (CommandData command : drawCommands) {
                glRenderer.drawCommand(command.textureId, command.model, command.uvOff);
            }
            glRenderer.flushBatch();
        }
        drawCommands.clear();
    }

    @Override
    public void clearCommands() {
        drawCommands.clear();
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public Bitmap[] getFrames() {
        return frames;
    }
    // endregion
}
