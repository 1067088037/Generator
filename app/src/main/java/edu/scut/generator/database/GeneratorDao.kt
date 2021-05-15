package edu.scut.generator.database

import androidx.room.*
import java.util.*

@Dao
interface GeneratorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(vararg generators: Generator)

    @Update
    fun update(generator: Generator)

    @Delete
    fun delete(generator: Generator)

    @Query("DELETE FROM generator")
    fun deleteAll()

    @Query("DELETE FROM generator WHERE uuid = :uuid")
    fun deleteByUuid(uuid: String)

    @Query("SELECT * FROM generator WHERE uuid = :uuid")
    fun getGenerator(uuid: String): Generator?

    @Query("SELECT * FROM generator")
    fun getAllGenerator(): List<Generator>

}