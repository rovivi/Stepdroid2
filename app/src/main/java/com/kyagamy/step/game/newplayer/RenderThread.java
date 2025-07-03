package com.kyagamy.step.game.newplayer;

import android.graphics.Canvas;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.SurfaceHolder;

/**
 * Simple render thread that locks the SurfaceHolder and draws the game.
 */
class RenderThread extends HandlerThread {

    private final SurfaceHolder holder;
    private final GamePlayNew game;
    private Handler handler;

    RenderThread(SurfaceHolder holder, GamePlayNew game) {
        super("RenderThread");
        this.holder = holder;
        this.game = game;
    }

    void startRendering() {
        start();
        handler = new Handler(getLooper());
    }

    void postRender() {
        if (handler == null) return;
        handler.post(() -> {
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    synchronized (holder) {
                        game.draw(canvas);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    void stopRendering() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        quitSafely();
    }
}
