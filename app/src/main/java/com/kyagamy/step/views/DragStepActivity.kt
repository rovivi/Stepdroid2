package com.kyagamy.step.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import com.google.gson.Gson
import com.kyagamy.step.R
import com.kyagamy.step.common.step.CommonGame.ArrowsPositionPlace
import com.kyagamy.step.databinding.ActivityDragStepBinding


class DragStepActivity : FullScreenActivity() {

    private lateinit var binding: ActivityDragStepBinding

    private var _xDelta = 0
    private var _yDelta = 0
    private var heightOffset = 0.5f // Variable para controlar altura (0.0 = arriba, 1.0 = abajo)
    private var horizontalOffset = 0.5f // Variable para controlar posición horizontal independiente
    private var spacingMultiplier = 1.0f // Variable para controlar espaciado entre pads
    private var currentPreset = Preset.PUMP // Guardar preset actual
    private var currentSize = 100 // Guardar tamaño actual
    private var stepInfo: List<Int> = listOf(
        R.drawable.selector_down_left,
        R.drawable.selector_up_left,
        R.drawable.selector_center,
        R.drawable.selector_up_right,
        R.drawable.selector_down_right
    )
    private var arrows: ArrayList<Button> = ArrayList()

    enum class Preset {
        PUMP,
        GUITAR,
        PAD
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDragStepBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //initUI()
    }

    override fun onStart() {
        super.onStart()
        initUI()
    }

