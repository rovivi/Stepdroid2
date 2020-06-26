package com.kyagamy.step.game.newplayer;


import com.kyagamy.step.common.step.CommonSteps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import game.GameRow;
import game.StepObject;

public class GameState {
    ArrayList<GameRow> steps;

    protected Double currentSpeedMod = 1D;
    protected Double lastScroll = 1D;
    public double currentBeat = 0d;
    public int currentTickCount = 0, currentElement = 0;
    public Double BPM;
    public Long currentTempoBeat = 0L, currentTempo = 0L, startTime = 0L, timeLapsedBeat;

    public double currentSecond = 0, lostBeatByWarp = 0;
    ArrayList<Double> currentSpeed = null;
    double initialSpeedMod = 1;
    public float currentDurationFake = 0, offset;
    public boolean isRunning = true;
    public double initialBPM ;


    public  Combo combo;


    public GameState(StepObject stepData) {
        steps = stepData.steps;
        BPM = stepData.getInitialBPM();
        initialBPM = stepData.getInitialBPM();

        offset = stepData.getSongOffset();

    }

    void checkEffects() {
        if (steps.get(currentElement).getModifiers() != null)
            effects(Objects.requireNonNull(steps.get(currentElement).getModifiers()), steps.get(currentElement).getCurrentBeat());
    }


    void effects(HashMap<String, ArrayList<Double>> effects, double effectBeat) {
        if (effects.get("BPMS") != null) {

            ArrayList<Double> entry = effects.get("BPMS");
            double auxBPM = entry.get(1);
            double difBetweenBeats2 = currentBeat - effectBeat;//2.5
            currentBeat = effectBeat + (difBetweenBeats2 / (BPM / auxBPM));//
            BPM = auxBPM;
            if (initialBPM==0){
                initialBPM=auxBPM;
            }
        }
        if (effects.get("SPEEDS") != null) {
            ArrayList<Double> entry = effects.get("SPEEDS");
            if (entry.get(2) == 0d && currentSpeed != null) {// esta cosa rara creo que la hace SM es la unica forma en la que pude "imitar unos efectos"
                currentSpeed.get(2);
                entry.set(2, currentSpeed.get(2));
            }
//
//            if (currentSpeed!=null)
//                System.out.println("aqui owo");

            initialSpeedMod = currentSpeedMod;
            currentSpeed = entry;
        }
        if (effects.get("SCROLLS") != null) {
            lastScroll = effects.get("SCROLLS").get(1);//==0d?1d:0d;
        }
        if (effects.get("WARPS") != null) {
            ArrayList<Double> entry = effects.get("WARPS");
            currentBeat += entry.get(1);
            double metaBeat = effectBeat + entry.get(1);
            while (steps.get(currentElement).getCurrentBeat() < metaBeat) {
                addCurrentElement();
                if (CommonSteps.Companion.almostEqual(metaBeat, steps.get(currentElement).getCurrentBeat())) {
                }
            }
        }
    }

    private void calculateBeat() {
        currentSecond += (System.nanoTime() - startTime) / 10000000.0;//se calcula el segundo
        startTime = System.nanoTime();
        if (lostBeatByWarp > 0) {
            currentBeat += lostBeatByWarp * 2;
            lostBeatByWarp = 0;
        }
        timeLapsedBeat = System.nanoTime() - currentTempoBeat;
        currentBeat += 1D * timeLapsedBeat / ((60 / BPM) * 1000 * 1000000);
        currentDurationFake -= timeLapsedBeat / ((60 / BPM) * 1000 * 1000000);//reduce la duración de los fakes
        currentTempoBeat = System.nanoTime();
        while (steps.get(currentElement).getCurrentBeat() <= currentBeat) {
          addCurrentElement();
        }
        isRunning = !(currentElement >= steps.size());

    }

    protected void reset() {
        currentBeat = 0;
        currentSecond = 20;
        currentElement = 0;
    }

    public void start() {
        currentTempoBeat = currentTempo = startTime = System.nanoTime();
    }

