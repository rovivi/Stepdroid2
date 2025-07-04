package com.kyagamy.step.game.newplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import com.kyagamy.step.common.step.CommonGame.CustomSprite.SpriteReader;
import com.kyagamy.step.common.step.CommonSteps;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Objects;

import com.kyagamy.step.common.step.Game.GameRow;

import game.Note;

import static com.kyagamy.step.common.step.CommonSteps.NOTE_LONG_PRESSED;
import static com.kyagamy.step.common.step.Game.GameRowKt.NOT_DRAWABLE;

public class StepsDrawer {

    // Constants
    private static final int NOT_USED = -999;
    private static final float STEPS_Y_COUNT = 9.3913f;
    private static final float RECEPTOR_Y_FACTOR = 0.7f;
    private static final float NOTE_SCALE_FACTOR = 1.245f;
    private static final float SCREEN_WIDTH_FACTOR = 0.1f;
    private static final float ASPECT_RATIO_4_3 = 0.75f;
    private static final float ASPECT_RATIO_16_9 = 0.5625f;
    private static final float ASPECT_RATIO_16_9_CALC = 1.77777778f;
    private static final float LONG_NOTE_BODY_OFFSET = 0.35f;
    private static final int LONG_NOTE_TAIL_OFFSET_DIVISOR = 3;
    private static final float DEBUG_TEXT_SIZE = 20f;

    // Enums
    public enum GameMode {
        PUMP_ROUTINE("pump-routine", 10),
        PUMP_DOUBLE("pump-double", 10),
        PUMP_HALFDOUBLE("pump-halfdouble", 10),
        PUMP_SINGLE("pump-single", 5),
        DANCE_SINGLE("dance-single", 4),
        EMPTY("", 0);

        private final String value;
        private final int steps;

        GameMode(String value, int steps) {
            this.value = value;
            this.steps = steps;
        }

        public String getValue() {
            return value;
        }

        public int getSteps() {
            return steps;
        }

        public static GameMode fromString(String value) {
            for (GameMode mode : values()) {
                if (mode.value.equals(value)) {
                    return mode;
                }
            }
            return EMPTY;
        }
    }

    public enum SkinType {
        SELECTED, ROUTINE0, ROUTINE1, ROUTINE2, ROUTINE3
    }

    // Fields
    protected int sizeX;
    protected int sizeY;
    protected int sizeNote;
    protected int scaledNoteSize;
    protected int offsetX = 0;
    protected int offsetY = 0;
    protected int posInitialX;
    protected int startValueY;

    private GameMode gameMode;
    private EnumMap<SkinType, NoteSkin> noteSkins;
    private int[] lastPositionDraw;

    // Reusable objects to avoid garbage collection
    private Paint debugPaint;
    private Rect drawRect;

    /**
     * Created the step drawer
     */
    StepsDrawer(Context context, String gameModeStr, String aspectRatio, boolean landScape, Point screenSize) {
        this.gameMode = GameMode.fromString(gameModeStr);
        initializeReusableObjects();
        calculateDimensions(aspectRatio, landScape, screenSize);
        initializeNoteSkins(context);
        initializeDrawingValues();
    }

    private void initializeReusableObjects() {
        debugPaint = new Paint();
        debugPaint.setColor(Color.WHITE);
        debugPaint.setStyle(Paint.Style.FILL);
        debugPaint.setTextSize(DEBUG_TEXT_SIZE);

        drawRect = new Rect();
        lastPositionDraw = new int[10];
        noteSkins = new EnumMap<>(SkinType.class);
    }

    private void calculateDimensions(String aspectRatio, boolean landScape, Point screenSize) {
        posInitialX = (int) (screenSize.x * SCREEN_WIDTH_FACTOR);

        float relationAspectValue = aspectRatio.contains("4:3") ? ASPECT_RATIO_4_3 : ASPECT_RATIO_16_9;

        if (landScape) {
            calculateLandscapeDimensions(screenSize);
        } else {
            calculatePortraitDimensions(screenSize);
        }

        sizeNote = (int) (sizeY / STEPS_Y_COUNT);
        scaledNoteSize = (int) (sizeNote * NOTE_SCALE_FACTOR);
        posInitialX = (((sizeX) - (sizeNote * gameMode.getSteps()))) / 2 + offsetX / 2;
    }

