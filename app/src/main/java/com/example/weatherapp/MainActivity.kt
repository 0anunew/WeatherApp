package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weatherapp.utils.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    private val requestLocationCode = 123123
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private lateinit var countryTextView: TextView
    private lateinit var dateTimeTextView: TextView
    private lateinit var statusTextView: TextView

    private lateinit var temperatureTextView: TextView
    private lateinit var minTemperatureTextView: TextView
    private lateinit var maxTemperatureTextView: TextView

    private lateinit var sunriseTextView: TextView
    private lateinit var sunsetTextView: TextView
    private lateinit var windSpeedTextView: TextView

    private lateinit var humidityTextView: TextView
    private lateinit var pressureTextView: TextView

    private lateinit var moreInfoButton: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        countryTextView = findViewById(R.id.country)
        dateTimeTextView = findViewById(R.id.date_time)
        statusTextView = findViewById(R.id.status)
        temperatureTextView = findViewById(R.id.temperature)
        minTemperatureTextView = findViewById(R.id.min_temperature)
        maxTemperatureTextView = findViewById(R.id.max_temperature)
        sunriseTextView = findViewById(R.id.sunrise)
        sunsetTextView = findViewById(R.id.sunset)
        windSpeedTextView = findViewById(R.id.wind_speed)
        humidityTextView = findViewById(R.id.humidity)
        pressureTextView = findViewById(R.id.pressure)

        moreInfoButton = findViewById(R.id.more_info)
        moreInfoButton.text = getString(R.string.more_info)

        moreInfoButton.isEnabled = false
        moreInfoButton.isClickable = false

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocation()
    }

    private fun showMoreInfoDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
       // builder.setCancelable(false) // User must click a button to dismiss

