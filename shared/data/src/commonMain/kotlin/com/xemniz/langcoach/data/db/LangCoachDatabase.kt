package com.xemniz.langcoach.data.db

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [
        VocabItem::class,
        ErrorCategory::class,
        ErrorInstance::class,
        SessionSummary::class,
        UsageEntry::class,
        UserModel::class,
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
@ConstructedBy(LangCoachDatabaseConstructor::class)
abstract class LangCoachDatabase : RoomDatabase() {
    abstract fun vocabDao(): VocabDao
    abstract fun errorCategoryDao(): ErrorCategoryDao
    abstract fun errorInstanceDao(): ErrorInstanceDao
    abstract fun sessionDao(): SessionDao
    abstract fun usageDao(): UsageDao
    abstract fun userModelDao(): UserModelDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object LangCoachDatabaseConstructor : RoomDatabaseConstructor<LangCoachDatabase> {
    override fun initialize(): LangCoachDatabase
}
