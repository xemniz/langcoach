package com.xemniz.langcoach.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [VocabItem::class], version = 1)
@ConstructedBy(LangCoachDatabaseConstructor::class)
abstract class LangCoachDatabase : RoomDatabase() {
    abstract fun vocabDao(): VocabDao
}

// Room KSP generates the actual implementation.
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object LangCoachDatabaseConstructor : RoomDatabaseConstructor<LangCoachDatabase> {
    override fun initialize(): LangCoachDatabase
}
