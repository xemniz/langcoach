package com.xemniz.langcoach.data.db

import androidx.room3.RoomDatabase

expect fun getDatabaseBuilder(): RoomDatabase.Builder<LangCoachDatabase>
