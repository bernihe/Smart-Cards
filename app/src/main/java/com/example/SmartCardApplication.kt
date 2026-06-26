package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.SmartCardRepository

class SmartCardApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: SmartCardRepository

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "smart_card_db"
        )
        .fallbackToDestructiveMigration()
        .build()
        
        repository = SmartCardRepository(database.smartCardDao())
    }
}
