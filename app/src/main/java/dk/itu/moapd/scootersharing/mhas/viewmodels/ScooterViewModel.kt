package dk.itu.moapd.scootersharing.mhas.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.itu.moapd.scootersharing.mhas.models.Scooter
import dk.itu.moapd.scootersharing.mhas.repositories.ScooterRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class ScooterViewModel : ViewModel() {
    private val scooterRepository = ScooterRepository.get()

    private val _scooters : MutableStateFlow<List<Scooter>> = MutableStateFlow(emptyList())

    private val _scooter = MutableLiveData<Scooter>()

    val scooter : LiveData<Scooter>
        get() = _scooter

    val scooters : StateFlow<List<Scooter>>
        get() = _scooters.asStateFlow()


    init {
        viewModelScope.launch {
            getScooters().collect {
                _scooters.value = it
            }

        }

    }

    fun selectScooter(scooter: Scooter) {
        _scooter.value = scooter
    }

    private fun getScooters() : Flow<List<Scooter>> {
        return scooterRepository.getScooters()
    }

    suspend fun getScooter(id: UUID) : Scooter? {
        return scooterRepository.getScooter(id)
    }

    suspend fun addScooter(scooter: Scooter) {
        return scooterRepository.addScooter(scooter)
    }

    suspend fun updateScooter(scooter: Scooter) {
        return scooterRepository.updateScooter(scooter)
    }

    suspend fun deleteScooter(scooter: Scooter){
        return scooterRepository.deleteScooter(scooter)
    }
}