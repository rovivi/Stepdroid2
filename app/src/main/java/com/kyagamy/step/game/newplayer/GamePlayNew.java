package com.kyagamy.step.game.newplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.Display;
import android.view.WindowManager;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.VideoView;

import com.kyagamy.step.views.gameplayactivity.GamePlayActivity;
import com.kyagamy.step.R;
import com.kyagamy.step.common.Common;
import com.kyagamy.step.common.step.CommonGame.ParamsSong;
import com.kyagamy.step.common.step.Game.GameRow;
import com.kyagamy.step.game.newplayer.GameConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import game.StepObject;

public class GamePlayNew extends SurfaceView implements SurfaceHolder.Callback {

    // Core game components
    public MainThreadNew mainThread;
    private GameState gameState;
    private MediaPlayer musicPlayer;
    private GameRenderer renderer;

    // Game elements
    private StepsDrawer stepsDrawer;
    private LifeBar bar;
    private Combo combo;
    private GamePad touchPad;
    private BgPlayer bgPlayer;

    // Display and configuration
    private int playerSizeX = GameConstants.DEFAULT_PLAYER_SIZE_X;
    private int playerSizeY = GameConstants.DEFAULT_PLAYER_SIZE_Y;
    private boolean isLandScape = false;
    private int refreshRate;

    // Audio
    public static SoundPool soundPool;
    public static int soundPullBeat;
    public static int soundPullMine;

    // Performance optimizations
    private final ArrayList<GameRow> drawList = new ArrayList<>();
    private Paint debugPaint;
    private boolean musicPlayerUpdated = false;
    private double audioVideoSyncValue = 100;

    // Game state
    public Double fps;
    private Handler handler = new Handler();
    private GamePlayActivity gamePlayActivity;

    private int speed;
    private String debugMessage;

    public GamePlayNew(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializePaints();
        refreshRate = getDisplayRefreshRate(context);
    }

    private void initializePaints() {
        debugPaint = new Paint();
        debugPaint.setTextSize(GameConstants.DEBUG_TEXT_SIZE);
        debugPaint.setColor(Color.WHITE);
        debugPaint.setStyle(Paint.Style.FILL);
    }

