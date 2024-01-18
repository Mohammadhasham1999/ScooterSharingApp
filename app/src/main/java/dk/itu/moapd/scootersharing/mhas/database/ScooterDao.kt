package dk.itu.moapd.scootersharing.mhas.database

import androidx.room.*
import dk.itu.moapd.scootersharing.mhas.models.Scooter
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface ScooterDao {
    @Query("SELECT * FROM scooter")
    fun getScooters() : Flow<List<Scooter>>

    @Query("SELECT * FROM scooter WHERE id = :id")
    suspend fun getScooter(id: UUID) : Scooter?

    @Insert
    suspend fun addScooter(scooter: Scooter)

    @Update
    suspend fun updateScooter(scooter: Scooter)

    @Delete
    suspend fun deleteScooter(scooter: Scooter)
}