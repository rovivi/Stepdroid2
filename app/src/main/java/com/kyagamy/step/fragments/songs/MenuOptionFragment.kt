package com.kyagamy.step.fragments.songs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.kyagamy.step.common.SettingsGameGetter
import com.kyagamy.step.common.step.CommonGame.ParamsSong
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MenuOptionFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    var skins: ArrayList<*> = ArrayList<Any>()
    var indexNS = 0
    var autoVelocity: Int = ParamsSong.av
    var speed: Float = ParamsSong.speed

    lateinit var settingsGameGetter: SettingsGameGetter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingsGameGetter = SettingsGameGetter(requireActivity().applicationContext)

        autoVelocity = settingsGameGetter.getValueInt(SettingsGameGetter.AV)
        ParamsSong.av = autoVelocity
        ParamsSong.speed = settingsGameGetter.getValueFloat(SettingsGameGetter.SPEED)
        speed = ParamsSong.speed

        indexNS = ParamsSong.skinIndex

        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    GameOptionsScreen(
                        initialAutoVelocity = autoVelocity,
                        initialSpeed = speed,
                        onAutoVelocityChange = { changeAutoVelocity(it) },
                        onSpeedChange = { changeSpeed(it) }
                    )
                }
            }
        }
    }

    private fun changeAutoVelocity(plusValue: Int) {
        autoVelocity += plusValue
        if (autoVelocity < 200) autoVelocity = 200
        if (autoVelocity > 1200) autoVelocity = 1200
        settingsGameGetter.saveSetting(SettingsGameGetter.AV, autoVelocity)
        ParamsSong.av = autoVelocity
    }

    private fun changeSpeed(plusValue: Float) {
        speed += plusValue
        if (speed < 0.25) speed = 0.25f
        if (speed > 10) speed = 10f
        settingsGameGetter.saveSetting(SettingsGameGetter.SPEED, speed)
        ParamsSong.speed = speed
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MenuOptionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

@Composable
fun GameOptionsScreen(
    initialAutoVelocity: Int,
    initialSpeed: Float,
    onAutoVelocityChange: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    var autoVelocity by remember { mutableIntStateOf(initialAutoVelocity) }
    var speed by remember { mutableFloatStateOf(initialSpeed) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Auto Velocity Section
            SettingSection(
                title = "Auto Velocity",
                value = autoVelocity.toString(),
                onValueChange = { delta ->
                    autoVelocity += delta
                    onAutoVelocityChange(delta)
                },
                increments = listOf(1, 10, 100),
                decrements = listOf(-1, -10, -100),
                cardColor = Color(0xFF4CAF50)
            )

            // Speed Section  
            SettingSection(
                title = "Speed",
                value = String.format("%.2f", speed),
                onValueChange = { delta ->
                    speed += delta
                    onSpeedChange(delta)
                },
                increments = listOf(0.25f, 0.5f, 1f),
                decrements = listOf(-0.25f, -0.5f, -1f),
                cardColor = Color(0xFF2196F3)
            )

            // Placeholder for future features
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Judgment - Rush - Skins\n(Coming Soon)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun <T : Number> SettingSection(
    title: String,
    value: String,
    onValueChange: (T) -> Unit,
    increments: List<T>,
    decrements: List<T>,
    cardColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    color = cardColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Increment buttons
                increments.forEach { increment ->
                    ActionButton(
                        text = "+${increment}",
                        onClick = { onValueChange(increment) },
                        color = cardColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Decrement buttons
                decrements.forEach { decrement ->
                    ActionButton(
                        text = "${decrement}",
                        onClick = { onValueChange(decrement) },
                        color = cardColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    color: Color
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(64.dp)
            .height(40.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
