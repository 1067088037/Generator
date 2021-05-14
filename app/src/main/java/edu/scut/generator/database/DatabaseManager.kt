package edu.scut.generator.database

import android.content.Context
import androidx.room.Room

object DatabaseManager {
    lateinit var generatorDataBase: GeneratorDataBase

    fun init(context: Context) {
        if (this::generatorDataBase.isInitialized.not()) generatorDataBase =
            Room.databaseBuilder(context, GeneratorDataBase::class.java, "main").build()
    }
}