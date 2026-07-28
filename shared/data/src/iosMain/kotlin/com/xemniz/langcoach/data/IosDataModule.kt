package com.xemniz.langcoach.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.xemniz.langcoach.data.db.LangCoachDatabase
import com.xemniz.langcoach.data.db.getDatabaseBuilder
import com.xemniz.langcoach.data.prefs.createIosProfileDataStore
import org.koin.dsl.module

val iosDataModule = module {
    single { SecureStorage() }
    single<DataStore<Preferences>> { createIosProfileDataStore() }
    single<LangCoachDatabase> {
        getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
}
