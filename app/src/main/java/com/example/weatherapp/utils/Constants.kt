package com.example.weatherapp.utils

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import androidx.annotation.RequiresPermission

object Constants {

    const val APP_ID: String ="9e8f0c6401a644c05fa120f76d342049"
    const val BASE_URL = "https://api.openweathermap.org/data/"
    const val METRIC_UNIT = "metric"

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun isNetworkAvailable(context: Context): Boolean{
        val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetwork?:return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(networkInfo)?:return false
        return when{
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)->true
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)->true
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)->true
            else->false
        }
    }
}