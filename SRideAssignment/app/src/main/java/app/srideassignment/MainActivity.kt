package app.srideassignment

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import app.srideassignment.api.WeatherInfoService
import app.srideassignment.databinding.ActivityMainBinding
import app.srideassignment.factory.WeatherInfoFactory
import app.srideassignment.model.Resource
import app.srideassignment.model.WeatherInfo
import app.srideassignment.repository.WeatherRepository
import app.srideassignment.ui.viewmodel.WeatherInfoViewModel
import com.google.android.gms.location.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    val PERMISSION_ID = 11
    private lateinit var weatherInfoViewModel: WeatherInfoViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var currentLocation: Location
    lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        initView()

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLastLocation()
    }

    private fun initView() {

        binding.btnCalender.setOnClickListener {
            showCalender()
        }

        binding.loading = false
    }

    fun initViewModel() {

        val weatherInfoService = WeatherInfoService.create()
        weatherInfoViewModel = ViewModelProviders.of(
            this, WeatherInfoFactory(currentLocation, WeatherRepository(weatherInfoService))
        )
            .get(WeatherInfoViewModel::class.java!!)
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        currentLocation = location
                        initViewModel()
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location = locationResult.lastLocation
            currentLocation = mLastLocation
            initViewModel()
        }
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_ID
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            } else if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED)) {
                finish()
            }
        }
    }

    private fun showCalender() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)


        val dpd = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->

                if (isPrimeNumber(dayOfMonth)) {

                    val sdf = SimpleDateFormat("dd-MM-yyyy")
                    val dateString = String.format(
                        getString(R.string.date_format),
                        dayOfMonth,
                        monthOfYear + 1,
                        year
                    )
                    try {
                        val date = sdf.parse(dateString)
                        val finalDate = date.time / 1000
                        weatherInfoViewModel?.weatherInfo(finalDate.toString())
                        binding.loading = true
                        weatherInfoViewModel?.searchResult?.observe(
                            this,
                            androidx.lifecycle.Observer<Resource<WeatherInfo>> { resource ->
                                if (resource != null) {
                                    when (resource.status) {
                                        Resource.Status.SUCCESS -> {
                                            val result = resource.data as WeatherInfo
                                            binding.weatherInfo = result
                                            binding.loading = false
                                        }
                                        Resource.Status.ERROR -> {
                                            Toast.makeText(
                                                this,
                                                "Error: " + resource.exception?.message,
                                                Toast.LENGTH_LONG
                                            )
                                            binding.loading = false
                                        }
                                    }


                                }
                            })


                    } catch (e: ParseException) {
                        e.printStackTrace()
                    }


                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.msg_not_a_prime_number),
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            year,
            month,
            day
        )

        dpd.show()
    }

    private fun isPrimeNumber(num: Int): Boolean {
        var flag = true
        for (i in 2..num / 2) {
            if (num % i == 0) {
                flag = false
                break
            }
        }
        return flag;
    }
}
