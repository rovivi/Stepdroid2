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

    private final RenderThread renderThread;


    public void setRunning(Boolean running) {
        this.running = running;
    }

    MainThreadNew(SurfaceHolder holder, GamePlayNew game, int maxFps) {
        super("GameLoopThread");
        this.sulrfaceHolder = holder;
        this.game = game;
        this.maxFps = maxFps > 0 ? maxFps : 60;
        this.framePeriod = 1000 / this.maxFps;
        this.renderThread = new RenderThread(holder, game);
    }
    @Override
    public void run() {
        long startTime;
        long timeMillis;
        long waitTime;
        long totalTime = 0;
        int frameCount = 0;

        renderThread.startRendering();

        while (running) {
            startTime = System.nanoTime();
            this.game.update();
            renderThread.postRender();

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

        renderThread.stopRendering();
    }


} 