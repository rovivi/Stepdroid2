package com.kyagamy.step.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Category (
    @PrimaryKey
    val name:String,
    val path:String,
    val banner:String?,
    val music:String?

)