    public void update() {
        if (isRunning) {
            calculateBeat();
        }
        if (currentSpeed != null)
            calculateCurrentSpeed();
        //evaluate



    }

    void calculateCurrentSpeed() {
        double beatInitial = currentSpeed.get(0);
        double razonBeat = (initialSpeedMod - currentSpeed.get(1)) / currentSpeed.get(2);
        double metaSpeed = currentSpeed.get(1);
        double metaBeat = currentSpeed.get(0) + currentSpeed.get(2);
        currentSpeedMod = initialSpeedMod + (beatInitial - currentBeat) * razonBeat;
        if (CommonSteps.Companion.almostEqual(metaSpeed, currentSpeedMod) || currentBeat >= metaBeat) {
            currentSpeedMod = metaSpeed;
        }
    }

    void addCurrentElement (){
        evaluate();

        checkEffects();
        currentElement++;

    }


    public void evaluate() {
        if (true) {
            if (true) {
                if (Evaluator.Companion.containNoteType(steps.get(currentElement),CommonSteps.NOTE_TAP)){
                    combo.setComboUpdate(Combo.VALUE_PERFECT);
                }
//                ObjectCombo.posjudge = 0;
//                if (containTapNote(bufferSteps.get(posBuffer).rowStep)) {
//                    playTick();
//                    combopp();
//                    currentLife += 0.5 * currentCombo;
//                    ObjectCombo.show();
//                    Note[] auxrow = bufferSteps.get(posBuffer).rowStep;
//                    for (int w = 0; w < auxrow.length; w++) {//animations
//                        int aux = auxrow[w].getNoteType();
//                        if (aux == 1)
//                            steps.noteSkins[0].explotions[w].play();
//                        else if (aux == 2) {
//                            steps.noteSkins[0].explotions[w].play();
//                            steps.noteSkins[0].explotionTails[w].play();
//                        } else if (aux == 0)
//                            steps.noteSkins[0].explotionTails[w].stop();
//                    }
//                    bufferSteps.get(posBuffer).rowStep = pressedRow;
//                } else if (containLongs(bufferSteps.get(posBuffer).rowStep)) {
//                    residuoTick += ((double) currentTickCount / 48);
//                    if (residuoTick >= 1) {
//                        residuoTick -= 1;
//                        ObjectCombo.show();
//                        combopp();
//                        currentLife += 0.5 * currentCombo;
//                    }
//                    bufferSteps.get(posBuffer).rowStep = pressedRow;
//                }
            } else {//juicio normal
//                double[] currentJudge = Common.JUDMENT[ParamsSong.judgment];
//                int posBack;
//                //   int posNext;
//                int backSteps;
//                int rGreat = mil2BackSpaces((float) currentJudge[3]);
//                int rGood = rGreat + mil2BackSpaces((float) currentJudge[2]);
//                int rBad = rGood + mil2BackSpaces((float) currentJudge[1]);
//                backSteps = rBad + 1;
//
//                posBack = -backSteps;
//                if (backSteps >= posBuffer)
//                    posBack = -posBuffer;
//                //posNext = backSteps;
//
//                if (containTaps(bufferSteps.get(posBuffer + posBack).rowStep)) {//evaluate miss
//                    comboLess();
//                    bufferSteps.get(posBuffer + posBack).rowStep = pressedRow;
//                    posBack++;
//                }
//                if (containsMine(bufferSteps.get(posBuffer + posBack).rowStep)) {//evaluate miss
//                    bufferSteps.get(posBuffer + posBack).rowStep = pressedRow;
//                    posBack++;
//                }
//                if (containLongs(bufferSteps.get(posBuffer + posBack).rowStep)) {
//                    residuoTick += ((double) currentTickCount / 48);
//                    if (residuoTick >= 1) {
//                        residuoTick -= 1;
//                        ObjectCombo.show();
//                        comboLess();
//                        currentLife += 0.5 * currentCombo;
//                        miss += currentCombo;
//                    }
//                }
            }
        }
    }

    public void setCombo(Combo combo) {
        this.combo = combo;
    }
}
