package app.srideassignment.api

import app.srideassignment.model.WeatherInfo
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface WeatherInfoService {

    /**
     * Retrieves Weather Info using https://darksky.net API.
     */

    @GET("{latitude},{longitude},{time}?exclude=daily,minutely,hourly,alerts,flags")
    fun fetchWeatherInfo(
        @Path("latitude") latitude: Double, @Path("longitude") longitude: Double, @Path("time") date: String
    ): Call<WeatherInfo>

    companion object Factory {

        fun create(): WeatherInfoService {
            val retrofit = Retrofit.Builder()
                //.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://api.darksky.net/forecast/e24b4319e15c193b7ac59e00855c449a/")
                .build()

            return retrofit.create(WeatherInfoService::class.java)
        }
    }


}