    private void calculateLandscapeDimensions(Point screenSize) {
        sizeY = screenSize.y;
        sizeX = (int) (screenSize.y * ASPECT_RATIO_16_9_CALC);
        offsetX = (int) ((screenSize.x - sizeX) / 2f);

        if (sizeX > screenSize.x) {
            sizeY = (int) (screenSize.x / ASPECT_RATIO_16_9_CALC);
            sizeX = (int) (sizeY * ASPECT_RATIO_16_9_CALC);
            offsetX = Math.abs((int) ((screenSize.x - sizeX) / 2f));
            offsetY = (int) ((screenSize.y - sizeY) / 2f);
        }

        sizeX += offsetX / 2;
        sizeY += offsetY;
    }

    private void calculatePortraitDimensions(Point screenSize) {
        sizeY = screenSize.y / 2;
        sizeX = screenSize.x;

        if ((int) (sizeY / STEPS_Y_COUNT) * gameMode.getSteps() > sizeX) {
            sizeY = (int) (sizeX / (gameMode.getSteps() + 0.2) * STEPS_Y_COUNT);
            offsetY = screenSize.y - sizeY;
        }
    }

    private void initializeNoteSkins(Context context) {
        switch (gameMode) {
            case PUMP_ROUTINE:
                noteSkins.put(SkinType.ROUTINE0, new NoteSkin(context, gameMode.getValue(), "routine1"));
                noteSkins.put(SkinType.ROUTINE1, new NoteSkin(context, gameMode.getValue(), "routine2"));
                noteSkins.put(SkinType.ROUTINE2, new NoteSkin(context, gameMode.getValue(), "routine3"));
                noteSkins.put(SkinType.ROUTINE3, new NoteSkin(context, gameMode.getValue(), "soccer"));
                break;
            case PUMP_DOUBLE:
            case PUMP_HALFDOUBLE:
            case PUMP_SINGLE:
                noteSkins.put(SkinType.SELECTED, new NoteSkin(context, gameMode.getValue(), "prime"));
                break;
            case DANCE_SINGLE:
            case EMPTY:
                break;
        }
    }

    private void initializeDrawingValues() {
        startValueY = (int) (sizeNote * RECEPTOR_Y_FACTOR);
        resetLastPositionDraw();
    }

    private void resetLastPositionDraw() {
        for (int i = 0; i < lastPositionDraw.length; i++) {
            lastPositionDraw[i] = NOT_USED;
        }
    }

    public void draw(Canvas canvas, ArrayList<GameRow> listRow) {
        resetLastPositionDraw();

        drawReceptors(canvas);
        drawNotes(canvas, listRow);
        drawEffects(canvas);
    }

    private void drawReceptors(Canvas canvas) {
        NoteSkin selectedSkin = noteSkins.get(SkinType.SELECTED);
        if (selectedSkin == null) return;

        for (int j = 0; j < selectedSkin.receptors.length; j++) {
            int startNoteX = posInitialX + sizeNote * j;
            setDrawRect(startNoteX, startValueY, startNoteX + scaledNoteSize, startValueY + scaledNoteSize);
            selectedSkin.receptors[j].draw(canvas, drawRect);
        }
    }

    private void drawNotes(Canvas canvas, ArrayList<GameRow> listRow) {
        for (GameRow gameRow : listRow) {
            ArrayList<Note> notes = gameRow.getNotes();
            short count = 0;

            for (Note note : notes) {
                if (note != null && note.getType() != CommonSteps.NOTE_EMPTY) {
                    drawSingleNote(canvas, note, gameRow, count);
                }
                count++;
            }
        }
    }

    private void drawSingleNote(Canvas canvas, Note note, GameRow gameRow, int columnIndex) {
        NoteSkin selectedSkin = noteSkins.get(SkinType.SELECTED);
        if (selectedSkin == null) return;

        int startNoteX = posInitialX + sizeNote * columnIndex;
        int endNoteX = startNoteX + scaledNoteSize;
        SpriteReader currentArrow = null;

        switch (note.getType()) {
            case CommonSteps.NOTE_TAP:
            case CommonSteps.NOTE_FAKE:
                currentArrow = selectedSkin.arrows[columnIndex];
                break;
            case CommonSteps.NOTE_LONG_START:
                drawLongNote(canvas, note, gameRow, startNoteX, endNoteX, columnIndex, selectedSkin);
                break;
            case CommonSteps.NOTE_LONG_BODY:
                drawLongNoteBody(canvas, note, gameRow, startNoteX, endNoteX, columnIndex, selectedSkin);
                break;
            case CommonSteps.NOTE_MINE:
                currentArrow = selectedSkin.mine;
                break;
        }

        if (currentArrow != null) {
            setDrawRect(startNoteX, gameRow.getPosY(), endNoteX, gameRow.getPosY() + scaledNoteSize);
            currentArrow.draw(canvas, drawRect);
        }
    }

