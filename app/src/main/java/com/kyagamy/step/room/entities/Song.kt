package com.kyagamy.step.room.entities
import androidx.room.*

@Entity
class Song (
    @PrimaryKey(autoGenerate = true)
    val uid:Int,
    val TITLE:String,
    val SUBTITLE:String,
    val ARTIST:String,
    val BANNER:String,
    val BACKGROUND:String,
    val CDIMAGE:String,
    val MUSIC:String,
    val OFFSET:String,
    val VERSION:String,
    val PATH:String
)
