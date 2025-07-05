package com.kyagamy.step.views

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyagamy.step.room.entities.Category

data class CategoryStats(
    val songCount: Int,
    val totalTime: Double,
    val genres: Int
)

@Composable
fun CategoryInfo(
    category: Category,
    stats: CategoryStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📺 Channel: ",
                color = Color.Yellow,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = category.name ?: "Unknown",
                color = Color.Yellow,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoItem(
                label = "Songs",
                value = stats.songCount.toString(),
                icon = "🎵"
            )

            InfoItem(
                label = "Genres",
                value = stats.genres.toString(),
                icon = "🎭"
            )

//            InfoItem(
//                label = "Duration",
//                value = "${String.format("%.1f", stats.totalTime / 60)}m",
//                icon = "⏱️"
//            )
        }
    }
}

@Composable
fun InfoItem(
    label: String,
    value: String,
    colr: Color = Color.Yellow,
    icon: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        Text(
//            text = icon,
//            fontSize = 20.sp
//        )
        Text(
            text = value,
            color = Color.Yellow,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Yellow.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}