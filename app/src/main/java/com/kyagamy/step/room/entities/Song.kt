package com.kyagamy.step.room.entities
import androidx.room.*

@Entity
class Song (
    @PrimaryKey(autoGenerate = true)
    val song_id:Int,
    val TITLE:String,
    val SUBTITLE:String,
    val ARTIST:String,
    val BANNER_SONG:String,
    val BACKGROUND:String,
    val CDIMAGE:String,
    val CDTITLE:String,
    val MUSIC:String,
    val SAMPLESTART:Double,
    val SAMPLELENGTH:Double,
    val SONGTYPE:String,
    val SONGCATEGORY:String,
    val VERSION:String,
    val PATH_SONG:String,
    val PATH_File:String,
    val GENRE:String,
    val PREVIEWVID:String,
    val DISPLAYBPM:String,
    val catecatecate:String,
    @Embedded
    val CATEGORY_LINK: Category?



) {

}
