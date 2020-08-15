package com.kyagamy.step.game.newplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import com.kyagamy.step.R;
import com.kyagamy.step.common.step.CommonGame.CustomSprite.SpriteReader;


public class Combo {

    public static final byte VALUE_PERFECT = 0;
    public static final byte VALUE_GREAT = 1;
    public static final byte VALUE_GOOD = 2;
    public static final byte VALUE_BAD = 3;
    public static final byte VALUE_MISS = 4;
    public static final byte VALUE_MISSING = 1;


    //proportions Y
    static private float COMBO_TEXT_RATIO_X = 0.14815f*1.25f;
    static private float COMBO_TEXT_RATIO_Y = 0.0363637f*1.25f;

    static private float COMBO_NUMBER_RATIO_X = 0.05555556f*1.15f;
    static private float COMBO_NUMBER_RATIO_Y = 0.06141616f;

    static private float COMBO_LABEL_RATIO_X = 0.306f;
    static private float COMBO_LABEL_RATIO_Y = 0.0555555556f;

    static  private float RATIO_BIGGER_LABEL =0.6666666667f;

    private Long timeMark;

    private SpriteReader judgeSprite, numberCombo;
    private Bitmap comboImage, badCombo, currentBitMapCombo;

    private int x;
    private int y;

    private int combo = 0;
    private int aumentTip = -220;
    private Paint paint = new Paint();
    private LifeBar lifeBar;



    public short positionJudge = 0;

    public Combo(Context c, StepsDrawer stepsDrawer) {
        timeMark = System.currentTimeMillis();
        this.x = stepsDrawer.sizeX+stepsDrawer.offsetX;
        this.y = stepsDrawer.sizeY;
        BitmapFactory.Options myOpt2 = new BitmapFactory.Options();
        myOpt2.inSampleSize = 0;
        numberCombo = new SpriteReader(BitmapFactory.decodeResource(c.getResources(), R.drawable.play_combo_number, myOpt2), 10, 1, 1f);
        judgeSprite = new SpriteReader(BitmapFactory.decodeResource(c.getResources(), R.drawable.play_combo_judge, myOpt2), 1, 5, 1f);
        comboImage = BitmapFactory.decodeResource(c.getResources(), R.drawable.play_combo, myOpt2);
        badCombo = BitmapFactory.decodeResource(c.getResources(), R.drawable.play_combo_bad, myOpt2);

    }

    public void setLifeBar(LifeBar lifeBar){
        this.lifeBar = lifeBar;

    }
    public void show() {
        aumentTip = 20;
        paint.setAlpha(255);
        currentBitMapCombo=(combo >= 0)?comboImage:badCombo;
    }

    public void setComboUpdate(short typeTap) {
        positionJudge = typeTap;
        switch (typeTap) {
            case VALUE_PERFECT:
                Evaluator.Companion.setPERFECT(Evaluator.Companion.getPERFECT()+1);
                combo = (combo < 0) ? 1 : (combo+1);
                break;
            case VALUE_GREAT:
                Evaluator.Companion.setGREAT(Evaluator.Companion.getGREAT()+1);
                combo = (combo < 0) ? 1 : (combo+1);
                break;
            case VALUE_GOOD:
                Evaluator.Companion.setGOOD(Evaluator.Companion.getGOOD()+1);
                if (combo <-4)combo=0;
                break;
            case VALUE_BAD:
                Evaluator.Companion.setBAD(Evaluator.Companion.getBAD()+1);
                if (combo !=0)combo=0;
                break;
            case VALUE_MISS:
                Evaluator.Companion.setMISS(Evaluator.Companion.getMISS()+1);
                combo = (combo > 0) ? 0 : (combo-1);
                break;
        }
        lifeBar.updateLife((byte) typeTap,1);
        if (combo>Evaluator.Companion.getMAX_COMBO())
            Evaluator.Companion.setMAX_COMBO(combo);
        show();

    }

    public void update() {
     //  if (System.nanoTime() - timeMark > 100) {
            aumentTip -= 1;
            timeMark = System.nanoTime();
       // }
    }

    public void draw(Canvas canvas) {
        //setSizes
        int numberSizeY = (int) (y * COMBO_NUMBER_RATIO_X);
        int numberSizeX = (int) (y * COMBO_NUMBER_RATIO_X);

        int comboSizeY =(int) ((y * COMBO_TEXT_RATIO_Y));
        int comboSizeX =(int) ((y * COMBO_TEXT_RATIO_X));

        int labelSizeY =(int) ((y * COMBO_LABEL_RATIO_Y));
        int labelSizeX =(int) ((y * COMBO_LABEL_RATIO_X));

        //initX For each type

        if (aumentTip>14 &&aumentTip<21)
        {
            float  relation = 1+  (aumentTip-15)*0.22f*RATIO_BIGGER_LABEL  ;
            labelSizeY*=relation;
            labelSizeX*=relation;
            comboSizeX*=(relation-1)/3 +1;
            comboSizeY*=(relation-1)/3 +1;
        }

        int posLabelIntX = (int) ((x / 2f - labelSizeX / 2f)*1.008);
        int posComboIntX = (int) (x / 2f - comboSizeX / 2f);

        if (aumentTip<6)
               paint.setAlpha(Math.abs(-(255/(5)*aumentTip)));


        int posIntYCombo = (y / 2 - (numberSizeY + labelSizeY + comboSizeY) / 2);// (int) (y / 2 - (y * 0.05) / 2);

        if (aumentTip > 0) {
            canvas.drawBitmap(judgeSprite.frames[positionJudge], null, new Rect(posLabelIntX, posIntYCombo, posLabelIntX + labelSizeX, posIntYCombo + labelSizeY), paint);

            posIntYCombo +=   labelSizeY*1.08;
            if (combo > 3 || combo < -3) {
                //show combo text
                canvas.drawBitmap(currentBitMapCombo, null, new Rect(posComboIntX, posIntYCombo, posComboIntX + comboSizeX, posIntYCombo +comboSizeY) , paint);
                posIntYCombo+=comboSizeY;
                String stringComboAux = (100000000 + Math.abs(combo)) + "";
                String stringCombo = Math.abs(combo) + "";

                int drawTimes = 4;//number of types you need to draw number example combo 39 then 3 digits show 039
                if (stringCombo.length() > 3)
                    drawTimes = stringCombo.length() + 1;

                for (int w = 1; w < drawTimes; w++) {
                    int totalComboLength = (drawTimes - 1) *numberSizeX;
                    int positionCurrentNumber = ((totalComboLength / 2) + x / 2) - numberSizeX * w;
                    int n = Integer.parseInt(stringComboAux.charAt(stringComboAux.length() - w) + "");
                    canvas.drawBitmap(numberCombo.frames[n], null, new Rect(positionCurrentNumber, posIntYCombo, positionCurrentNumber + numberSizeX, posIntYCombo + numberSizeY), paint);
                }
            }
        }
    }
}
