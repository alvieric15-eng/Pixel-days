package com.pixeldays.app.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile private var instance: PixelDatabase? = null
    fun get(context: Context): PixelDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext, PixelDatabase::class.java, "pixel-days.db"
        ).fallbackToDestructiveMigration().build().also { instance = it }
    }
}
