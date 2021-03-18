package com.example.locationapptest

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.MutableLiveData

class LocationServiceModel(private val context: Context) : LocationService.LocationUpdateListener {
    private val tag: String
        get() = this.toString()

    var locationService: LocationService? = null
    var locationServiceBound: Boolean = false
    var locationServiceConnection: ServiceConnection

    init {
        locationServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
                Log.v(tag, "service connected")
                locationServiceBound = true
                val binder = iBinder as LocationService.LocationBinder
                locationService = binder.getService()
                locationService!!.locationUpdateListener = this@LocationServiceModel
                locationUpdateEnable.postValue(locationService!!.locationUpdating)
                this@LocationServiceModel.onServiceConnected()

                val lastLocationInfo = locationService!!.getLastLocation()
                if (lastLocationInfo == null) {
                    Log.v(tag, "last location info is null")
                } else {
                    Log.v(tag, "last location info is $lastLocationInfo")
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.v(tag, "service disconnected")
                unbindService()
                locationServiceBound = false
            }
        }
    }

    /** some service state info */
    val serviceState: MutableLiveData<Boolean> = MutableLiveData(false)
    val locationUpdateEnable: MutableLiveData<Boolean> = MutableLiveData(false)
    val locationProviderState: MutableLiveData<HashMap<String, Boolean>> =
        MutableLiveData(HashMap())
    val currentLocationInfo: MutableLiveData<LocationInfo?> = MutableLiveData(null)

    fun bindService() {
        if (!locationServiceBound) {
            Log.v(tag, "bind service")
            if (context.bindService(
                    Intent(context, LocationService::class.java),
                    locationServiceConnection,
                    0
                )
            ) {
                Log.v(tag, "bind success")

            } else {
                Log.v(tag, "bind failed")
            }
        } else {
            Log.v(tag, "already bind service")
        }
    }

    fun getUsableLocation(): LocationInfo? {
        return locationService?.getUsableLocation()
    }

    private fun unbindService() {
        if (locationServiceBound) {
            Log.v(tag, "unbind service")
            context.unbindService(locationServiceConnection)
        }
    }

    private fun onServiceConnected() {
        serviceState.postValue(true)
        //bindService()
    }


    override fun onServiceStart() {
        Log.v(tag, "service start")
        serviceState.postValue(true)
    }

    override fun onServiceStop() {
        Log.v(tag, "service stop")
        serviceState.postValue(false)
    }

    override fun onLocationEnabledSuccess() {
        Log.v(tag, "request location update success")
        locationUpdateEnable.postValue(true)
    }

    override fun onLocationEnabledFailed() {
        Log.v(tag, "request location update failed")
        locationUpdateEnable.postValue(false)
    }

    override fun onProviderDisable(provider: String) {
        Log.v(tag, "$provider disable")
        locationProviderState.value!![provider] = false
    }

    override fun onProviderEnabled(provider: String) {
        Log.v(tag, "$provider enabled")
        locationProviderState.value!![provider] = true
    }

    override fun onLocationUpdate(locationInfo: LocationInfo) {
        Log.v(tag, locationInfo.toString())
        currentLocationInfo.postValue(locationInfo)
    }

}