package dk.itu.moapd.scootersharing.mhas.repositories

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import dk.itu.moapd.scootersharing.mhas.database.RidesDB
import dk.itu.moapd.scootersharing.mhas.models.Scooter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.*

private const val DATABASE_NAME = "scooter-database"

class ScooterRepository private constructor(context: Context) : RoomDatabase.Callback() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)


    private val database : RidesDB = Room
        .databaseBuilder(
            context.applicationContext,
            RidesDB::class.java,
            DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        //.createFromAsset("scooters.db")
        .build()

    fun prepopulate() {
        for (scooter in INITIAL_SCOOTERS) {
            coroutineScope.launch {
                addScooter(scooter)
            }
        }
    }


    fun getScooters() : Flow<List<Scooter>> = database.scooterDao().getScooters()

    suspend fun getScooter(id: UUID): Scooter? = database.scooterDao().getScooter(id)

    suspend fun addScooter(scooter : Scooter) = database.scooterDao().addScooter(scooter)

    suspend fun updateScooter(scooter: Scooter) = database.scooterDao().updateScooter(scooter)

    suspend fun deleteScooter(scooter: Scooter) = database.scooterDao().deleteScooter(scooter)

    companion object {
        private var INSTANCE : ScooterRepository? = null
        private val storage = Firebase.storage


        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = ScooterRepository(context)
            }
        }

        fun get() : ScooterRepository {
            return INSTANCE ?:
            throw IllegalStateException("ScooterRepository must be initialized")
        }


        private val INITIAL_SCOOTERS = listOf(
            Scooter(
                UUID.randomUUID(),
                "Scooter01",
                "Baltorpvej 158, 2750 Ballerup, Danmark",
                System.currentTimeMillis(),
                storage.reference.child("images/Scooter01.webp").path,
                55.729633,
                12.344175,
                null,
                isStarted = false,
                null,
                isInGeoFence = false
            ),
            Scooter(
                UUID.randomUUID(),
                "Scooter02",
                "Baltorpvej 159, 2750 Ballerup, Danmark",
                System.currentTimeMillis(),
                storage.reference.child("images/Scooter02.webp").path,
                55.72866,
                12.346824,
                null,
                isStarted = false,
                null,
                isInGeoFence = false
            ),
            Scooter(
                UUID.randomUUID(),
                "Scooter03",
                "Baltorpvej 160, 2750 Ballerup, Danmark",
                System.currentTimeMillis(),
                storage.reference.child("images/Scooter03.webp").path,
                55.729597,
                12.343913,
                null,
                isStarted = false,
                null,
                isInGeoFence = false
            )
        )
    }
}