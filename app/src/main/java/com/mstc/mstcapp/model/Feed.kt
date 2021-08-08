package com.mstc.mstcapp.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "FEED")
class Feed(
    @PrimaryKey
    @SerializedName("_id")
    val id: String,
    val title: String,
    val description: String,
    val link: String,
    val image: String,
){
    @Ignore
    var expand:Boolean = false
}