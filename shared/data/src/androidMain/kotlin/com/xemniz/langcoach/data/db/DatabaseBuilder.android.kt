package com.xemniz.langcoach.data.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

actual fun getDatabaseBuilder(): RoomDatabase.Builder<LangCoachDatabase> {
    throw IllegalStateException("Use getDatabaseBuilder(context) on Android")
}

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<LangCoachDatabase> {
    val dbFile = context.getDatabasePath("langcoach.db")
    return Room.databaseBuilder<LangCoachDatabase>(
        context = context,
        name = dbFile.absolutePath,
    )
}