    private void drawLongNote(Canvas canvas, Note note, GameRow gameRow, int startNoteX, int endNoteX, int columnIndex, NoteSkin skin) {
        int endY = (Objects.requireNonNull(note.getRowEnd()).getPosY() == NOT_DRAWABLE) ? sizeY : note.getRowEnd().getPosY();
        lastPositionDraw[columnIndex] = endY + scaledNoteSize;

        // Draw long note body
        setDrawRect(startNoteX, gameRow.getPosY() + ((int) (scaledNoteSize * LONG_NOTE_BODY_OFFSET)),
                endNoteX, endY + scaledNoteSize / LONG_NOTE_TAIL_OFFSET_DIVISOR);
        skin.longs[columnIndex].draw(canvas, drawRect);

        // Draw start arrow
        setDrawRect(startNoteX, gameRow.getPosY(), endNoteX, gameRow.getPosY() + scaledNoteSize);
        skin.arrows[columnIndex].draw(canvas, drawRect);

        // Draw tail if end exists
        if (Objects.requireNonNull(note.getRowEnd()).getPosY() != NOT_DRAWABLE) {
            setDrawRect(startNoteX, endY, endNoteX, endY + scaledNoteSize);
            skin.tails[columnIndex].draw(canvas, drawRect);
        }
    }

    private void drawLongNoteBody(Canvas canvas, Note note, GameRow gameRow, int startNoteX, int endNoteX, int columnIndex, NoteSkin skin) {
        if (gameRow.getPosY() > lastPositionDraw[columnIndex]) {
            int startY = gameRow.getPosY();
            if (gameRow.getPosY() > startValueY && gameRow.getPosY() < sizeY) {
                startY = startValueY;
            }

            int endY = (Objects.requireNonNull(note.getRowEnd()).getPosY() == NOT_DRAWABLE) ? sizeY : note.getRowEnd().getPosY();
            lastPositionDraw[columnIndex] = endY;

            // Draw long note body
            setDrawRect(startNoteX, startY + ((int) (scaledNoteSize * LONG_NOTE_BODY_OFFSET)),
                    endNoteX, endY + scaledNoteSize / LONG_NOTE_TAIL_OFFSET_DIVISOR);
            skin.longs[columnIndex].draw(canvas, drawRect);

            // Draw arrow
            setDrawRect(startNoteX, startY, endNoteX, startY + scaledNoteSize);
            skin.arrows[columnIndex].draw(canvas, drawRect);

            // Draw tail if end exists
            if (Objects.requireNonNull(note.getRowEnd()).getPosY() != NOT_DRAWABLE) {
                setDrawRect(startNoteX, endY, endNoteX, endY + scaledNoteSize);
                skin.tails[columnIndex].draw(canvas, drawRect);
            }
        }
    }

    private void drawEffects(Canvas canvas) {
        NoteSkin selectedSkin = noteSkins.get(SkinType.SELECTED);
        if (selectedSkin == null) return;

        for (int j = 0; j < selectedSkin.arrows.length; j++) {
            int startNoteX = posInitialX + sizeNote * j;
            int endNoteX = startNoteX + scaledNoteSize;

            setDrawRect(startNoteX, startValueY, endNoteX, startValueY + scaledNoteSize);
            selectedSkin.explotions[j].staticDraw(canvas, drawRect);
            selectedSkin.explotionTails[j].draw(canvas, drawRect);
            selectedSkin.tapsEffect[j].staticDraw(canvas, drawRect);
        }
    }

    private void setDrawRect(int left, int top, int right, int bottom) {
        drawRect.set(left, top, right, bottom);
    }

    public void update() {
        for (NoteSkin currentNote : noteSkins.values()) {
            updateNoteSkin(currentNote);
        }
    }

    private void updateNoteSkin(NoteSkin noteSkin) {
        for (int x = 0; x < noteSkin.arrows.length; x++) {
            noteSkin.arrows[x].update();
            noteSkin.tails[x].update();
            noteSkin.longs[x].update();
            noteSkin.explotions[x].update();
            noteSkin.explotionTails[x].update();
            noteSkin.tapsEffect[x].update();
            noteSkin.receptors[x].update();
        }
        noteSkin.mine.update();
    }

    public int getStepsByGameMode() {
        return gameMode.getSteps();
    }

    public NoteSkin getNoteSkin(SkinType skinType) {
        return noteSkins.get(skinType);
    }

    public NoteSkin getSelectedSkin() {
        return noteSkins.get(SkinType.SELECTED);
    }
}