    private fun initUI() {
        val sharedPref = this.getSharedPreferences(
            getString(R.string.singleArrowsPos), Context.MODE_PRIVATE
        )
        val gson = Gson()

        with(binding) {
            sizeBar.max = 230
            sizeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(seekBar: SeekBar, value: Int, b: Boolean) {
                    currentSize = value + 50 // Guardar tamaño actual
                    sizeText.text = "Size : ${currentSize} dp"
                    resizeArrows(currentSize)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })
            heightBar.max = 100
            heightBar.progress = (heightOffset * 100).toInt()
            heightBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(seekBar: SeekBar, value: Int, b: Boolean) {
                    heightOffset = value / 100.0f
                    heightText.text = "Height: ${(heightOffset * 100).toInt()}%"
                    // Reaplicar el preset actual
                    if (arrows.isNotEmpty()) {
                        applyPreset(currentPreset) // Aplicar preset actual
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })
            verticalBar.max = 100
            verticalBar.progress = (horizontalOffset * 100).toInt()
            verticalBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(seekBar: SeekBar, value: Int, b: Boolean) {
                    horizontalOffset = value / 100.0f
                    verticalText.text = "Horizontal: ${(horizontalOffset * 100).toInt()}%"
                    // Reaplicar el preset actual
                    if (arrows.isNotEmpty()) {
                        applyPreset(currentPreset) // Aplicar preset actual
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })
            spacingBar.max = 500
            spacingBar.progress = (spacingMultiplier * 100).toInt()
            spacingBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(seekBar: SeekBar, value: Int, b: Boolean) {
                    spacingMultiplier = value / 100.0f
                    spacingText.text = "Spacing: ${(spacingMultiplier * 100).toInt()}%"
                    // Reaplicar el preset actual
                    if (arrows.isNotEmpty()) {
                        applyPreset(currentPreset) // Aplicar preset actual
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })
            saveArrows.setOnClickListener {
                val save = ArrowsPositionPlace()
                save.size = sizeBar.progress + 50 + 20
                save.heightOffset = heightOffset
                save.horizontalOffset = horizontalOffset
                save.spacingMultiplier = spacingMultiplier
                save.currentPreset = currentPreset.name
                val positions = ArrayList<Point>()
                arrows.forEach { x ->
                    positions.add(Point(x.x.toInt(), x.y.toInt()))
                }
                save.positions = positions.toList().toTypedArray()

                with(sharedPref.edit()) {
                    putString(
                        getString(R.string.singleArrowsPos),
                        gson.toJson(save)
                    )
                    apply()
                }
                Toast.makeText(this@DragStepActivity, "Saved success!", Toast.LENGTH_SHORT).show()
            }

            buttonReset.setOnClickListener { resetPanel() }

            buttonPresetPump.setOnClickListener {
                currentPreset = Preset.PUMP
                applyPreset(Preset.PUMP)
            }
            buttonPresetGuitar.setOnClickListener {
                currentPreset = Preset.GUITAR
                applyPreset(Preset.GUITAR)
            }
            buttonPresetPad.setOnClickListener {
                currentPreset = Preset.PAD
                applyPreset(Preset.PAD)
            }

            drawArrows(false)
            val saveGson = sharedPref.getString(getString(R.string.singleArrowsPos), "")
            if (saveGson != "") {
                val obj: ArrowsPositionPlace =
                    gson.fromJson(saveGson, ArrowsPositionPlace::class.java)
                sizeBar.progress = obj.size - 50 - 20
                heightOffset = obj.heightOffset
                heightBar.progress = (heightOffset * 100).toInt()
                heightText.text = "Height: ${(heightOffset * 100).toInt()}%"
                horizontalOffset = obj.horizontalOffset
                verticalBar.progress = (horizontalOffset * 100).toInt()
                verticalText.text = "Horizontal: ${(horizontalOffset * 100).toInt()}%"
                spacingMultiplier = obj.spacingMultiplier
                spacingBar.progress = (spacingMultiplier * 100).toInt()
                spacingText.text = "Spacing: ${(spacingMultiplier * 100).toInt()}%"
                currentPreset = Preset.valueOf(obj.currentPreset)
                obj.positions.forEachIndexed { index, pos ->
                    arrows[index].x = pos.x.toFloat()
                    arrows[index].y = pos.y.toFloat()
                }
            } else {
                resetPanel()
            }
        }
    }

    private fun resetPanel() {
        // Restaurar todos los valores por defecto
        heightOffset = 0.5f
        spacingMultiplier = 1.0f
        horizontalOffset = 0.5f
        currentPreset = Preset.PUMP
        currentSize = 100

        // Actualizar los SeekBars
        binding.sizeBar.progress = 50 // currentSize - 50 = 100 - 50 = 50
        binding.heightBar.progress = (heightOffset * 100).toInt()
        binding.spacingBar.progress = (spacingMultiplier * 100).toInt()
        binding.verticalBar.progress = (horizontalOffset * 100).toInt()

        // Actualizar textos
        binding.sizeText.text = "Size : ${currentSize} dp"
        binding.heightText.text = "Height: ${(heightOffset * 100).toInt()}%"
        binding.spacingText.text = "Spacing: ${(spacingMultiplier * 100).toInt()}%"
        binding.verticalText.text = "Horizontal: ${(horizontalOffset * 100).toInt()}%"

        // Aplicar preset PUMP
        applyPreset(Preset.PUMP)
    }

    private fun pxToDp(px: Int): Int {
        return (px / Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun applyPreset(preset: Preset) {
        val layout = binding.relativeLayoutToDrag
        val width = layout.width
        val height = layout.height
        if (width == 0 || height == 0) {
            layout.post { applyPreset(preset) }
            return
        }
        when (preset) {
            Preset.PUMP -> {
                val padSize = currentSize.toFloat()

                // Centro de la pantalla considerando el pad en el centro 
                val centerX = (width - padSize) / 2f
                val centerY = (height - padSize) / 2f

                // Aplicar heightOffset al centro
                val adjustedCenterY = centerY + (height * (heightOffset - 0.5f) * 0.8f)
                // Aplicar horizontalOffset como posición horizontal independiente
                val finalCenterX = centerX + (width * (horizontalOffset - 0.5f) * 0.2f)

                // Para las esquinas, calcular la posición para que la esquina interna de cada pad (no el centro)
                // toque perfectamente el centro del pad central. Esto implica que la distancia desde el centro
                // al centro de la esquina es sqrt(2)*padSize, pero hay que corregir hacia adentro la mitad del tamaño.
                // Se busca: el centro de cada pad de esquina, está a sqrt(2) * spacingMultiplier * padSize del centro central
                val offset = (padSize + (padSize * spacingMultiplier)) / 2f

                // Ángulo de cada flecha de esquina respectiva
                val sqrt2over2 = 0.70710677f
                val dx = offset * sqrt2over2
                val dy = offset * sqrt2over2

                // Center (pad index 2)
                arrows[2].x = finalCenterX
                arrows[2].y = adjustedCenterY

                // DownLeft (pad index 0) - abajo e izquierda
                arrows[0].x = finalCenterX - dx
                arrows[0].y = adjustedCenterY + dy

                // UpLeft (pad index 1) - arriba e izquierda
                arrows[1].x = finalCenterX - dx
                arrows[1].y = adjustedCenterY - dy

                // UpRight (pad index 3) - arriba e derecha
                arrows[3].x = finalCenterX + dx
                arrows[3].y = adjustedCenterY - dy

                // DownRight (pad index 4) - abajo e derecha
                arrows[4].x = finalCenterX + dx
                arrows[4].y = adjustedCenterY + dy

                // No cambiar el tamaño del seekbar automáticamente
            }
            Preset.GUITAR -> {
                val size = currentSize.toFloat() // Usar tamaño actual
                val totalWidth = size * 5 * spacingMultiplier
                val startX = (width - totalWidth) / 2f + (width * (horizontalOffset - 0.5f) * 0.2f)
                val baseY = (height * heightOffset) - (size * 0.2f)

                for (i in 0 until 5) {
                    arrows[i].x = startX + i * size * spacingMultiplier
                    arrows[i].y = baseY
                }
                // No cambiar automáticamente el slider de tamaño
            }

            Preset.PAD -> {
                val sizeSquare = currentSize.toFloat() // Usar tamaño actual
                val spacingOffset = sizeSquare * (spacingMultiplier - 1.0f)

                arrows[0].x = 0f - spacingOffset + (width * (horizontalOffset - 0.5f) * 0.2f)
                arrows[0].y = height - sizeSquare - (height * (1 - heightOffset)) + spacingOffset

                arrows[1].x = 0f - spacingOffset + (width * (horizontalOffset - 0.5f) * 0.2f)
                arrows[1].y = (height * heightOffset) - sizeSquare - spacingOffset

                arrows[2].x = (width - sizeSquare) / 2f + (width * (horizontalOffset - 0.5f) * 0.2f)
                arrows[2].y = height / 2f + (height * (heightOffset - 0.5f) * 0.5f)

                arrows[3].x =
                    width - sizeSquare + spacingOffset + (width * (horizontalOffset - 0.5f) * 0.2f)
                arrows[3].y = (height * heightOffset) - sizeSquare - spacingOffset

                arrows[4].x =
                    width - sizeSquare + spacingOffset + (width * (horizontalOffset - 0.5f) * 0.2f)
                arrows[4].y = height - sizeSquare - (height * (1 - heightOffset)) + spacingOffset

                // No cambiar automáticamente el slider de tamaño
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun drawArrows(isDouble: Boolean) {
        // Limpiar flechas existentes antes de crear nuevas
        clearArrows()

        stepInfo.forEach { x ->
            val iv = Button(this)
            iv.background = Drawable.createFromXml(resources, resources.getXml(x))
            iv.setOnTouchListener(move)
            arrows.add(iv)
            binding.relativeLayoutToDrag.addView(iv)
        }

        // Aplicar el tamaño actual después de crear las flechas
        resizeArrows(currentSize)
    }

    private fun clearArrows() {
        // Remover todas las flechas del layout
        arrows.forEach { arrow ->
            binding.relativeLayoutToDrag.removeView(arrow)
        }
        // Limpiar la lista
        arrows.clear()
    }

    private fun resizeArrows(size: Int) {
        val pixel = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            size.toFloat(), resources.displayMetrics
        ).toInt()

        arrows.forEach { arrow ->
            val lp = arrow.layoutParams as RelativeLayout.LayoutParams
            lp.width = pixel
            lp.height = pixel
            arrow.layoutParams = lp
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    val move = View.OnTouchListener { view, event ->
        val x = event.rawX.toInt()
        val y = event.rawY.toInt()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                _xDelta = (event.rawX - view.x).toInt()
                _yDelta = (event.rawY - view.y).toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                val newX = event.rawX - _xDelta
                val newY = event.rawY - _yDelta
                val maxX = binding.relativeLayoutToDrag.width - view.width
                val maxY = binding.relativeLayoutToDrag.height - view.height
                view.x = newX.coerceIn(0f, maxX.toFloat())
                view.y = newY.coerceIn(0f, maxY.toFloat())
            }
        }
        true
    }
}
