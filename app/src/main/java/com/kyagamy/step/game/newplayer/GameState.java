package com.kyagamy.step.game.newplayer;

import android.media.AudioManager;
import android.media.SoundPool;

import com.kyagamy.step.common.Common;
import com.kyagamy.step.common.step.CommonSteps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import com.kyagamy.step.common.step.Game.GameRow;

import game.Note;
import game.StepObject;

import static com.kyagamy.step.common.step.CommonSteps.ARROW_HOLD_PRESSED;
import static com.kyagamy.step.common.step.CommonSteps.ARROW_PRESSED;
import static com.kyagamy.step.common.step.CommonSteps.ARROW_UNPRESSED;
import static com.kyagamy.step.common.step.CommonSteps.NOTE_LONG_BODY;
import static com.kyagamy.step.common.step.CommonSteps.NOTE_LONG_END;
import static com.kyagamy.step.common.step.CommonSteps.NOTE_LONG_PRESSED;
import static com.kyagamy.step.common.step.CommonSteps.NOTE_LONG_START;
import static com.kyagamy.step.common.step.CommonSteps.NOTE_PRESSED;
import static com.kyagamy.step.common.step.CommonSteps.NOTE_TAP;

public class GameState {

    ArrayList<GameRow> steps;
    protected Double currentSpeedMod = 1D;
    protected Double lastScroll = 1D;

    public Double currentAudioSecond = 0d;

    public double currentBeat = 0d;
    public int currentTickCount = 0, currentElement = 0;
    public Double BPM;
    public Long currentTempoBeat = 0L, currentTempo = 0L, startTime = 0L, timeLapsedBeat;
    public double currentSecond = 0, lostBeatByWarp = 0;
    ArrayList<Double> currentSpeed = null;
    double initialSpeedMod = 1;
    public float currentDurationFake = 0, offset;
    public boolean isRunning = true;
    public double initialBPM;
    public byte[] inputs;
    private GamePad touchPad;
    public Combo combo;
    public StepsDrawer stepsDrawer;
    public String eventAux = "";


    public GameState(StepObject stepData, byte[] pad) {
        this.inputs = pad;
        steps = stepData.steps;
        BPM = stepData.getInitialBPM();
        initialBPM = stepData.getInitialBPM();
        offset = stepData.getSongOffset();

    }


    /**
     * Validate Effects  call the method effects if found someone, its a method because its called by multiples sites
     */
    void checkEffects() {
        if (steps.get(currentElement).getModifiers() != null)
            effects(Objects.requireNonNull(steps.get(currentElement).getModifiers()), steps.get(currentElement).getCurrentBeat());
    }


