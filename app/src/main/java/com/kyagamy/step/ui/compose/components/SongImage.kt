package com.kyagamy.step.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyagamy.step.room.entities.Song

@Composable
fun SongImage(
    song: Song,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current

    if (song.BANNER_SONG.isNotEmpty()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.PATH_SONG + "/" + song.BANNER_SONG)
                .crossfade(true)
                .build(),
            contentDescription = song.TITLE,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Cyan.copy(alpha = 0.7f),
                        Color.Blue.copy(alpha = 0.5f)
                    )
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "♪",
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SongImagePreview() {
    MaterialTheme {
        SongImage(
            song = Song(
                song_id = 1,
                TITLE = "Sample Song",
                ARTIST = "Sample Artist",
                PATH_SONG = "",
                BANNER_SONG = "",
                PREVIEWVID = "",
                DISPLAYBPM = "140",
                GENRE = "Pop",
                MUSIC = "",
                SAMPLESTART = 0.0,
                SAMPLELENGTH = 30.0,
                SUBTITLE = "",
                BACKGROUND = "",
                CDIMAGE = "",
                CDTITLE = "",
                SONGTYPE = "",
                SONGCATEGORY = "",
                VERSION = "",
                PATH_File = "",
                catecatecate = "",
                CATEGORY_LINK = null
            )
        )
    }
}