package com.kyagamy.step.game.newplayer

import com.kyagamy.step.common.Common.Companion.JUDMENT
import com.kyagamy.step.common.step.CommonSteps
import com.kyagamy.step.common.step.CommonSteps.Companion.almostEqual
import com.kyagamy.step.common.step.CommonSteps.Companion.beatToSecond
import com.kyagamy.step.common.step.CommonSteps.Companion.secondToBeat
import com.kyagamy.step.common.step.Game.GameRow
import game.StepObject
import java.util.*
import kotlin.math.abs

class GameState(stepData: StepObject, @JvmField var inputs: ByteArray) {
    @JvmField
    var steps: ArrayList<GameRow>

    @JvmField
    var currentSpeedMod: Double = 1.0
    @JvmField
    var lastScroll: Double? = 1.0

    var currentAudioSecond: Double = 0.0

    @JvmField
    var currentBeat: Double = 0.0
    var currentTickCount: Int = 0
    @JvmField
    var currentElement: Int = 0
    @JvmField
    var BPM: Double
    var currentTempoBeat: Long = 0L
    var currentTempo: Long = 0L
    var startTime: Long = 0L
    var timeLapsedBeat: Long? = null
    @JvmField
    var currentSecond: Double = 0.0
    var lostBeatByWarp: Double = 0.0
    var currentSpeed: ArrayList<Double>? = null
    var initialSpeedMod: Double = 1.0
    var currentDurationFake: Float = 0f
    @JvmField
    var offset: Float
    @JvmField
    var isRunning: Boolean = true
    var initialBPM: Double
    private val touchPad: GamePad? = null

    @JvmField
    var combo: Combo? = null

    @JvmField
    var stepsDrawer: StepsDrawer? = null
    var eventAux: String = ""


    init {
        steps = stepData.steps
        BPM = stepData.getInitialBPM()
        initialBPM = stepData.getInitialBPM()
        offset = stepData.getSongOffset()
    }


    /**
     * Validate Effects  call the method effects if found someone, its a method because its called by multiples sites
     */
    fun checkEffects() {
        if (steps.get(currentElement)!!.modifiers != null) effects(
            Objects.requireNonNull<HashMap<String, ArrayList<Double>>?>(
                steps.get(currentElement)!!.modifiers
            ), steps.get(currentElement)!!.currentBeat
        )
    }


    /**
     * This method applies each effect to the SM files
     *
     *
     * param effects contains the effects type for the current beat
     *
     * @param effectBeat beat when the effect must be called (it needs to calculate dif in ms whit the current beat )
     */
    fun effects(effects: HashMap<String, ArrayList<Double>>, effectBeat: Double) {
        if (effects.get("BPMS") != null) {
            val entry = effects.get("BPMS")
            val auxBPM: Double = entry!!.get(1)!!
            val difBetweenBeats2 = currentBeat - effectBeat //2.5
            currentBeat = effectBeat + (difBetweenBeats2 / (BPM / auxBPM)) //
            BPM = auxBPM
            if (initialBPM == 0.0) {
                initialBPM = auxBPM
            }
        }
        if (effects.get("SPEEDS") != null) {
            val entry = effects.get("SPEEDS")
            if (entry!!.get(2) == 0.0 && currentSpeed != null) { // esta cosa rara creo que la hace SM es la unica forma en la que pude "imitar unos efectos"
                entry.set(2, currentSpeed!!.get(2))
            }

            //
//            if (currentSpeed!=null)
//                System.out.println("aqui owo");
            initialSpeedMod = currentSpeedMod
            currentSpeed = entry
        }
        if (effects.get("SCROLLS") != null) {
            lastScroll = effects.get("SCROLLS")!!.get(1) //==0d?1d:0d;
        }
        if (effects.get("WARPS") != null) {
            val entry = effects.get("WARPS")
            currentBeat += entry!!.get(1)!!
            val metaBeat = effectBeat + entry.get(1)!!
            while (steps.get(currentElement)!!.currentBeat < metaBeat) {
                steps.get(currentElement)!!.hasPressed = true
                currentElement++
                steps.get(currentElement)!!.hasPressed = true
                checkEffects()
                if (almostEqual(metaBeat, steps.get(currentElement)!!.currentBeat)) {
                }
            }
        }
    }

