package app.srideassignment.factory

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.srideassignment.repository.WeatherRepository
import app.srideassignment.ui.viewmodel.WeatherInfoViewModel


class WeatherInfoFactory(
    private val location: Location,
    private val repository: WeatherRepository
) :
    ViewModelProvider.Factory {


    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WeatherInfoViewModel(location, repository) as T
    }
}