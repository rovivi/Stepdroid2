package com.kyagamy.step.game.newplayer;

import android.graphics.Color;

public final class GameConstants {
    // Display and refresh rate
    public static final int DEFAULT_REFRESH_RATE = 60;
    public static final double ASPECT_RATIO_4_3 = 0.75d;
    public static final double ASPECT_RATIO_16_9 = 0.5625d;

    // Game speed and timing
    public static final double SPEED_MULTIPLIER = 0.9f;
    public static final double NOTE_POSITION_FACTOR = 0.7;
    public static final int SPEED_INCREMENT = 10;
    public static final int MIN_SPEED = 99;
    public static final int NOTE_SIZE_MULTIPLIER = 2;

    // Audio synchronization
    public static final double AUDIO_SYNC_THRESHOLD = 0.001;
    public static final double AUDIO_SYNC_DIFF_THRESHOLD = 0.04;
    public static final int SOUNDPOOL_MAX_STREAMS = 25;

    // Colors
    public static final int[] STEP_COLORS = {
            Color.BLUE, Color.RED, Color.YELLOW, Color.RED, Color.BLUE,
            Color.BLUE, Color.RED, Color.YELLOW, Color.RED, Color.BLUE
    };

    // UI sizing
    public static final int DEFAULT_PLAYER_SIZE_X = 400;
    public static final int DEFAULT_PLAYER_SIZE_Y = 500;
    public static final int DEBUG_TEXT_SIZE = 25;
    public static final int DEBUG_TEXT_SIZE_SMALL = 20;
    public static final int DEBUG_TEXT_OFFSET_X = 250;
    public static final int DEBUG_TEXT_OFFSET_Y = 20;

    // Screen orientations
    public static final String SCREEN_RATIO_16_9 = "16:9";
    public static final String SCREEN_RATIO_4_3 = "4:3";

    // Input handling
    public static final int MAX_INPUT_FINGERS = 10;

    // Time calculations
    public static final double SECONDS_TO_MILLISECONDS = 1000d;
    public static final double MILLISECONDS_TO_SECONDS = 1000d;
    public static final int TIME_PRECISION_FACTOR = 100;

    private GameConstants() {
        // Prevent instantiation
    }
}