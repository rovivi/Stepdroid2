package com.kyagamy.step.game.newplayer;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import androidx.annotation.RequiresApi;

import com.kyagamy.step.common.Common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import game.GameRow;
import game.StepObject;

public class GamePlayNew extends SurfaceView implements SurfaceHolder.Callback {
    private static int[] colors = {Color.BLUE, Color.RED, Color.YELLOW, Color.RED, Color.BLUE, Color.BLUE, Color.RED, Color.YELLOW, Color.RED, Color.BLUE};
    float testFloatNOTUSE = 0f;
    public MainThreadNew mainTread;
    //private Bitmap bgaBitmap;
    private MediaPlayer mpMusic;
    private int playerSizeX = 400, playerSizeY = 500;
    private boolean isLandScape = false;

    private GameState gameState;
    public Double fps;
    Handler handler1 = new Handler();
    private Paint paint, clearPaint;
    //////
    StepsDrawer stepsDrawer;
    LifeBar bar;
    Combo combo;
    private GamePad touchPad;
    String msj;
    BgPlayer bgPlayer;
    private int speed;

    public GamePlayNew(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private Runnable musicRun = new Runnable() {
        @Override
        public void run() {
            if (mpMusic != null) {
                mpMusic.start();
                gameState.isRunning = true;
            }
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void build1Object(VideoView videoView, StepObject stepData, Context context, Point sizeScreen, byte[] inputs) {
        try {
            isLandScape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            this.setZOrderOnTop(true); //necessary
            getHolder().setFormat(PixelFormat.TRANSPARENT);
            getHolder().addCallback(this);
            gameState = new GameState(stepData, inputs);
            gameState.reset();
            mpMusic = new MediaPlayer();

            clearPaint = new Paint();
            clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));


            mainTread = new MainThreadNew(getHolder(), this);
            mainTread.setRunning(true);

            bgPlayer = new BgPlayer(stepData.getPath(), stepData.getBgChanges(), videoView, getContext(), gameState.BPM);

            fps = 0d;
            setFocusable(true);
            paint = new Paint();
            paint.setTextSize(30);
            //-- Metrics of player
            Point size = Common.Companion.getSize(getContext());
            playerSizeX = size.x;
            //verify if landscape
            //16:9
            //playerSizeY = (int) (size.x*0.5625d);
            //4:3
            playerSizeY = (int) (size.x * 0.75d);


            paint.setColor(Color.WHITE);
            try {
                mpMusic.setDataSource(stepData.getMusicPath());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    //   mpMusic.setPlaybackParams(mpMusic.getPlaybackParams().setSpeed(ParamsSong.rush));// Esto será para el rush
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //steps

            mpMusic.prepare();
            mpMusic.setOnCompletionListener(mp -> stop());
            mpMusic.setOnPreparedListener(mp -> startGame());

            stepsDrawer = new StepsDrawer(getContext(), stepData.getStepType(), "16:9", isLandScape, sizeScreen);

            //match video whit stepDrawer
            videoView.getLayoutParams().height = stepsDrawer.sizeY + stepsDrawer.offsetY;
            videoView.getLayoutParams().width = stepsDrawer.sizeX;

            //lifeBar
            bar = new LifeBar(context, stepsDrawer);

            combo = new Combo(getContext(), stepsDrawer);
            touchPad = new GamePad(context, stepData.getStepType(), gameState.inputs, sizeScreen.x, size.y);

            gameState.setCombo(combo);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        gameState.reset();
        mainTread.setRunning(true);
        mainTread.start();
    }

    public void startGame() {
        gameState.start();
        try {
            if (mainTread.running) {
                if (gameState.offset > 0) {
                    bgPlayer.start(gameState.currentBeat);
                    handler1.postDelayed(musicRun, (long) (gameState.offset * 1000));
                } else {
                    mpMusic.seekTo((int) Math.abs(gameState.offset * 1000));
                    mpMusic.setOnPreparedListener(mp -> {
                        mpMusic.start();
                        gameState.isRunning = true;
                    });
                    bgPlayer.start(gameState.currentBeat);
                    mpMusic.prepare();
                }
            } else
                mainTread.sulrfaceHolder = this.getHolder();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        //se limpia la pantalla
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        try {
            //speed calc
            double avAuxValue = (gameState.initialBPM); //example BPM 200 ;
            speed = (int) (stepsDrawer.sizeNote / avAuxValue * 580);//580 av
            double lastScrollAux = gameState.lastScroll;
            double lastBeat = this.gameState.currentBeat + 0;
            double lastPosition = stepsDrawer.sizeNote * 0.7;
            ArrayList<GameRow> list = new ArrayList<>();
            if (gameState.isRunning) {
                drawStats(canvas);
                for (int x = 0; (gameState.currentElement + x) < gameState.steps.size(); x++) {
                    GameRow currentElemt = gameState.steps.get(gameState.currentElement + x);
                    double diffBeats = currentElemt.getCurrentBeat() - lastBeat;
                    lastPosition += diffBeats * speed * gameState.currentSpeedMod * lastScrollAux;
                    if (lastPosition >= stepsDrawer.sizeY + stepsDrawer.sizeNote / 3)
                        break;
                    if (currentElemt.getNotes() != null) {
//                        stepsDrawer.draw(canvas, currentElemt.getNotes(), (int) lastPosition);
                        currentElemt.setPosY((int) lastPosition);
                        list.add(currentElemt);
                    }
                    if (currentElemt.getModifiers() != null && currentElemt.getModifiers().get("SCROLLS") != null)
                        lastScrollAux = Objects.requireNonNull(currentElemt.getModifiers().get("SCROLLS")).get(1);
                    lastBeat = currentElemt.getCurrentBeat();
                }
                stepsDrawer.draw(canvas, list);
            }
            bar.draw(canvas);
            combo.draw(canvas);


            if (!isLandScape)
                canvas.drawRect(new Rect(0, stepsDrawer.sizeY, stepsDrawer.offsetX + stepsDrawer.sizeX, stepsDrawer.sizeY * 2), clearPaint);

            //touchPad.draw(canvas);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update() {
        gameState.update();
        combo.update();
        if (gameState.isRunning) {
            stepsDrawer.update();
            bgPlayer.update(gameState.currentBeat);
            bar.updateLife(50);
        }

    }

    public void drawStats(Canvas c) {
        paint.setTextSize(20);
        paint.setStyle(Paint.Style.FILL);
        c.drawPaint(paint);
        paint.setTextSize(25);
        paint.setColor(Color.WHITE);
        //  c.drawText("::: " + msj, 0, 20, paint);
        c.drawText("FPS: " + fps, 0, 250, paint);
        // c.drawText("Log: " + gameState.currentTickCount, 0, 100, paint);
        //c//.drawText("event: " + testFloatNOTUSE, 0, playerSizeY - 200, paint);
        //c.drawText("C Seg: " + String.format(new Locale("es"), "%.3f", gameState.currentSecond), 0, playerSizeY - 300, paint);
        c.drawText("C Beat: " + String.format(new Locale("es"), "%.3f", gameState.currentBeat), 0, playerSizeY - 150, paint);
        c.drawText("C BPM: " + gameState.BPM, 0, playerSizeY - 250, paint);
        c.drawText("C Speed: " + gameState.currentSpeedMod, 0, playerSizeY - 100, paint);
        c.drawText("Scroll: " + gameState.lastScroll, 0, playerSizeY - 400, paint);
        StringBuilder st = new StringBuilder();
        for (int j = 0; j < 10; j++)
            st.append(gameState.inputs[j]);
          c.drawText("pad: "+st, playerSizeX - 250, playerSizeY - 20, paint);
        //  paint.setColor(Color.BLACK);
        paint.setColor(Color.TRANSPARENT);
    }


    public void stop() {
        boolean retry = true;
        if (mainTread != null)
            mainTread.setRunning(false);
        while (retry) {
            try {
                if (mainTread != null)
                    mainTread.setRunning(false);
                releaseMediaPlayer();
                retry = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void releaseMediaPlayer() {
        try {
            if (mpMusic != null) {
                if (mpMusic.isPlaying())
                    mpMusic.stop();
                mpMusic.release();
                mpMusic = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        try {
            int maskedAction = event.getActionMasked();
            int fingers = event.getPointerCount();
            //this.fingersOnScreen = (byte) fingers;
            int[][] inputsTouch = new int[fingers][2];
            for (int i = 0; i < fingers; i++) {
                inputsTouch[i][0] = (int) event.getX(i);
                inputsTouch[i][1] = (int) event.getY(i);
//                this.event += " " + i + ":(" + (int) event.getX(i) + "," + (int) event.getY(i) + ")";
            }
            switch (maskedAction) {
                case MotionEvent.ACTION_POINTER_UP:
                    int actionIndex = event.getPointerId(event.getActionIndex());
                    this.touchPad.unpress(event.getX(actionIndex), event.getY(actionIndex));
                    break;
                case MotionEvent.ACTION_DOWN:
                    if (event.getX() > playerSizeX / 2 && event.getY() < playerSizeY / 2) {
                        speed += 10;
                    }
                    else if (event.getX() < playerSizeX / 2 && event.getY() < playerSizeY / 2) {
                    if (speed > 99)
                        speed += 10;
                } else if (event.getX() < playerSizeX / 2 && event.getY() > playerSizeY / 2 && event.getY() < playerSizeY) {
                    //ParamsSong.autoplay = !ParamsSong.autoplay;
                } else if (event.getX() > playerSizeX / 2 && event.getY() > playerSizeY / 2 && event.getY() < playerSizeY) {
                    //    steps.doMagic = !steps.doMagic;
                }
                    touchPad.checkInputs(inputsTouch, true);
                default:
                    //   this.event = "numero" + maskedAction;
                    this.touchPad.checkInputs(inputsTouch, false);
                    break;
                case MotionEvent.ACTION_UP:
                    if (fingers == 1)
                        touchPad.clearPad();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

}
