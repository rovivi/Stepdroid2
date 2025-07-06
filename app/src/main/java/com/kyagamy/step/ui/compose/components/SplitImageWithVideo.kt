package com.kyagamy.step.ui.compose.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.kyagamy.step.room.entities.Song
import kotlinx.coroutines.delay
import java.io.File
import android.widget.VideoView

@Composable
fun SplitImageWithVideo(
    song: Song,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val imgPath = "${song.PATH_SONG}/${song.BANNER_SONG}"
    val vidPath = "${song.PATH_SONG}/${song.PREVIEWVID}"
    val showVideo = remember(vidPath) { File(vidPath).exists() }

    var trigger by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(song) {
        delay(155)
        trigger = 1f
    }

    val bitmap by produceState<ImageBitmap?>(null, imgPath) {
        val loader = ImageLoader(ctx)
        val req = ImageRequest.Builder(ctx).data(imgPath).build()
        val res = loader.execute(req)
        if (res is SuccessResult) value = (res.drawable.toBitmap()).asImageBitmap()
    }
    if (bitmap == null) return

    BoxWithConstraints(modifier = modifier.clip(RoundedCornerShape(20.dp))) {
        val w = maxWidth
        val h = maxHeight
        val halfW = w / 2
        val anim = animateDpAsState(
            targetValue = (halfW * trigger).coerceAtMost(halfW),
            animationSpec = tween(
                1500,
                easing = FastOutSlowInEasing
            )
        )

        if (showVideo) {
            AndroidView({
                VideoView(it).apply {
                    setVideoPath(vidPath)
                    setOnPreparedListener { mp -> mp.isLooping = true; mp.start() }
                }
            }, Modifier.fillMaxSize())
        }

        Canvas(Modifier.fillMaxSize()) {
            val imgW = bitmap!!.width
            val imgH = bitmap!!.height
            val srcW = imgW / 2

            drawImage(
                image = bitmap!!,
                srcOffset = IntOffset(0, 0),
                srcSize = IntSize(srcW, imgH),
                dstOffset = IntOffset(-anim.value.roundToPx(), 0),
                dstSize = IntSize(halfW.roundToPx(), h.roundToPx())
            )
            drawImage(
                image = bitmap!!,
                srcOffset = IntOffset(srcW, 0),
                srcSize = IntSize(srcW, imgH),
                dstOffset = IntOffset((halfW + anim.value).roundToPx(), 0),
                dstSize = IntSize(halfW.roundToPx(), h.roundToPx())
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplitImageWithVideoPreview() {
    MaterialTheme {
        SplitImageWithVideo(
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