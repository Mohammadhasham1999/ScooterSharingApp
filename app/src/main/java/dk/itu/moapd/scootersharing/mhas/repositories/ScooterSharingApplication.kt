package dk.itu.moapd.scootersharing.mhas.repositories

import android.app.Application

class ScooterSharingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ScooterRepository.initialize(this)
        ScooterRepository.get().prepopulate()
    }

}