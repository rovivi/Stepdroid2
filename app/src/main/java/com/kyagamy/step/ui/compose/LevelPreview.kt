package com.kyagamy.step.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kyagamy.step.room.entities.Level
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.ui.ui.theme.StepDroidTheme

@Preview(showBackground = true)
@Composable
fun LevelItemPreview() {
    StepDroidTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            LevelItem(
                level = sampleLevel(),
                bigMode = false,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LevelItemBigModePreview() {
    StepDroidTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            LevelItem(
                level = sampleLevel(),
                bigMode = true,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LevelListPreview() {
    StepDroidTheme {
        LevelList(
            levels = sampleLevels(),
            bigMode = false,
            onItemClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LevelGridPreview() {
    StepDroidTheme {
        LevelGrid(
            levels = sampleLevels(),
            columns = 4,
            bigMode = false,
            onItemClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}

// Sample data for previews
private fun sampleLevel() = Level(
    id = 1,
    index = 0,
    METER = 5,
    CREDIT = "Test Credit",
    STEPSTYPE = "pump-single",
    DESCRIPTION = "Single chart",
    CHARTNAME = "Test Chart",
    song_fkid = 1,
    song = sampleSong()
)

private fun sampleLevels(): List<Level> = listOf(
    Level(
        id = 1,
        index = 0,
        METER = 3,
        CREDIT = "Test Credit",
        STEPSTYPE = "pump-single",
        DESCRIPTION = "Single chart",
        CHARTNAME = "Test Chart",
        song_fkid = 1,
        song = sampleSong()
    ),
    Level(
        id = 2,
        index = 1,
        METER = 7,
        CREDIT = "Test Credit",
        STEPSTYPE = "pump-double",
        DESCRIPTION = "Double chart",
        CHARTNAME = "Test Chart",
        song_fkid = 1,
        song = sampleSong()
    ),
    Level(
        id = 3,
        index = 2,
        METER = 12,
        CREDIT = "Test Credit",
        STEPSTYPE = "pump-single",
        DESCRIPTION = "DP",
        CHARTNAME = "Test Chart",
        song_fkid = 1,
        song = sampleSong()
    ),
    Level(
        id = 4,
        index = 3,
        METER = 15,
        CREDIT = "Test Credit",
        STEPSTYPE = "pump-double",
        DESCRIPTION = "Double chart",
        CHARTNAME = "coop",
        song_fkid = 1,
        song = sampleSong()
    ),
    Level(
        id = 5,
        index = 4,
        METER = 9,
        CREDIT = "Test Credit",
        STEPSTYPE = "pump-single",
        DESCRIPTION = "Single chart",
        CHARTNAME = "Test Chart",
        song_fkid = 1,
        song = sampleSong()
    ),
    Level(
        id = 6,
        index = 5,
        METER = 18,
        CREDIT = "Test Credit",
        STEPSTYPE = "pump-double",
        DESCRIPTION = "DP",
        CHARTNAME = "Test Chart",
        song_fkid = 1,
        song = sampleSong()
    )
)

private fun sampleSong() = Song(
    song_id = 1,
    TITLE = "Test Song",
    SUBTITLE = "Test Subtitle",
    ARTIST = "Test Artist",
    BANNER_SONG = "",
    BACKGROUND = "",
    CDIMAGE = "",
    CDTITLE = "",
    MUSIC = "",
    SAMPLESTART = 0.0,
    SAMPLELENGTH = 0.0,
    SONGTYPE = "",
    SONGCATEGORY = "",
    VERSION = "",
    PATH_SONG = "",
    PATH_File = "",
    GENRE = "",
    PREVIEWVID = "",
    DISPLAYBPM = "",
    catecatecate = "",
    CATEGORY_LINK = null
)