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
import java.util.Objects;

import com.kyagamy.step.common.step.Game.GameRow;

import game.Note;

import static com.kyagamy.step.common.step.CommonSteps.NOTE_LONG_PRESSED;
import static com.kyagamy.step.common.step.Game.GameRowKt.NOT_DRAWABLE;

public class StepsDrawer {

private int NON_USE = -999;
    //Constantes de clases
    private static byte SELECTED_SKIN = 0;
    private static byte ROUTINE0_SKIN = 1;
    private static byte ROUTINE1_SKIN = 3;
    private static byte ROUTINE2_SKIN = 4;
    private static byte ROUTINE3_SKIN = 5;
    private static float NUMBER_OF_Y_STEPS = 9.3913f;


    protected int sizeX;
    protected int sizeY;

    protected int sizeNote;
    protected int offsetX = 0;//game
    protected int offsetY = 0;
    private int sizeArrows = 1;//alto de la pantalla entre 9.3913

    protected int posInitialX;


    static private int[][] longInfo;
    static private int[] noteSkin = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private String gameMode;

    static {
        longInfo = new int[2][10];
        longInfo[1] = noteSkin;
    }

   public  static NoteSkin[] noteSkins;

    /**
     * Created the step
     *
     * @param context
     * @param gameMode
     */
    StepsDrawer(Context context, String gameMode, String aspectRatio, boolean landScape, Point screenSize) {
        //que tipo de tablero y nivel es aqui se tiene que calcular las medidas necesarias
//        Point screenSize =Common.Companion.getSize(context);
        this.gameMode = gameMode;
        posInitialX = (int) (screenSize.x * 0.1);
        longInfo[1] = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        noteSkins = new NoteSkin[1];


        float relationAspectValue = aspectRatio.contains("4:3") ? 0.75f : 0.5625f;

        if (landScape) {
            sizeY = screenSize.y;
            sizeX = (int) (screenSize.y * 1.77777778d);
            offsetX = (int) ((screenSize.x - sizeX) / 2f);
            if (sizeX > screenSize.x) {
                sizeY = (int) (screenSize.x / 1.7777777f);
                sizeX = (int) (sizeY * 1.77777778d);
                offsetX = Math.abs((int) ((screenSize.x - sizeX) / 2f));
                offsetY = (int) ((screenSize.y - sizeY) / 2f);
            }
            sizeX += offsetX / 2;
            sizeY += offsetY;
        } else {
            sizeY = screenSize.y / 2;
            sizeX = screenSize.x;
            if ((int) (sizeY / NUMBER_OF_Y_STEPS) * getStepsByGameMode() > sizeX) {
                sizeY = (int) (sizeX / (getStepsByGameMode() + 0.2) * NUMBER_OF_Y_STEPS);
                offsetY = screenSize.y - sizeY;
            }

        }
        sizeNote = (int) (sizeY / NUMBER_OF_Y_STEPS);


        switch (gameMode) {
            case "pump-routine":
                noteSkins = new NoteSkin[4];
                noteSkins[ROUTINE0_SKIN] = new NoteSkin(context, gameMode, "routine1");
                noteSkins[ROUTINE1_SKIN] = new NoteSkin(context, gameMode, "routine2");
                noteSkins[ROUTINE2_SKIN] = new NoteSkin(context, gameMode, "routine3");
                noteSkins[ROUTINE3_SKIN] = new NoteSkin(context, gameMode, "soccer");
                break;
            case "pump-double":
            case "pump-halfdouble":
            case "pump-single":
                noteSkins[SELECTED_SKIN] = new NoteSkin(context, gameMode, "prime");
                //noteSkins[1] = new NoteSkin(context, gameMode, "routine2");
            case "dance-single":
                break;
            case "":
                break;
        }

        //calculate offset
        posInitialX = (((sizeX) - (sizeNote * getStepsByGameMode()))) / 2 + offsetX / 2;

    }


