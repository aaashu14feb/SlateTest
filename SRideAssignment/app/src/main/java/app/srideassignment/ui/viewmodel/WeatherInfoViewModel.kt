package app.srideassignment.ui.viewmodel

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import app.srideassignment.repository.WeatherRepository

class WeatherInfoViewModel(location: Location, repository: WeatherRepository) : ViewModel() {

    private val date: MutableLiveData<String> = MutableLiveData()

    val searchResult = Transformations.switchMap(date) {
        repository.fetchWeather(location, it)
    }

    fun weatherInfo(str: String) {
        date.value = str
    }
}