package dk.itu.moapd.scootersharing.mhas.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dk.itu.moapd.scootersharing.mhas.models.Scooter
import java.util.*

@Database(entities = [Scooter::class], version = 5, exportSchema = true)
//@TypeConverters(ScooterTypeConverters::class)
abstract class RidesDB : RoomDatabase() {

    abstract fun scooterDao(): ScooterDao
}