    /**
     * This method applies each effect to the SM files
     * <p>
     * param effects contains the effects type for the current beat
     *
     * @param effectBeat beat when the effect must be called (it needs to calculate dif in ms whit the current beat )
     */
    void effects(HashMap<String, ArrayList<Double>> effects, double effectBeat) {
        if (effects.get("BPMS") != null) {

            ArrayList<Double> entry = effects.get("BPMS");
            double auxBPM = entry.get(1);
            double difBetweenBeats2 = currentBeat - effectBeat;//2.5
            currentBeat = effectBeat + (difBetweenBeats2 / (BPM / auxBPM));//
            BPM = auxBPM;
            if (initialBPM == 0) {
                initialBPM = auxBPM;
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
                steps.get(currentElement).setHasPressed(true);
                currentElement++;
                steps.get(currentElement).setHasPressed(true);
                checkEffects();
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
            checkEffects();
            currentElement++;
            if (Evaluator.Companion.containsNoteTap(steps.get(currentElement)) || Evaluator.Companion.containNoteType(steps.get(currentElement), NOTE_LONG_START)) {
                //  combo.setComboUpdate(Combo.VALUE_PERFECT);
            }
        }
        isRunning = !(currentElement >= steps.size());
        evaluate();
    }

    protected void reset() {
        currentBeat = 0;
        currentSecond = 0;
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

    void addCurrentElement(boolean evaluate) {
        if (evaluate) {
            //   evaluate();
        }
        checkEffects();
        currentElement++;
    }

    public void setStepsDrawer(StepsDrawer stepsDrawer) {
        this.stepsDrawer = stepsDrawer;
    }

    public void evaluate() {
        if (false) {//Autoplay

//            if (Evaluator.Companion.containNoteType(steps.get(currentElement), CommonSteps.NOTE_TAP)) {
//               // combo.setComboUpdate(Combo.VALUE_PERFECT);
//            }
//            //ObjectCombo.posjudge = 0;
//            if (Evaluator.Companion.containsNoteTap(steps.get(currentElement))) {
//                //  combopp();
//                //currentLife += 0.5 * currentCombo;
//                //ObjectCombo.show();
//                GameRow auxrow = steps.get(currentElement);
//                for (int w = 0; auxrow.getNotes() != null && w < auxrow.getNotes().size(); w++) {//animations
//                    int aux = auxrow.getNotes().get(w).getType();
//                    if (aux == NOTE_TAP)
//                        stepsDrawer.noteSkins[0].explotions[w].play();
//                    else if (aux == NOTE_LONG_BODY) {
//                        stepsDrawer.noteSkins[0].explotions[w].play();
//                        stepsDrawer.noteSkins[0].explotionTails[w].play();
//                    } else if (aux == NOTE_EMPTY)
//                        stepsDrawer.noteSkins[0].explotionTails[w].stop();
//                }
//                steps.get(currentElement).setHasPressed(true);
//            } else if (Evaluator.Companion.containNoteLong(steps.get(currentElement))) {
//                combo.setComboUpdate(Combo.VALUE_PERFECT);
//            }
//            steps.get(currentElement).setHasPressed(true);
        } else {//juicio normal
            double[] currentJudge = Common.Companion.getJUDMENT()[2];//se busca el miss
            int posBack;
            double rGreat = currentJudge[3];
            double rGood = rGreat + currentJudge[2];
            double rBad = rGood + currentJudge[1];

            double addBeats = CommonSteps.Companion.secondToBeat(rBad / 1000d, BPM);
            posBack = 0;
            //Search outBeatRange gameRow
            while (( currentElement + posBack) > 0 &&
                    steps.get(currentElement + posBack).getCurrentBeat() >= (currentBeat - addBeats)) {
                posBack--;
            }
            ////MISS
            if ((currentElement + posBack) > 0 &&
                    !steps.get(currentElement + posBack - 1).getHasPressed() &&
                    Evaluator.Companion.containNoteToEvaluate(steps.get(currentElement + posBack - 1))
            ) {
                combo.setComboUpdate(Combo.VALUE_MISS);
                steps.get(currentElement + posBack - 1).setHasPressed(true);
            }

            int posEvaluate = -1;
            while ((currentElement + posBack) < steps.size() &&
                    steps.get(currentElement + posBack).getCurrentBeat() <= (currentBeat + addBeats)) {

                if ((steps.get(currentElement + posBack)).getNotes() != null) {//Validate emptyRow
                    //boolean checkLong = true;
                    //byte[] auxRow = (byte[]) steps.get(currentElement + posBack)[0];
                    for (int arrowIndex = 0; arrowIndex < steps.get(currentElement + posBack).getNotes().size(); arrowIndex++) {
                        Note currentChar = steps.get(currentElement + posBack).getNotes().get(arrowIndex);
                        if (inputs[arrowIndex] == ARROW_PRESSED && currentChar.getType() == NOTE_TAP) {//NORMALTAP
                            StepsDrawer.noteSkins[0].explotions[arrowIndex].play();
                            steps.get(currentElement + posBack).getNotes().get(arrowIndex).setType(NOTE_PRESSED);
                            inputs[arrowIndex] = ARROW_HOLD_PRESSED;
                            posEvaluate = currentElement + posBack;
                            // continue;
                        }
                        if (inputs[arrowIndex] != ARROW_UNPRESSED
                                && (currentChar.getType() == NOTE_LONG_START || currentChar.getType() == NOTE_LONG_BODY || currentChar.getType() == NOTE_LONG_END)
                                && posBack < 0
                        ) {// tap1
                            steps.get(currentElement + posBack).getNotes().get(arrowIndex).setType(NOTE_LONG_PRESSED);
//                            steps.get(currentElement + posBack).getNotes().get(arrowIndex).setType(currentChar.getType() == NOTE_LONG_END ? NOTE_PRESSED : NOTE_LONG_PRESSED);
                            if(!Evaluator.Companion.containNoteToEvaluate(steps.get(currentElement + posBack))){
                                steps.get(currentElement + posBack).setHasPressed(true);
                                combo.setComboUpdate(Combo.VALUE_PERFECT);
                            }

                            StepsDrawer.noteSkins[0].explotionTails[arrowIndex].play();
                            inputs[arrowIndex] = ARROW_HOLD_PRESSED;
                        }
                        if (inputs[arrowIndex] == ARROW_UNPRESSED) {
                            if (arrowIndex < StepsDrawer.noteSkins[0].explotionTails.length) {
                                StepsDrawer.noteSkins[0].explotionTails[arrowIndex].stop();
                            }
                        }
                    }
                }
                if (posEvaluate != -1) {
                    boolean bol = !steps.get(posEvaluate).getHasPressed();

                    if (!Evaluator.Companion.containNoteToEvaluate(steps.get(posEvaluate))  && bol) {//mejorar la condicion xdd
                        steps.get(posEvaluate).setHasPressed(true);
                        double auxRetro = Math.abs(CommonSteps.Companion.beatToSecond(currentBeat - steps.get(posEvaluate).getCurrentBeat(), BPM)) * 1000;
                        System.out.println(auxRetro + "NOTE" + posEvaluate);
                        if (Evaluator.Companion.containsNoteLongPressed(steps.get(posEvaluate))) {
                            combo.setComboUpdate(Combo.VALUE_PERFECT);
                        } else if (auxRetro < rGreat) {//perfetc
                            combo.setComboUpdate(Combo.VALUE_PERFECT);
                        } else if (auxRetro < rGood) {//great
                            combo.setComboUpdate(Combo.VALUE_GREAT);
                        } else if (auxRetro < rBad) {//good
                            combo.setComboUpdate(Combo.VALUE_GOOD);
                        } else {//bad
                            combo.setComboUpdate(Combo.VALUE_BAD);
                        }
                        eventAux = "add:" + addBeats + " positions to check:" + posBack + "beat eval:" + steps.get(posEvaluate).getCurrentBeat();
                        continue;
                    }
                }
                eventAux = currentBeat + ":" + steps.get(posBack + currentElement).getCurrentBeat();
                posBack++;
            }
        }




    }

    public void setCombo(Combo combo) {
        this.combo = combo;
    }

}