    private fun calculateBeat() {
        currentSecond += (System.nanoTime() - startTime) / 10000000.0 //se calcula el segundo
        startTime = System.nanoTime()
        if (lostBeatByWarp > 0) {
            currentBeat += lostBeatByWarp * 2
            lostBeatByWarp = 0.0
        }
        timeLapsedBeat = System.nanoTime() - currentTempoBeat
        currentBeat += 1.0 * timeLapsedBeat!! / ((60 / BPM) * 1000 * 1000000)
        currentDurationFake -= (timeLapsedBeat!! / ((60 / BPM) * 1000 * 1000000)).toFloat() //reduce la duración de los fakes
        currentTempoBeat = System.nanoTime()
        while (steps.get(currentElement)!!.currentBeat <= currentBeat) {
            checkEffects()
            currentElement++
            if (Evaluator.Companion.containsNoteTap(steps.get(currentElement)!!) || Evaluator.Companion.containNoteType(
                    steps.get(currentElement)!!, CommonSteps.Companion.NOTE_LONG_START
                )
            ) {
                //  combo.setComboUpdate(Combo.VALUE_PERFECT);
            }
        }
        isRunning = currentElement < steps.size
        evaluate()
    }

    fun reset() {
        currentBeat = 0.0
        currentSecond = 0.0
        currentElement = 0
    }

    fun start() {
        startTime = System.nanoTime()
        currentTempo = startTime
        currentTempoBeat = currentTempo
    }

    fun update() {
        if (isRunning) {
            calculateBeat()
        }
        if (currentSpeed != null) calculateCurrentSpeed()
    }

    fun calculateCurrentSpeed() {
        val beatInitial = currentSpeed!!.get(0)
        val razonBeat = (initialSpeedMod - currentSpeed!!.get(1)) / currentSpeed!!.get(2)
        val metaSpeed = currentSpeed!!.get(1)
        val metaBeat = currentSpeed!!.get(0) + currentSpeed!!.get(2)
        currentSpeedMod = initialSpeedMod + (beatInitial - currentBeat) * razonBeat
        if (almostEqual(metaSpeed, currentSpeedMod) || currentBeat >= metaBeat) {
            currentSpeedMod = metaSpeed
        }
    }

    fun addCurrentElement(evaluate: Boolean) {
        if (evaluate) {
            //   evaluate();
        }
        checkEffects()
        currentElement++
    }

