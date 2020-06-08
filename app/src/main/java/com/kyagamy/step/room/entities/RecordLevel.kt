package com.kyagamy.step.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class RecordLevel (
    @PrimaryKey
    val levelName:String,
    val score :Double

)