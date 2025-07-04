package com.kyagamy.step.game.newplayer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.MediaPlayer;

import com.kyagamy.step.common.step.Game.GameRow;
import com.kyagamy.step.game.newplayer.Evaluator;

import java.util.ArrayList;
import java.util.Locale;

public class GameRenderer {
    private final StepsDrawer stepsDrawer;
    private final LifeBar bar;
    private final Combo combo;
    private final Paint debugPaint;
    private final Paint clearPaint;
    private final boolean isLandScape;

    public GameRenderer(StepsDrawer stepsDrawer, LifeBar bar, Combo combo, Paint debugPaint, boolean isLandScape) {
        this.stepsDrawer = stepsDrawer;
        this.bar = bar;
        this.combo = combo;
        this.debugPaint = debugPaint;
        this.isLandScape = isLandScape;

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public void drawGame(Canvas canvas, ArrayList<GameRow> drawList, GameState gameState, int speed) {
        stepsDrawer.draw(canvas, drawList);
    }

    public void drawUI(Canvas canvas) {
        bar.draw(canvas);
        combo.draw(canvas);

        if (!isLandScape) {
            canvas.drawRect(new Rect(0, stepsDrawer.sizeY, stepsDrawer.offsetX + stepsDrawer.sizeX, stepsDrawer.sizeY * 2), clearPaint);
        }
    }

    public void drawDebugInfo(Canvas canvas, GameState gameState, MediaPlayer musicPlayer, Double fps, int speed, boolean musicPlayerUpdated, int playerSizeX, int playerSizeY) {
        debugPaint.setTextSize(GameConstants.DEBUG_TEXT_SIZE_SMALL);
        debugPaint.setStyle(Paint.Style.FILL);
        canvas.drawPaint(debugPaint);
        debugPaint.setTextSize(GameConstants.DEBUG_TEXT_SIZE);

        // === TOP SECTION - Hardware Acceleration Status ===
        boolean isCanvasHardwareAccelerated = canvas.isHardwareAccelerated();
        debugPaint.setColor(isCanvasHardwareAccelerated ? Color.GREEN : Color.RED);
        canvas.drawText("HW Accel: " + (isCanvasHardwareAccelerated ? "ENABLED" : "DISABLED"), 10, 30, debugPaint);

        debugPaint.setColor(Color.CYAN);
        canvas.drawText("GPU: " + (isCanvasHardwareAccelerated ? "ACTIVE" : "SOFTWARE"), 10, 55, debugPaint);

        // === PERFORMANCE INFO ===
        debugPaint.setColor(Color.WHITE);
        canvas.drawText("FPS: " + fps, 10, 85, debugPaint);
        canvas.drawText("Speed: " + speed, 10, 110, debugPaint);

        // === TIMING INFO ===
        debugPaint.setColor(Color.YELLOW);
        canvas.drawText("Beat: " + String.format(new Locale("es"), "%.3f", gameState.currentBeat), 10, 140, debugPaint);
        canvas.drawText("BPM: " + gameState.BPM, 10, 165, debugPaint);
        canvas.drawText("Sec: " + String.format(new Locale("es"), "%.3f", (gameState.currentSecond / 100d) - gameState.offset), 10, 190, debugPaint);

        // === AUDIO SYNC INFO ===
        debugPaint.setColor(Color.MAGENTA);
        canvas.drawText("Sync: " + musicPlayerUpdated, 10, 220, debugPaint);
        canvas.drawText("Diff: " + String.format(new Locale("es"), "%.3f", (gameState.currentSecond / 100d) - gameState.offset - musicPlayer.getCurrentPosition() / 1000d), 10, 245, debugPaint);

        // === INPUT STATUS (Right side) ===
        StringBuilder inputStatus = new StringBuilder();
        for (int j = 0; j < GameConstants.MAX_INPUT_FINGERS; j++)
            inputStatus.append(gameState.inputs[j]);
        debugPaint.setColor(Color.GREEN);
        canvas.drawText("Inputs: " + inputStatus, playerSizeX - 200, 30, debugPaint);

        // === GAME STATE INFO (Right side) ===
        debugPaint.setColor(Color.CYAN);
        canvas.drawText("Speed Mod: " + gameState.currentSpeedMod, playerSizeX - 200, 60, debugPaint);
        canvas.drawText("Scroll: " + gameState.lastScroll, playerSizeX - 200, 90, debugPaint);

        // === BOTTOM SECTION - Song and Level Info ===
        debugPaint.setTextSize(GameConstants.DEBUG_TEXT_SIZE + 2);
        debugPaint.setColor(Color.WHITE);

        // Song name (if available)
        String currentSongName = Evaluator.Companion.getSongName();
        if (currentSongName != null && !currentSongName.isEmpty()) {
            canvas.drawText("♪ " + currentSongName, 10, playerSizeY - 60, debugPaint);
        }

        // Level/Difficulty info
        debugPaint.setColor(Color.YELLOW);
        canvas.drawText("Level: " + gameState.currentElement + "/" + gameState.steps.size(), 10, playerSizeY - 35, debugPaint);

        // Score info
        debugPaint.setColor(Color.GREEN);
        canvas.drawText("Score: " + String.format(new Locale("es"), "%.1f", Evaluator.Companion.getTotalScore()), 10, playerSizeY - 10, debugPaint);

        // Progress bar
        debugPaint.setColor(Color.BLUE);
        float progress = (float) gameState.currentElement / gameState.steps.size();
        canvas.drawRect(playerSizeX - 210, playerSizeY - 25, playerSizeX - 10, playerSizeY - 15, debugPaint);
        debugPaint.setColor(Color.GREEN);
        canvas.drawRect(playerSizeX - 210, playerSizeY - 25, playerSizeX - 210 + (200 * progress), playerSizeY - 15, debugPaint);

        debugPaint.setColor(Color.TRANSPARENT);
    }
}