    private static int getDisplayRefreshRate(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Display display = context.getDisplay();
            if (display != null) {
                return Math.round(display.getRefreshRate());
            }
        }
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            Display d = wm.getDefaultDisplay();
            return Math.round(d.getRefreshRate());
        }
        return GameConstants.DEFAULT_REFRESH_RATE;
    }

    private final Runnable musicStartRunnable = new Runnable() {
        @Override
        public void run() {
            if (musicPlayer != null) {
                musicPlayer.start();
                gameState.isRunning = true;
            }
        }
    };

    public void startGamePLay(VideoView videoView, StepObject stepData, Context context, Point sizeScreen, GamePlayActivity gamePlayActivity, byte[] inputs) {
        try {
            this.gamePlayActivity = gamePlayActivity;
            isLandScape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            setupSurface();
            initializeGameComponents(stepData, inputs, context, sizeScreen, videoView);
            setupAudio(stepData);
            setupVideoView(videoView);
            initializeSoundPool();

            audioVideoSyncValue = stepData.getDisplayBPM();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupSurface() {
        this.setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSPARENT);
        getHolder().addCallback(this);
    }

    private void initializeGameComponents(StepObject stepData, byte[] inputs, Context context, Point sizeScreen, VideoView videoView) {
        gameState = new GameState(stepData, inputs);
        gameState.reset();

        mainThread = new MainThreadNew(getHolder(), this, refreshRate);
        mainThread.setRunning(true);

        bgPlayer = new BgPlayer(stepData.getPath(), stepData.getBgChanges(), videoView, getContext(), gameState.BPM);
        fps = 0d;
        setFocusable(true);

        calculatePlayerSize(context);

        stepsDrawer = new StepsDrawer(getContext(), stepData.getStepType(), "16:9", isLandScape, sizeScreen);
        bar = new LifeBar(context, stepsDrawer);
        combo = new Combo(getContext(), stepsDrawer);
        touchPad = new GamePad(context, stepData.getStepType(), gameState.inputs, sizeScreen.x, Common.Companion.getSize(getContext()).y);
        touchPad.setGamePlayNew(this);

        combo.setLifeBar(bar);
        gameState.combo = combo;
        gameState.stepsDrawer = stepsDrawer;

        renderer = new GameRenderer(stepsDrawer, bar, combo, debugPaint, isLandScape);
    }

    private void calculatePlayerSize(Context context) {
        Point size = Common.Companion.getSize(context);
        playerSizeX = size.x;
        playerSizeY = (int) (size.x * GameConstants.ASPECT_RATIO_4_3); // 4:3 aspect ratio
    }

    private void setupAudio(StepObject stepData) {
        try {
            musicPlayer = new MediaPlayer();
            musicPlayer.setDataSource(stepData.getMusicPath());
            musicPlayer.prepare();
            musicPlayer.setOnCompletionListener(mp -> startEvaluation());
            musicPlayer.setOnPreparedListener(mp -> startGame());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupVideoView(VideoView videoView) {
        if (videoView != null) {
            videoView.getLayoutParams().height = stepsDrawer.sizeY + stepsDrawer.offsetY;
            videoView.getLayoutParams().width = stepsDrawer.sizeX;
        }
    }

    private void initializeSoundPool() {
        soundPool = new SoundPool.Builder()
                .setMaxStreams(GameConstants.SOUNDPOOL_MAX_STREAMS)
                .build();

        soundPullBeat = soundPool.load(this.getContext(), R.raw.beat2, 1);
        soundPullMine = soundPool.load(this.getContext(), R.raw.mine, 1);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        gameState.reset();
        mainThread.setRunning(true);
        mainThread.start();
    }

    public void startGame() {
        Evaluator.Companion.resetScore();
        musicPlayer.setOnCompletionListener(mp -> startEvaluation());

        gameState.start();
        try {
            if (mainThread.running) {
                if (gameState.offset > 0) {
                    bgPlayer.start(gameState.currentBeat);
                    handler.postDelayed(musicStartRunnable, (long) (gameState.offset * 1000));
                } else {
                    musicPlayer.seekTo((int) Math.abs(gameState.offset * 1000));
                    musicPlayer.setOnPreparedListener(mp -> {
                        musicPlayer.start();
                        gameState.isRunning = true;
                    });
                    bgPlayer.start(gameState.currentBeat);
                }
            }
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
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        try {
            if (gameState.isRunning) {
                calculateSpeed();
                drawList.clear();
                calculateVisibleNotes();
                renderer.drawGame(canvas, drawList, gameState, speed);
                renderer.drawDebugInfo(canvas, gameState, musicPlayer, fps, speed, musicPlayerUpdated, playerSizeX, playerSizeY);

                if (gameState.currentElement + 1 == gameState.steps.size()) {
                    startEvaluation();
                }
            }

            renderer.drawUI(canvas);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void calculateSpeed() {
        speed = (int) ((stepsDrawer.sizeNote / audioVideoSyncValue * ParamsSong.av) * GameConstants.SPEED_MULTIPLIER);
    }

    private void calculateVisibleNotes() {
        double lastScrollAux = gameState.lastScroll;
        double lastBeat = gameState.currentBeat;
        double lastPosition = stepsDrawer.sizeNote * GameConstants.NOTE_POSITION_FACTOR;

        int initialIndex = findInitialVisibleIndex(lastScrollAux, lastBeat, lastPosition);
        populateVisibleNotes(initialIndex);
    }

    private int findInitialVisibleIndex(double lastScrollAux, double lastBeat, double lastPosition) {
        int initialIndex = 0;
        double currentPosition = lastPosition;
        double currentBeat = lastBeat;

        for (int x = 0; (gameState.currentElement + x) >= 0 && lastScrollAux != 0; x--) {
            GameRow currentElement = gameState.steps.get(gameState.currentElement + x);
            double diffBeats = currentElement.getCurrentBeat() - currentBeat;
            currentPosition += diffBeats * speed * gameState.currentSpeedMod * lastScrollAux;
            if (currentPosition < -stepsDrawer.sizeNote * GameConstants.NOTE_SIZE_MULTIPLIER) break;
            currentBeat = currentElement.getCurrentBeat();
            initialIndex = x;
        }
        return initialIndex;
    }

    private void populateVisibleNotes(int initialIndex) {
        double lastScrollAux = gameState.lastScroll;
        double lastBeat = gameState.currentBeat;
        double lastPosition = stepsDrawer.sizeNote * GameConstants.NOTE_POSITION_FACTOR;

        for (int x = initialIndex; (gameState.currentElement + x) < gameState.steps.size() &&
                (gameState.currentElement + x) >= 0; x++) {
            GameRow currentElement = gameState.steps.get(gameState.currentElement + x);
            double diffBeats = currentElement.getCurrentBeat() - lastBeat;
            lastPosition += diffBeats * speed * gameState.currentSpeedMod * lastScrollAux;

            if (currentElement.getNotes() != null) {
                currentElement.setPosY((int) lastPosition);
                drawList.add(currentElement);
            }

            if (lastPosition >= stepsDrawer.sizeY + stepsDrawer.sizeNote) break;

            if (currentElement.getModifiers() != null &&
                    currentElement.getModifiers().get("SCROLLS") != null && x >= 0) {
                lastScrollAux = Objects.requireNonNull(currentElement.getModifiers().get("SCROLLS")).get(1);
            }
            lastBeat = currentElement.getCurrentBeat();
        }
    }

    public void update() {
        gameState.update();
        combo.update();

        if (gameState.isRunning) {
            stepsDrawer.update();
            bgPlayer.update(gameState.currentBeat);
            bar.update();
            syncAudioVideo();
        }
    }

    private void syncAudioVideo() {
        if (!musicPlayerUpdated) {
            double diff = ((gameState.currentSecond / 100d) - gameState.offset - musicPlayer.getCurrentPosition() / 1000d);
            if (Math.abs(diff) < GameConstants.AUDIO_SYNC_THRESHOLD) {
                musicPlayerUpdated = true;
            }

            if (diff >= GameConstants.AUDIO_SYNC_DIFF_THRESHOLD && !musicPlayerUpdated &&
                    gameState.isRunning && musicPlayer.isPlaying()) {
                gameState.currentBeat -= Common.Companion.second2Beat(diff, gameState.BPM);
                gameState.currentSecond -= diff * 100;
            }
        }
    }

    private void startEvaluation() {
        stop();
        if (gamePlayActivity != null) {
            gamePlayActivity.startEvaluation();
            gamePlayActivity.finish();
        }
    }

    public void stop() {
        boolean retry = true;
        if (mainThread != null) {
            mainThread.setRunning(false);
        }

        while (retry) {
            try {
                if (mainThread != null) {
                    mainThread.setRunning(false);
                }
                releaseResources();
                retry = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void releaseResources() {
        releaseMusicPlayer();
        releaseSoundPool();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private void releaseMusicPlayer() {
        try {
            if (musicPlayer != null) {
                if (musicPlayer.isPlaying()) {
                    musicPlayer.stop();
                }
                musicPlayer.release();
                musicPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseSoundPool() {
        try {
            if (soundPool != null) {
                soundPool.release();
                soundPool = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        try {
            int maskedAction = event.getActionMasked();
            int fingers = event.getPointerCount();
            int[][] inputsTouch = new int[fingers][2];

            for (int i = 0; i < fingers; i++) {
                inputsTouch[i][0] = (int) event.getX(i);
                inputsTouch[i][1] = (int) event.getY(i);
            }

            switch (maskedAction) {
                case MotionEvent.ACTION_POINTER_UP:
                    int actionIndex = event.getPointerId(event.getActionIndex());
                    touchPad.unpress(event.getX(actionIndex), event.getY(actionIndex));
                    break;
                case MotionEvent.ACTION_DOWN:
                    handleDebugTouches(event);
                    touchPad.checkInputs(inputsTouch, true);
                    break;
                default:
                    touchPad.checkInputs(inputsTouch, false);
                    break;
                case MotionEvent.ACTION_UP:
                    if (fingers == 1) {
                        touchPad.clearPad();
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void handleDebugTouches(MotionEvent event) {
        if (event.getX() > playerSizeX / 2 && event.getY() < playerSizeY / 2) {
            speed += GameConstants.SPEED_INCREMENT;
        } else if (event.getX() < playerSizeX / 2 && event.getY() < playerSizeY / 2) {
            if (speed > GameConstants.MIN_SPEED) {
                speed -= GameConstants.SPEED_INCREMENT;
            }
        }
    }

    public GamePad getTouchPad() {
        return touchPad;
    }

    public void notifyPadStateChanged() {
        if (gamePlayActivity != null) {
            gamePlayActivity.syncPadState();
        }
    }
}
