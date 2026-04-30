package com.xemniz.langcoach.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import platform.Foundation.NSHomeDirectory

fun createIosProfileDataStore(): DataStore<Preferences> {
    val path = NSHomeDirectory() + "/Documents/$PROFILE_PREFS_FILENAME"
    return createProfileDataStore { path }
}
