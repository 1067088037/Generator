package edu.scut.generator.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import edu.scut.generator.global.debug
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

@Database(entities = [Generator::class], version = 1, exportSchema = false)
abstract class GeneratorDataBase : RoomDatabase() {
    abstract val generatorDao: GeneratorDao

    @Synchronized
    fun addGenerator(vararg generator: Generator) {
        CoroutineScope(Dispatchers.IO).launch {
            if (this@GeneratorDataBase.isOpen) generatorDao.add(*generator)
        }
    }

    @Synchronized
    fun updateGenerator(generator: Generator) {
        CoroutineScope(Dispatchers.IO).launch {
            if (this@GeneratorDataBase.isOpen) generatorDao.update(generator)
        }
    }

    @Synchronized
    fun deleteGenerator(generator: Generator) {
        CoroutineScope(Dispatchers.IO).launch {
            if (this@GeneratorDataBase.isOpen) generatorDao.delete(generator)
        }
    }

    @Synchronized
    fun deleteAll() {
        CoroutineScope(Dispatchers.IO).launch {
            if (this@GeneratorDataBase.isOpen) generatorDao.deleteAll()
        }
    }

    @Synchronized
    fun deleteGeneratorByUuid(uuid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (this@GeneratorDataBase.isOpen) generatorDao.deleteByUuid(uuid)
        }
    }

    @Synchronized
    fun getGenerator(uuid: String): Generator? {
        return generatorDao.getGenerator(uuid)
    }

    @Synchronized
    fun getAllGenerator(): List<Generator> {
        return generatorDao.getAllGenerator()
    }

    companion object {
        private lateinit var instance: GeneratorDataBase

        fun init(context: Context) {
            instance =
                Room.databaseBuilder(context, GeneratorDataBase::class.java, "main.sql").build()
            CoroutineScope(Dispatchers.IO).launch {
                instance.getAllGenerator()
            }
        }

        fun getInstance(): GeneratorDataBase = instance
    }

}