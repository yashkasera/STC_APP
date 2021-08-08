package com.mstc.mstcapp.model.remoteKeys

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "PROJECT_REMOTE_KEYS")
data class ProjectRemoteKey(
    @PrimaryKey
    val projectId: String,
    val prevKey: Int?,
    val nextKey: Int?,
)