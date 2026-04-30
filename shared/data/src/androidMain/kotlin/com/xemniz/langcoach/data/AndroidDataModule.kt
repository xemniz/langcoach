package com.xemniz.langcoach.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.xemniz.langcoach.data.db.LangCoachDatabase
import com.xemniz.langcoach.data.db.getDatabaseBuilder
import com.xemniz.langcoach.data.prefs.createAndroidProfileDataStore
import org.koin.dsl.module

val androidDataModule = module {
    single { SecureStorage(get<Context>()) }
    single<DataStore<Preferences>> { createAndroidProfileDataStore(get<Context>()) }
    single<LangCoachDatabase> {
        getDatabaseBuilder(get<Context>())
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
}