    fun evaluate() {
        if (false) { //Autoplay

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
        } else { //juicio normal
            val currentJudge = JUDMENT[2] //se busca el miss
            var posBack: Int
            val rGreat = currentJudge[3]
            val rGood = rGreat + currentJudge[2]
            val rBad = rGood + currentJudge[1]

            // Log timing windows (only once every few seconds to avoid spam)
            if (currentElement % 100 == 0) {
                println("⏱️ Timing windows - PERFECT: <${rGreat}ms, GREAT: ${rGreat}-${rGood}ms, GOOD: ${rGood}-${rBad}ms, BAD: >${rBad}ms")
            }

            val addBeats = secondToBeat(rBad / 1000.0, BPM)
            posBack = 0
            //Search outBeatRange gameRow
            while ((currentElement + posBack) > 0 &&
                steps.get(currentElement + posBack)!!.currentBeat >= (currentBeat - addBeats)
            ) {
                posBack--
            }
            ///**/MISS**/
            if ((currentElement + posBack) > 0 && !steps.get(currentElement + posBack - 1)!!.hasPressed &&
                Evaluator.Companion.containNoteToEvaluate(steps.get(currentElement + posBack - 1)!!)
            ) {
                combo!!.setComboUpdate(Combo.VALUE_MISS.toShort())
                steps.get(currentElement + posBack - 1)!!.hasPressed = true
            }

            var posEvaluate = -1
            while ((currentElement + posBack) < steps.size &&
                steps.get(currentElement + posBack)!!.currentBeat <= (currentBeat + addBeats)
            ) {
                if ((steps.get(currentElement + posBack))!!.notes != null) { //Validate emptyRow
                    //boolean checkLong = true;
                    //byte[] auxRow = (byte[]) steps.get(currentElement + posBack)[0];
                    for (arrowIndex in steps.get(currentElement + posBack)!!.notes!!.indices) {
                        val currentChar =
                            steps.get(currentElement + posBack)!!.notes!!.get(arrowIndex)
                        if (inputs[arrowIndex] == CommonSteps.Companion.ARROW_PRESSED && currentChar.noteType == CommonSteps.Companion.NOTE_TAP) { //NORMALTAP
                            stepsDrawer?.selectedSkin?.explotions?.get(arrowIndex)?.play()
                            steps.get(currentElement + posBack)!!.notes!!.get(arrowIndex).noteType =
                                CommonSteps.Companion.NOTE_PRESSED
                            inputs[arrowIndex] = CommonSteps.Companion.ARROW_HOLD_PRESSED
                            posEvaluate = currentElement + posBack
                            // continue;
                        }
                        if (inputs[arrowIndex] != CommonSteps.Companion.ARROW_UNPRESSED && (currentChar.noteType == CommonSteps.Companion.NOTE_LONG_START || currentChar.noteType == CommonSteps.Companion.NOTE_LONG_BODY || currentChar.noteType == CommonSteps.Companion.NOTE_LONG_END)
                            && posBack < 0
                        ) { // tap1
                            steps.get(currentElement + posBack)!!.notes!!.get(arrowIndex).noteType =
                                CommonSteps.Companion.NOTE_LONG_PRESSED
                            //                            steps.get(currentElement + posBack).getNotes().get(arrowIndex).setType(currentChar.getType() == NOTE_LONG_END ? NOTE_PRESSED : NOTE_LONG_PRESSED);
                            if (!Evaluator.Companion.containNoteToEvaluate(steps.get(currentElement + posBack)!!)) {
                                steps.get(currentElement + posBack)!!.hasPressed = true
                                combo!!.setComboUpdate(Combo.VALUE_PERFECT.toShort())
                            }

                            stepsDrawer?.selectedSkin?.explotionTails?.get(arrowIndex)?.play()
                            inputs[arrowIndex] = CommonSteps.Companion.ARROW_HOLD_PRESSED
                        }
                        if (inputs[arrowIndex] == CommonSteps.Companion.ARROW_UNPRESSED) {
                            val selectedSkin = stepsDrawer?.selectedSkin
                            if (arrowIndex < (selectedSkin?.explotionTails?.size ?: 0)) {
                                selectedSkin?.explotionTails?.get(arrowIndex)?.stop()
                            }
                        }
                    }
                }
                if (posEvaluate != -1) {
                    val bol = !steps.get(posEvaluate)!!.hasPressed

                    if (!Evaluator.Companion.containNoteToEvaluate(steps.get(posEvaluate)!!) && bol) { //mejorar la condicion xdd
                        steps.get(posEvaluate)!!.hasPressed = true
                        val auxRetro = abs(
                            beatToSecond(
                                currentBeat - steps.get(posEvaluate)!!.currentBeat,
                                BPM
                            )
                        ) * 1000
                        println("🎯 Timing evaluation: auxRetro=${auxRetro}ms, rGreat=${rGreat}ms, rGood=${rGood}ms, rBad=${rBad}ms")
                        if (Evaluator.Companion.containsNoteLongPressed(steps.get(posEvaluate)!!)) {
                            println("🎵 LONG NOTE -> PERFECT")
                            combo!!.setComboUpdate(Combo.VALUE_PERFECT.toShort())
                        } else if (auxRetro < rGreat) { //perfetc
                            println("🎵 PERFECT (${auxRetro} < ${rGreat})")
                            combo!!.setComboUpdate(Combo.VALUE_PERFECT.toShort())
                        } else if (auxRetro < rGood) { //great
                            println("🎵 GREAT (${auxRetro} < ${rGood})")
                            combo!!.setComboUpdate(Combo.VALUE_GREAT.toShort())
                        } else if (auxRetro < rBad) { //good
                            println("🎵 GOOD (${auxRetro} < ${rBad})")
                            combo!!.setComboUpdate(Combo.VALUE_GOOD.toShort())
                        } else { //bad
                            println("🎵 BAD (${auxRetro} >= ${rBad})")
                            combo!!.setComboUpdate(Combo.VALUE_BAD.toShort())
                        }
                        eventAux =
                            "add:" + addBeats + " positions to check:" + posBack + "beat eval:" + steps.get(
                                posEvaluate
                            )!!.currentBeat
                        continue
                    }
                }
                eventAux =
                    currentBeat.toString() + ":" + steps.get(posBack + currentElement)!!.currentBeat
                posBack++
            }
        }
    }
}