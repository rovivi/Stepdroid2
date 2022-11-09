package com.kyagamy.step

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kyagamy.step.common.Common
import com.kyagamy.step.common.step.Parsers.FileSSC
import com.kyagamy.step.databinding.ActivityLoadingSongBinding
import com.kyagamy.step.room.entities.Category
import com.kyagamy.step.room.entities.Level
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.viewModels.CategoryViewModel
import com.kyagamy.step.viewModels.LevelViewModel
import com.kyagamy.step.viewModels.SongViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.lang.Exception

class LoadingSongActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoadingSongBinding
    private lateinit var songsModel: SongViewModel
    private lateinit var categoryModel: CategoryViewModel
    private lateinit var levelModel: LevelViewModel
    private lateinit var text: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingSongBinding.inflate(layoutInflater)

        setContentView(R.layout.activity_loading_song)

        setSupportActionBar(binding.toolbar)
        songsModel = ViewModelProvider(this)[SongViewModel::class.java]
        categoryModel = ViewModelProvider(this)[CategoryViewModel::class.java]
        levelModel = ViewModelProvider(this)[LevelViewModel::class.java]

        text = findViewById(R.id.textViewSong)
        lifecycleScope.launch {
            reloadSongs()
        }
    }

    private suspend fun reloadSongs() {
        delay(500)
        val sharedPref = this.getSharedPreferences("pref", Context.MODE_PRIVATE)
        var basePath = sharedPref.getString(this.getString(R.string.base_path), "Error path")

        basePath += "${File.separator}stepdroid${File.separator}songs"

        val file = File(basePath)
        if (!file.exists()) {
            file.mkdir()
            Toast.makeText(this, "No songs yet :(, please put in $basePath", Toast.LENGTH_LONG)
                .show()
            finish()
        }
        Toast.makeText(this, "" + file.isDirectory, Toast.LENGTH_LONG).show()

        val category = file.listFiles()
        if (category != null && category!!.isEmpty()) {
            Toast.makeText(this, "No songs yet :(, please put in $basePath", Toast.LENGTH_LONG)
                .show()
            finish()
        }
        //clean all category,songs and levels
        categoryModel.deleteAll()
        songsModel.deleteAll()
        levelModel.deleteAll()

        //add default categorysogs hardCoded needed NO
//        val category = Category(
//            cate.name,
//            cate.path,
//            if (banner.exists()) banner.path else null,
//            if (sound.exists()) sound.path else null
//        )

        var songId = 1


        category?.filter { x -> x.isDirectory }?.forEach { cate ->
            run {
                // se valida si existe una carpeta de info con sonidito
                var sound =
                    File("${cate.absolutePath + File.separator}info${File.separator}sound.ogg")
                if (!sound.exists())
                    sound =
                        File("${cate.absolutePath + File.separator}info${File.separator}sound.mp3")
                if (!sound.exists())
                    sound =
                        File("${cate.absolutePath + File.separator}info${File.separator}sound.wav")
                val banner = File("${cate.absolutePath + File.separator}banner.png")
                val category = Category(
                    cate.name,
                    cate.path,
                    if (banner.exists()) banner.path else null,
                    if (sound.exists()) sound.path else null
                )
                //No se añade si no tiene canciones
                var hasSong = false
                var count = 0
                val listFilesCategory =cate.listFiles()
                //binding.textViewCategory
                //binding.textViewCategory.text= cate.name
                listFilesCategory ?.filter { x -> x.isDirectory }?.forEach { subFolder ->
                    run {
                        text.text = subFolder.name
                        val fileSong = subFolder.listFiles()
                        val songFile = fileSong.firstOrNull { ssc ->
                            ssc.name.toLowerCase().endsWith("ssc")
                        }
                        if (songFile != null && songFile.isFile) {
                            hasSong = true
                            try {
                                //parse and save songs info
                                val data: String =
                                    Common.convertStreamToString(FileInputStream(songFile.path))!!
                                val stepFileLoaded = FileSSC(data, count++)
                                val parsedFile = stepFileLoaded.parseData(true)
                                val song = Song(
                                    songId++,
                                    parsedFile.songMetadata["TITLE"] ?: "",
                                    parsedFile.songMetadata["SUBTITLE"] ?: "",
                                    parsedFile.songMetadata["ARTIST"] ?: "",
                                    parsedFile.songMetadata["BANNER"] ?: "",
                                    parsedFile.songMetadata["BACKGROUND"] ?: "",
                                    parsedFile.songMetadata["CDIMAGE"] ?: "",
                                    parsedFile.songMetadata["CDTITLE"] ?: "",
                                    parsedFile.songMetadata["MUSIC"] ?: "",
                                    parsedFile.songMetadata["SAMPLESTART"]?.toDouble()!!,
                                    parsedFile.songMetadata["SAMPLELENGTH"]?.toDouble()!!,
                                    parsedFile.songMetadata["SONGTYPE"] ?: "",
                                    parsedFile.songMetadata["SONGCATEGORY"] ?: "",
                                    parsedFile.songMetadata["VERSION"] ?: "",
                                    subFolder.path,
                                    songFile.path,
                                    parsedFile.songMetadata["GENRE"] ?: "",
                                    parsedFile.songMetadata["PREVIEWVID"] ?: "",
                                    parsedFile.getDisplayBPM().toString(),
                                    category.name,
                                    category
                                )
                                songsModel.insert(song)
                                //insert level
                                parsedFile.levelList?.forEach { level ->
                                    levelModel.insert(
                                        Level(
                                            0,
                                            level.index,
                                            level.METER,
                                            level.CREDIT,
                                            level.STEPSTYPE,
                                            level.DESCRIPTION,
                                            level.CHARTNAME,
                                            song.song_id,
                                            song
                                        )
                                    )
                                }
                                delay(1)
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }
                }
                if (hasSong)
                    categoryModel.insert(category)
            }
        }
        text.text = "Success!"
        delay(500)
        finish()
    }
}