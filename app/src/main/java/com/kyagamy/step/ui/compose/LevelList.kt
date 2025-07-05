package com.kyagamy.step.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyagamy.step.room.entities.Level

@Composable
fun LevelList(
    levels: List<Level>,
    bigMode: Boolean = false,
    onItemClick: (Level) -> Unit = {},
    modifier: Modifier = Modifier
) {

    if (bigMode) {
        // For big mode, use a grid layout
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize()
        ) {
            itemsIndexed(levels) { index, level ->
                LevelItem(
                    level = level,
                    bigMode = true,
                    onClick = { onItemClick(level) }
                )
            }
        }
    } else {
        // For normal mode, use a horizontal scrollable row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            itemsIndexed(levels) { index, level ->

                LevelItem(
                    level = level,
                    bigMode = false,
                    onClick = { onItemClick(level) }
                )

            }
        }
    }
}

@Composable
fun LevelGrid(
    levels: List<Level>,
    columns: Int = 3,
    bigMode: Boolean = false,
    onItemClick: (Level) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(levels) { index, level ->
            LevelItem(
                level = level,
                bigMode = bigMode,
                onClick = { onItemClick(level) }
            )
        }
    }
}