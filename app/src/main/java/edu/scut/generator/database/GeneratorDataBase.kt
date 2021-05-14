package edu.scut.generator.database

import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

@Database(entities = [Generator::class], version = 1, exportSchema = false)
abstract class GeneratorDataBase : RoomDatabase() {
    abstract val generatorDao: GeneratorDao

    @Synchronized
    fun addGenerator(vararg generator: Generator) {
        CoroutineScope(Dispatchers.IO).launch {
            generatorDao.add(*generator)
        }
    }

    @Synchronized
    fun updateGenerator(generator: Generator) {
        CoroutineScope(Dispatchers.IO).launch {
            generatorDao.update(generator)
        }
    }

    @Synchronized
    fun deleteGenerator(generator: Generator) {
        CoroutineScope(Dispatchers.IO).launch {
            generatorDao.delete(generator)
        }
    }

    @Synchronized
    suspend fun getGenerator(uuid: String): Generator? {
        return generatorDao.getGenerator(uuid)
    }

    suspend fun getAllGenerator(): List<Generator> {
        return generatorDao.getAllGenerator()
    }

}