    public void draw(Canvas canvas, ArrayList<GameRow> listRow) {

        //List position
        int[] lastPositionDraw = {NON_USE,NON_USE,NON_USE,NON_USE,NON_USE,NON_USE,NON_USE,NON_USE,NON_USE};


        int startValueY = (int) (sizeNote * 0.7);

        if (true) {//FD{
            int sizeNote = (int) (this.sizeNote * 1.245);
            for (int j = 0; j < noteSkins[0].receptors.length; j++) {//se Dibujan receptores y effectos de las notas si los hay
                noteSkins[0].receptors[j].draw(canvas, new Rect(posInitialX + this.sizeNote * j, startValueY, posInitialX + this.sizeNote * (j) + sizeNote, startValueY + sizeNote));
            }
        }

        for (GameRow gameRow : listRow) {
            ArrayList<Note> notes = gameRow.getNotes();
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(20f);
            short count = 0;

            for (Note note : notes) {
                if (note != null && note.getType() != CommonSteps.NOTE_EMPTY) {
                    SpriteReader currentArrow = null;
                    int startNoteX = posInitialX + sizeNote * count;
                    int sizeNote = (int) (this.sizeNote * 1.245);
                    int endNoteX = startNoteX + sizeNote;
                    switch (note.getType()) {
                        case CommonSteps.NOTE_TAP:
                        case CommonSteps.NOTE_FAKE:
                            currentArrow = noteSkins[SELECTED_SKIN].arrows[count];
                            break;
                        case CommonSteps.NOTE_LONG_START:
                            //case CommonSteps.NOTE_LONG_BODY:
                            //first calculate end of note
                            int endY = (Objects.requireNonNull(note.getRowEnd()).getPosY() == NOT_DRAWABLE) ? sizeY : note.getRowEnd().getPosY();
                            lastPositionDraw[count] = endY+sizeNote;
                            noteSkins[SELECTED_SKIN]
                                    .longs[count].draw(canvas, new Rect(startNoteX, gameRow.getPosY() + ((int) (sizeNote * 0.35)), endNoteX, endY + sizeNote / 3));
                            noteSkins[SELECTED_SKIN].arrows[count]
                                    .draw(canvas, new Rect(startNoteX, gameRow.getPosY(), endNoteX, gameRow.getPosY() + sizeNote));
                            if (Objects.requireNonNull(note.getRowEnd()).getPosY() != NOT_DRAWABLE) {//not necessary when end not exist
                                noteSkins[SELECTED_SKIN].tails[count]
                                        .draw(canvas, new Rect(startNoteX, endY, endNoteX, endY + sizeNote));
                            }
                            break;
                        case CommonSteps.NOTE_LONG_BODY:

                            if (gameRow.getPosY() > lastPositionDraw[count]) {//VALIDAMOS que la flecha no se haya dinukjando antes
                                int startY= gameRow.getPosY();
                                if(gameRow.getPosY()>startValueY && gameRow.getPosY()<sizeY){
                                    startY=startValueY;
                                }
                                endY = (Objects.requireNonNull(note.getRowEnd()).getPosY() == NOT_DRAWABLE) ? sizeY : note.getRowEnd().getPosY();
                                lastPositionDraw[count] = endY;
                                noteSkins[SELECTED_SKIN]
                                        .longs[count].draw(canvas, new Rect(startNoteX, startY + ((int) (sizeNote * 0.35)), endNoteX, endY + sizeNote / 3));
                                noteSkins[SELECTED_SKIN].arrows[count]
                                        .draw(canvas, new Rect(startNoteX, startY, endNoteX, startY + sizeNote));
                                if (Objects.requireNonNull(note.getRowEnd()).getPosY() != NOT_DRAWABLE) {//not necessary when end not exist
                                    noteSkins[SELECTED_SKIN].tails[count]
                                            .draw(canvas, new Rect(startNoteX, endY, endNoteX, endY + sizeNote));
                                }
                            }
                            break;
                        case CommonSteps.NOTE_MINE:
                            currentArrow = noteSkins[SELECTED_SKIN].mine;
                            break;
                    }
                    if (currentArrow != null)
                        currentArrow.draw(canvas, new Rect(startNoteX, gameRow.getPosY(), endNoteX, gameRow.getPosY() + sizeNote));
                    //canvas.drawText(gameRow.getPosY()+"", startNoteX, gameRow.getPosY(), paint);
                }
                count++;
            }
        }


        for (int j = 0; j < noteSkins[0].arrows.length; j++) {//se Dibujan receptores y effectos de las notas si los hay
            int startNoteX = posInitialX + sizeNote * j;
            int sizeNote = (int) (this.sizeNote * 1.245);
            int endNoteX = startNoteX + sizeNote;
            noteSkins[0].explotions[j].staticDraw(canvas, new Rect(startNoteX, startValueY, endNoteX, startValueY + sizeNote));
            noteSkins[0].explotionTails[j].draw(canvas, new Rect(startNoteX, startValueY, endNoteX, startValueY + sizeNote));
            noteSkins[0].tapsEffect[j].staticDraw(canvas, new Rect(startNoteX, startValueY, endNoteX, startValueY + sizeNote));
        }
        //test draw border

//        Paint myPaint = new Paint();
//        myPaint.setColor(Color.rgb(255, 0, 0));
//        myPaint.setStrokeWidth(5);
//        myPaint.setStyle(Paint.Style.STROKE);
//        canvas.drawRect(offsetX, offsetY, sizeX, sizeY, myPaint);
//

    }

    public void update() {
        for (NoteSkin currentNote : noteSkins) {
            for (int x = 0; x < currentNote.arrows.length; x++) {
                currentNote.arrows[x].update();
                currentNote.tails[x].update();
                currentNote.longs[x].update();
                currentNote.explotions[x].update();
                currentNote.explotionTails[x].update();
                currentNote.tapsEffect[x].update();
                currentNote.receptors[x].update();
            }
            //   currentNote.receptor.update();
            currentNote.mine.update();
        }
    }


    public int getStepsByGameMode() {
        switch (gameMode) {
            case "pump-routine":
            case "pump-double":
            case "pump-halfdouble":
                return 10;
            case "pump-single":
                return 5;
            case "dance-single":
                return 4;
            case "":
                break;
        }
        return 0;

    }


}

