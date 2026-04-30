package com.xemniz.langcoach.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

fun createAndroidProfileDataStore(context: Context): DataStore<Preferences> {
    val path = context.filesDir.resolve("datastore/$PROFILE_PREFS_FILENAME").absolutePath
    return createProfileDataStore { path }
}
