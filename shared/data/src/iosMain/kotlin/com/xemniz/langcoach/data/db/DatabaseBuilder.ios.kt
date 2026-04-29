package com.xemniz.langcoach.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import platform.Foundation.NSHomeDirectory

actual fun getDatabaseBuilder(): RoomDatabase.Builder<LangCoachDatabase> {
    val dbFilePath = NSHomeDirectory() + "/Documents/langcoach.db"
    return Room.databaseBuilder<LangCoachDatabase>(
        name = dbFilePath,
    )
}