//        // Set the positive button and its click listener
//        builder.setPositiveButton("Proceed") { dialog: DialogInterface, which: Int ->
//            Toast.makeText(applicationContext, "Action proceeded", Toast.LENGTH_SHORT).show()
//        }

        // Set the negative button and its click listener
        builder.setNegativeButton("Dismiss") { dialog: DialogInterface, which: Int ->
            Toast.makeText(applicationContext, "Dismiss", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // Create and show the dialog
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestLocationCode && grantResults.isNotEmpty()) {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            requestLocationData()
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        mFusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val lat: Double? = locationResult.lastLocation?.latitude
                    val long: Double? = locationResult.lastLocation?.longitude
                    Log.d("Location", "onLocationResult: $lat, $long")
                    getLocationWeatherDetails(lat!!,long!!)
                }
            }, Looper.myLooper()
        )

    }

    private fun checkLocation() {

        if (!isLocationServiceEnabled()) {
            Toast.makeText(this, R.string.location_service_not_enabled_message, Toast.LENGTH_SHORT)
                .show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            requestPermissions()
        }
    }

    private fun isLocationServiceEnabled(): Boolean {

        val locationMgr: LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        return locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showLocationRequestDialog()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            showLocationRequestDialog()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                requestLocationCode
            )
        }
    }

    private fun showLocationRequestDialog() {
        AlertDialog.Builder(this).setPositiveButton("Go to Settings") { _, _ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }.setNegativeButton("Close") { dialog, _ ->
            dialog.cancel()
        }.setTitle("Location Permission Needed")
            .setMessage("This app needs the Location permission, please accept to use location functionality")
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun convertIntToDateTime(timestampInt: Int): String {
        // 1. Convert Int to Long
        val timestampLong = timestampInt.toLong()

        // 2. Check if the timestamp is in seconds or milliseconds
        // Unix timestamps are usually in seconds (10 digits).
        // If your int is 13 digits, it's likely milliseconds.
        val instant = if (timestampLong > 10000000000L) {
            // Assume milliseconds
            Instant.ofEpochMilli(timestampLong)
        } else {
            // Assume seconds
            Instant.ofEpochSecond(timestampLong)
        }

        // 3. Convert Instant to LocalDateTime in the system's default time zone
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

        // 4. Format the LocalDateTime for display (optional)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
        return localDateTime.format(formatter)
    }

    fun convertWeatherVisibilityToString(visibilityMeters: Int): String {
        return when {
            visibilityMeters >= 10000 -> "Excellent (10km+)"
            visibilityMeters >= 4000 -> "Good (4km - 10km)"
            visibilityMeters >= 1000 -> "Moderate (1km - 4km)"
            visibilityMeters >= 500 -> "Low (500m - 1km)"
            visibilityMeters >= 100 -> "Poor (100m - 500m)"
            visibilityMeters >= 0 -> "Very Poor (< 100m)"
            else -> "Unknown"
        }
    }

    fun fahrenheitToCelsius(fahrenheit: Double): String {
        val celsius = (fahrenheit - 32) * 5.0 / 9.0 //
        // Use String.format with Locale.ROOT to ensure consistent decimal point usage
        return String.format(Locale.ROOT, "%.2f", celsius) //
    }

    private fun getLocationWeatherDetails(latitude: Double,longitude: Double){
        if(Constants.isNetworkAvailable(this)){
            val retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val weatherServiceCallAPI = retrofit.create(WeatherServiceAPI::class.java)
            val weatherListCall = weatherServiceCallAPI.getCurrentWeatherData(
                latitude,
                longitude,
                Constants.APP_ID
            )
            weatherListCall.enqueue(object : Callback<WeatherJSON>{
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: retrofit2.Call<WeatherJSON?>,
                    response: Response<WeatherJSON?>
                ) {
                    if(response.isSuccessful){
                        val weatherList: WeatherJSON? = response.body()
                        Toast.makeText(this@MainActivity, "$weatherList", Toast.LENGTH_SHORT).show()

                        countryTextView.text = weatherList?.sys?.country
                        dateTimeTextView.text = convertIntToDateTime(weatherList!!.dt)
                        statusTextView.text = weatherList.weather[0].description
                        temperatureTextView.text = fahrenheitToCelsius(weatherList.main.temp)
                        minTemperatureTextView.text = "Min Temp: " + fahrenheitToCelsius(weatherList.main.temp_min)
                        maxTemperatureTextView.text = "Max Temp: " + fahrenheitToCelsius(weatherList.main.temp_max)
                        sunriseTextView.text = "Sunrise : ${convertIntToDateTime(weatherList.sys.sunrise)}"
                        sunsetTextView.text = "Sunset: ${convertIntToDateTime(weatherList.sys.sunset)}"
                        windSpeedTextView.text = "Wind Speed: " + weatherList.wind.speed.toString()
                        humidityTextView.text = "Humidity: " + weatherList.main.humidity.toString()
                        pressureTextView.text = "Pressure: " + weatherList.main.pressure.toString()

                        moreInfoButton.isEnabled = true
                        moreInfoButton.isClickable = true

                        moreInfoButton.setOnClickListener {

                            var moreInfoString = StringBuilder()
                            moreInfoString.append("Coordinates : ${weatherList.coord.lat}, ${weatherList.coord.lon}")
                            moreInfoString.append("\n")
                            moreInfoString.append("Feels Like : ${fahrenheitToCelsius(weatherList.main.feels_like)}")
                            moreInfoString.append("\n")
                            moreInfoString.append("Sea Level : ${weatherList.main.sea_level}")
                            moreInfoString.append("\n")
                            moreInfoString.append("Ground Level: ${weatherList.main.grnd_level}")
                            moreInfoString.append("\n")
                            moreInfoString.append("Visibility: ${convertWeatherVisibilityToString(weatherList.visibility)}")


                            Log.i("More Info",moreInfoString.toString())

                            showMoreInfoDialog("More Info",moreInfoString.toString())
                        }

                    }else{
                        Toast.makeText(this@MainActivity, "Response not successful", Toast.LENGTH_SHORT).show()

                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<WeatherJSON?>,
                    t: Throwable
                ) {
                    Toast.makeText(this@MainActivity, "Error", Toast.LENGTH_SHORT).show()
                }

            })

        }else{
            Toast.makeText(this@MainActivity, "No Internet Connection", Toast.LENGTH_SHORT).show()
        }
    }
}
