package dk.itu.moapd.scootersharing.mhas.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 * @param name represents the name of the Scooter
 * @param location represents the location of the Scooter
 * @param timestamp denotes the time
 */
@Entity
 data class Scooter(@PrimaryKey val id : UUID, var name: String, var location: String, var timestamp: Long, var url: String, var latitude: Double, var longitude: Double, var last_photo : String? = null, var isStarted: Boolean, var userId : String? = null, var isInGeoFence : Boolean) {
     override fun toString(): String {
         return "[Scooter] $name is placed at $location at $timestamp."
     }
 }


