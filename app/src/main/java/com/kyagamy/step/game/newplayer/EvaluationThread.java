package com.kyagamy.step.game.newplayer;

import com.kyagamy.step.common.Common;

import java.util.ArrayList;

import game.Note;

import static com.kyagamy.step.common.step.CommonSteps.NOTE_FAKE;
import static com.kyagamy.step.common.step.CommonSteps.NOTE_TAP;

public class EvaluationThread extends Thread {
    public boolean running = true;
    GameState gameState;


    public EvaluationThread(GameState game) {
        this.gameState = game;
    }


    @Override
    public void run() {
        while (running) {
            try {
                    evaluate();
                
                sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    public boolean checkTime(ArrayList<Float[]> array, int pos) {
        return array != null && (array.size() > 0 && array.size() > pos && gameState.currentBeat >= array.get(pos)[0]);
    }


    public void evaluate() {
        if (true) {
            double[] currentJudge = Common.Companion.getJUDMENT()[4];
            if (true) {//autoPlay
           //juicio normal
                int posBack;
                int posNext;
                int backSteps;
                int rGreat = mil2BackSpaces((float) currentJudge[3]);
                int rGood = rGreat + mil2BackSpaces((float) currentJudge[2]);
                int rBad = rGood + mil2BackSpaces((float) currentJudge[1]);
                backSteps = rBad + 1;

                posBack = -backSteps;
                if (backSteps >= gameState.currentElement) {
                    posBack = -gameState.currentElement;
                }
                posNext = backSteps;
                int posEvaluate = -1;
                while ((gameState.currentElement + posBack < gameState.steps.size()) && posBack <= posNext ) {
//                    if (containSteps( gameState.steps.get(gameState.currentElement + posBack))) {
                        boolean checkLong = true;
                        //byte[] auxRow = (byte[]) steps.get(currentElement + posBack)[0];
                        for (int w = 0; w < gameState.steps.get(gameState.currentElement + posBack).getNotes().size(); w++) {
                            Note currentChar = gameState.steps.get(gameState.currentElement + posBack).getNotes().get(w);


                            if (gameState.inputs[w] == 1 && currentChar.getType()!= NOTE_FAKE) {// tap1
                                //gameState.steps.noteSkins[0].explotions[w].play();
                                gameState.steps.get(gameState.currentElement + posBack).getNotes().get(w).setType(NOTE_TAP);
                                gameState.inputs[w] = 2;
                                posEvaluate = gameState.currentElement + posBack;

                            }

//                            if (gameState.inputs[w] == 1 && containsMine(currentChar)) {//tap mine
//                                // steps.explotions[w].play();
//                                gameState.steps.get(gameState.currentElement + posBack)[w].setNoteType((byte) 0);
//                                gameState.inputs[w] = 2;
//                                posEvaluate = gameState.currentElement + posBack;
//                             //   gameState.soundPool.play(gameState.soundPullMine, 0.8f, 0.8f, 1, 0, 1f);
//                              //  gameState.mineHideValue = 255;
////                                gameState.currentLife -= 10;
//                            }


                            if (gameState.inputs[w] == 0) {
//                                if (w < Steps.noteSkins[0].explotionTails.length) {
//                                    Steps.noteSkins[0].explotionTails[w].stop();
//                                }
                            }



                        }
//                    }
                    if (posEvaluate != -1) {
                        if (!Evaluator.Companion.containsNoteTap(gameState.steps.get(posEvaluate))) {

                            int auxRetro = Math.abs(posBack);

                            if (auxRetro < rGreat) {//perfetc
                                gameState.combo.setComboUpdate (Combo.VALUE_GREAT);
                            } else if (auxRetro < rGood) {//great
                                gameState.combo.setComboUpdate (Combo.VALUE_GOOD);
                            } else if (auxRetro < rBad) {//good
                                gameState.combo.setComboUpdate (Combo.VALUE_MISS);
                            } else {//bad
                                gameState.combo.setComboUpdate (Combo.VALUE_BAD);

                            }
                            // AQUI SE VERA SI ES GREAT O QUE ONDA
//                            gameState.ObjectCombo.show();
                        }
                        posEvaluate = -1;

                    }
                    posBack++;
                }
            }
        }
    }


    private int mil2BackSpaces(float judgeTime) {
        int backs = 0;
        float auxJudge = 0;
        while ((gameState.currentElement - backs) >= 0) {
            auxJudge += Common.Companion.beat2Second((double) 4 / 192, gameState.BPM) * 1000;
            backs++;
            if (auxJudge >= judgeTime + 23) {
                break;
            }
        }
        return backs;
    }



}
