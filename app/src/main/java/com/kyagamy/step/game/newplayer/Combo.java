package com.kyagamy.step.game.newplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.kyagamy.step.R;
import com.kyagamy.step.common.step.CommonGame.CustomSprite.SpriteReader;
import com.kyagamy.step.common.step.CommonGame.TransformBitmap;


public class Combo {

    public  static final byte VALUE_PERFECT =0;
    public  static final byte VALUE_GREAT = 1;
    public  static final byte VALUE_GOOD = 2;
    public  static final byte VALUE_BAD = 3;
    public  static final byte VALUE_MISS = 4;
    public  static final byte VALUE_MISSING = 1;

    Long timeMark;
    short auxLife = 1;

    private SpriteReader judgeSprite, numberCombo;
    private Bitmap comboImage, badCombo, currentBitMapCombo;
    private float ratioStep, aument, ratioStepCombo, aumentLabel, tiemposcombo;
    private int posIntY,x,y, posIntXCombo, posIntYCombo, posIntYNC, posIntX, combo = 0,aumentCombo = -220;

    public short positionJudge = 0;
    float factor=1f;

    public Combo(Context c,StepsDrawer stepsDrawer) {
        this.x=stepsDrawer.sizeX;
        this.y=stepsDrawer.sizeY;
        BitmapFactory.Options myOpt2 = new BitmapFactory.Options();
        myOpt2.inSampleSize = 0;
        numberCombo = new SpriteReader(BitmapFactory.decodeResource(c.getResources(), R.drawable.play_combo_number, myOpt2),10 , 1, 1f);
        judgeSprite = new SpriteReader( BitmapFactory.decodeResource(c.getResources(), R.drawable.play_combo_judge, myOpt2), 1, 5, 1f);
        comboImage = BitmapFactory.decodeResource(c.getResources(), R.drawable.play_combo, myOpt2);
        badCombo = BitmapFactory.decodeResource(c.getResources(), R.drawable.play_combo_bad, myOpt2);
        // dibujante.setColor(Color.TRANSPARENT);


        ////////Medidades
            /*
            Perfect = (x=20%,y=6&) y max (x=30%,y=8.4&)
            Combo   =   (x=10%,y=3.1&) y max (x=15%,y=5&)
            1000    =  (x=5%,y=5&)
            */
        /////////
        ratioStep = 0.66f;
        aument = 0.0425f;
        aumentLabel = 2 * aument / 3;
        ratioStepCombo = 2 * ratioStep / 3;


        posIntY = (int) (y / 2 - (y * 0.084) / 2);


        posIntYNC = 0;

    timeMark =System.currentTimeMillis();
    }


    public void start() {
        tiemposcombo = System.currentTimeMillis();
    }

    public void show() {

        aumentCombo = 8;
        if (combo >= 0) {
            currentBitMapCombo = comboImage;
        } else {
            currentBitMapCombo = badCombo;
        }


    }

    public void setComboUpdate(short typeTap) {
        aumentCombo = 12;
        positionJudge=typeTap;
        switch (typeTap){
            case VALUE_PERFECT:
            case VALUE_GREAT:
                combo++;
                break;
            case VALUE_BAD:
            case VALUE_MISS:
                combo = (combo >0)?0: combo--;
        }
        show();

    }

    public void update(){
        if (System.nanoTime() - timeMark > 150) {
//            if (aumentCombo > 6 || aumentCombo < 0) {
//                auxLife *= -1;
//            /}
            aumentCombo -= 1;
            timeMark = System.nanoTime();
        }

        //;

    }



    public void draw(Canvas canvas) {
        posIntX = (int) (x / 2 - (x * 0.4 * (ratioStep + aument * aumentCombo)) / 2);


        posIntXCombo = (int) (x / 2 - (x * 0.13) * (ratioStep + aument * aumentCombo) / 2);


        posIntYCombo = (int) (posIntY + (y * 0.039) + (y * 0.084) * (ratioStepCombo + aumentLabel * aumentCombo) / 2);// (int) (y / 2 - (y * 0.05) / 2);


        if (aumentCombo > -12) {
            if (aumentCombo >= 0) {

                canvas.drawBitmap(judgeSprite.frames[positionJudge], null, new Rect(posIntX, posIntY, posIntX + (int) ((x * 0.4) * (ratioStep + aument * aumentCombo)), posIntY + (int) ((y * 0.084*factor) * (ratioStep + aument * aumentCombo))), new Paint());
                posIntYNC = posIntYCombo + (int) ((comboImage.getHeight()) * (ratioStepCombo + aumentLabel * aumentCombo) * 0.7);

            } else if (aumentCombo < -6) {
                posIntX = (int) (x / 2 - (x * 0.4 * (ratioStep)) / 2);
                int opacidad = (100 + 5 * aumentCombo);
                int xt = aumentCombo * -1;
                int yt = aumentCombo;
                canvas.drawBitmap(TransformBitmap.makeTransparent(judgeSprite.frames[positionJudge], opacidad), null, new Rect(posIntX, posIntY, posIntX + (int) (x * 0.4 * (ratioStep) * ((float) xt * 5 / 100)), 10 * aumentCombo + posIntY + (int) ((y *factor* 0.084) * (ratioStep + aument * aumentCombo) * ((float) yt * 5 / 100))), new Paint());

            } else {
                posIntX = (int) (x / 2 - (x * 0.4 * (ratioStep)) / 2);
                canvas.drawBitmap(judgeSprite.frames[positionJudge], null, new Rect(posIntX, posIntY, posIntX + (int) (x * 0.4 * ratioStep), posIntY + (int) (y * 0.084 *factor* ratioStep)), new Paint());
                // canvas.drawBitmap(currentBitMapCombo, null, new Rect(posIntXCombo, posIntYCombo, posIntXCombo + (int) ((x * 0.17) * (ratiostepcombo + aumentolabel)), posIntYCombo + (int) ((y * 0.053) * (ratiostepcombo + aumentolabel))), new Paint());
            }

            //draw
            //canvas.drawText(""+Combo,x/2-50,y/2,dibujante);

            posIntYNC = posIntYCombo + (int) ((y * 0.05) * (ratioStepCombo + aumentLabel * aumentCombo));
            if (combo > 3 || combo < -3) {


                canvas.drawBitmap(currentBitMapCombo, null, new Rect(posIntXCombo, posIntYCombo, posIntXCombo + (int) ((x * 0.17) * (ratioStepCombo + aumentLabel * aumentCombo)), posIntYCombo + (int) ((y * 0.053) * (ratioStepCombo + aumentLabel * aumentCombo))), new Paint());

                ////

                long lc = 100000000 + Math.abs(combo);
                String sc = lc + "";
                String sc2 = Math.abs(combo) + "";
                int nveces = 4;//dice el numero de veces que tienes

                if (sc2.length() > 3) {
                    nveces = sc2.length() + 1;
                }
                for (int w = 1; w < nveces; w++) {
                    int leng = (nveces - 1) * (int) (x * 0.05);
                    int posxnc = (int) ((0.9 * (leng / 2) + x / 2) - x * 0.05 * w * 1.0);
                    int n = Integer.parseInt(sc.charAt(sc.length() - w) + "");
                    canvas.drawBitmap(numberCombo.frames[n], null, new Rect(posxnc, posIntYNC, posxnc + (int) (x * 0.05), posIntYNC + (int) (x * 0.05)), new Paint());
                }
            }

        }

    }
}
