package com.mstc.mstcapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mstc.mstcapp.model.explore.BoardMember
import com.mstc.mstcapp.model.resource.Detail
import com.mstc.mstcapp.model.resource.Resource
import com.mstc.mstcapp.model.resource.Roadmap

@Database(entities = [
    Detail::class,
    Roadmap::class,
    Resource::class,
    BoardMember::class
], version = 1, exportSchema = false)
abstract class STCDatabase : RoomDatabase() {
    abstract fun databaseDao(): DatabaseDao

    companion object {

        @Volatile
        private var INSTANCE: STCDatabase? = null

        fun getInstance(context: Context): STCDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                STCDatabase::class.java, "STCDatabase.db"
            )
                .build()
    }
}