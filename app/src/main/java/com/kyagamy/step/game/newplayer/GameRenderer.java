package com.kyagamy.step.game.newplayer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.MediaPlayer;

import com.kyagamy.step.common.step.Game.GameRow;

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
        debugPaint.setColor(Color.WHITE);

        canvas.drawText("FPS: " + fps, 0, 250, debugPaint);
        canvas.drawText("C Seg: " + String.format(new Locale("es"), "%.5f", (gameState.currentSecond / 100d) - gameState.offset), 0, playerSizeY + 40, debugPaint);
        canvas.drawText("offset: " + musicPlayerUpdated, 0, playerSizeY + 90, debugPaint);
        canvas.drawText("diff: " + String.format(new Locale("es"), "%.5f", (gameState.currentSecond / 100d) - gameState.offset - musicPlayer.getCurrentPosition() / 1000d), 0, playerSizeY + 110, debugPaint);
        canvas.drawText("MP ms: " + musicPlayer.getCurrentPosition() / 1000d, 0, playerSizeY + 70, debugPaint);
        canvas.drawText("C Beat: " + String.format(new Locale("es"), "%.3f", gameState.currentBeat), 0, playerSizeY - 150, debugPaint);
        canvas.drawText("C BPM: " + gameState.BPM, 0, playerSizeY - 250, debugPaint);
        canvas.drawText("C Speed: " + gameState.currentSpeedMod, 0, playerSizeY - 100, debugPaint);
        canvas.drawText("Scroll: " + gameState.lastScroll, 0, playerSizeY, debugPaint);

        StringBuilder st = new StringBuilder();
        for (int j = 0; j < GameConstants.MAX_INPUT_FINGERS; j++)
            st.append(gameState.inputs[j]);
        canvas.drawText("pad: " + st, playerSizeX - GameConstants.DEBUG_TEXT_OFFSET_X, playerSizeY - GameConstants.DEBUG_TEXT_OFFSET_Y, debugPaint);

        debugPaint.setColor(Color.TRANSPARENT);
    }
}