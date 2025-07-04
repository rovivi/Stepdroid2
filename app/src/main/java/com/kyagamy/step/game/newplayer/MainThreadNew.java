package com.kyagamy.step.game.newplayer;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class MainThreadNew extends Thread {
    private final int maxFps;
    private final int framePeriod;

    private double avergeFPS;
    public final SurfaceHolder sulrfaceHolder;
    private final GamePlayNew game;
    public boolean running;
    public static Canvas canvas;



    public void setRunning(Boolean running) {
        this.running = running;
    }

    MainThreadNew(SurfaceHolder holder, GamePlayNew game, int maxFps) {
        super("GameLoopThread");
        this.sulrfaceHolder = holder;
        this.game = game;
        this.maxFps = maxFps > 0 ? maxFps : 60;
        this.framePeriod = 1000 / this.maxFps;
    }
    @Override
    public void run() {
        long startTime;
        long timeMillis;
        long waitTime;
        long totalTime = 0;
        int frameCount = 0;

        while (running) {
            startTime = System.nanoTime();
            canvas = null;
            try {
                canvas = this.sulrfaceHolder.lockCanvas();
                synchronized (sulrfaceHolder) {
                    this.game.update();
                    this.game.draw(canvas);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    try {
                        sulrfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            timeMillis = (System.nanoTime() - startTime) / 1_000_000L;
            waitTime = framePeriod - timeMillis;
            if (waitTime > 0) {
                try {
                    sleep(waitTime);
                } catch (InterruptedException ignored) {
                }
            }

            totalTime += System.nanoTime() - startTime;
            frameCount++;
            if (frameCount == maxFps) {
                avergeFPS = 1000.0 / ((totalTime / frameCount) / 1_000_000.0);
                frameCount = 0;
                totalTime = 0;
            }
            game.fps = avergeFPS;
        }

    }


} 