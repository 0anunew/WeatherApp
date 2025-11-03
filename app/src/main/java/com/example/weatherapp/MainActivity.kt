package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

class MainActivity : AppCompatActivity() {
    private val requestLocationCode = 123123
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocation()
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
                override fun onResponse(
                    call: retrofit2.Call<WeatherJSON?>,
                    response: Response<WeatherJSON?>
                ) {
                    if(response.isSuccessful){
                        val weatherList: WeatherJSON? = response.body()
                        Toast.makeText(this@MainActivity, "$weatherList", Toast.LENGTH_SHORT).show()
                        Log.i("Coordinate","${weatherList?.coord}")
                        Log.i("Weather ", "${weatherList?.weather}")
                        Log.i("Base ", "${weatherList?.base}")
                        Log.i("Main ", "${weatherList?.main}")
                        Log.i("Wind ", "${weatherList?.wind}")
                        Log.i("Clouds ", "${weatherList?.clouds}")
                        Log.i("Dt ", "${weatherList?.dt}")
                        Log.i("Sys ", "${weatherList?.sys}")
                        Log.i("Timezone ", "${weatherList?.timezone}")
                        Log.i("Id ", "${weatherList?.id}")
                        Log.i("Name ", "${weatherList?.name}")
                        Log.i("Cod ", "${weatherList?.cod}")
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
        }
    }
}
