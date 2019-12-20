package app.srideassignment.model

data class WeatherInfo(

    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "",
    val currently: Currently
)

data class Currently(

    val summary: String = "",
    val temperature: Double = 0.0,
    val humidity: Double = 0.0,
    val windSpeed: Double = 0.0
)