package com.kyagamy.step.room.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
class Level (
    @PrimaryKey(autoGenerate = true)
    val id:Int,
    val index:Int,
    val METER:String,
    val CREDIT:String,
    val STEPSTYPE:String,
    val DESCRIPTION:String,
    val CHARTNAME:String,
    val song_fkid:Int,
    @Embedded
    val song:Song?
)
