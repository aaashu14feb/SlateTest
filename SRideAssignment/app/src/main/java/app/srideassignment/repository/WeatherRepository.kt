package app.srideassignment.repository

import android.location.Location
import androidx.lifecycle.MutableLiveData
import app.srideassignment.api.WeatherInfoService
import app.srideassignment.exception.AppException
import app.srideassignment.model.Resource
import app.srideassignment.model.WeatherInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WeatherRepository(val apiService: WeatherInfoService) {

    fun fetchWeather(location: Location, date: String): MutableLiveData<Resource<WeatherInfo>> {

        val data = MutableLiveData<Resource<WeatherInfo>>();
        apiService.fetchWeatherInfo(location.latitude, location.longitude, date).enqueue(object : Callback<WeatherInfo> {

            override fun onResponse(call: Call<WeatherInfo>?, response: Response<WeatherInfo>?) {
                data.value = Resource.success(response?.body());
            }

            override fun onFailure(call: Call<WeatherInfo>?, t: Throwable?) {
                val exception = AppException(t)
                data.value = Resource.error(exception)
            }
        })
        return data
    }
}
