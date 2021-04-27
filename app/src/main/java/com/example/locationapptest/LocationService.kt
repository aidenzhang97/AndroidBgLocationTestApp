package com.example.locationapptest

import android.Manifest
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue

/**
 * allocate a thread to get gps info from gps provider and wifi provider(location provider)
 * and save it to locationInfo queue
 * TODO change to flow
 * */
class LocationService : Service(), LocationListener {
    private val TAG: String
        get() = this.toString()

    /** location queue: save the last 20 location record */
    lateinit var locationInfoQueue: Queue<LocationInfo>

    /** binder for service */
    private val binder: LocationBinder = LocationBinder()

    /** handler for service */
    private lateinit var handlerThread: HandlerThread
    lateinit var serviceHandler: ServiceHandler

    /** location manager */
    lateinit var locationManager: LocationManager

    /** location update state*/
    var locationUpdating: Boolean = false

    /** location update listener  TODO change to Flow*/
    var locationUpdateListener: LocationUpdateListener? = null

    /** permission state */
    var locationPermissionState: Boolean = false

    interface LocationUpdateListener {
        fun onServiceStart()
        fun onServiceStop()
        fun onLocationEnabledSuccess()
        fun onLocationEnabledFailed()
        fun onProviderDisable(provider: String)
        fun onProviderEnabled(provider: String)
        fun onLocationUpdate(locationInfo: LocationInfo)
    }

    companion object {
        /** location update config
         * frequency*/
        var LOCATION_UPDATE_FREQUENCY_IN_MILLISECONDS = 3000L
        var LOCATION_UPDATE_MIN_DISTANCE_IN_METER = 0.0F
        /** others*/
    }

    class ServiceHandler(looper: android.os.Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }


    /**if there is no bind, the service will be destroy*/
    inner class LocationBinder : Binder() {
        fun getService(): LocationService {
            return this@LocationService
        }

        fun getLocationInfoQue(): Queue<LocationInfo> {
            return this@LocationService.locationInfoQueue
        }

        fun getLastLocationInfo(): LocationInfo? {
            return this@LocationService.locationInfoQueue.peek()
        }
    }

    /**before onBind or onStartCommand, if service is running,it would not be called*/
    override fun onCreate() {
        Log.v(TAG, "service create")
        super.onCreate()
        locationManager = ContextCompat.getSystemService(
            this,
            LocationManager::class.java
        ) as LocationManager
        locationInfoQueue = ArrayBlockingQueue(20)
        handlerThread =
            HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
                start()
                // Get the HandlerThread's Looper and use it for our Handler
                serviceHandler = ServiceHandler(looper)
            }
        locationUpdating = false
    }

    /**called after startService*/
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceHandler.post {
            Log.v(TAG, "service start")
            if (requestLocationUpdate()) {
                locationUpdateListener?.onLocationEnabledSuccess()
            } else {
                locationUpdateListener?.onLocationEnabledFailed()
            }
            locationUpdateListener?.onServiceStart()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**bind service to connect to the service*/
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        Log.v(TAG, "service destroy")
        locationUpdateListener?.onServiceStop()
        locationUpdating = false
        locationManager.removeUpdates(this)
        serviceHandler.removeCallbacksAndMessages(null)
        serviceHandler.looper.quitSafely()
        handlerThread.quitSafely()
        locationInfoQueue.clear()
        super.onDestroy()
    }


    /** service info helper function */
    fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(
            ACTIVITY_SERVICE
        ) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (javaClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        //super.onStatusChanged(provider, status, extras)
    }

    override fun onLocationChanged(location: Location) {
        serviceHandler.post {
            val locationInfo = LocationInfo(
                locationInfoQueue.size, location, location.longitude, location.latitude,
                getCurrentTm()
            )
            Log.v(TAG, "get updated location $location")
            if (locationInfoQueue.size >= 19) {
                val locationInfoAbandon = locationInfoQueue.poll()
                if (locationInfoAbandon != null) {
                    Log.v(TAG, "que almost full,remove head location info $locationInfoAbandon")
                }
            }
            if (locationInfoQueue.offer(locationInfo)) {
                Log.v(TAG, "offer location info to queue success")
            } else {
                Log.e(TAG, "offer location info to queue failed")
                throw IllegalStateException("can not offer to queue")
            }
            locationUpdateListener?.onLocationUpdate(locationInfo)

        }
    }

    override fun onProviderEnabled(provider: String) {
        serviceHandler.post {
            Log.v(TAG, "location provider enabled $provider")
            locationUpdateListener?.onProviderEnabled(provider)
            locationUpdating = true
            locationUpdateListener?.onLocationEnabledSuccess()
        }
    }

    override fun onProviderDisabled(provider: String) {
        serviceHandler.post {
            Log.v(TAG, "location provider disable $provider")
            locationUpdateListener?.onProviderDisable(provider)
            locationUpdating = false
            locationUpdateListener?.onLocationEnabledFailed()
        }
    }

    private fun requestLocationUpdate(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationUpdating = false
            locationPermissionState = false
            return false
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            LOCATION_UPDATE_FREQUENCY_IN_MILLISECONDS,
            LOCATION_UPDATE_MIN_DISTANCE_IN_METER,
            this
        )
        locationPermissionState = true
        locationUpdating = true
        return true
    }

    fun getLastLocation(): LocationInfo? {
        return locationInfoQueue.lastOrNull()
    }

    fun getUsableLocation(): LocationInfo? {
        var locationInfo = getLastLocation()
        if (locationInfo == null) {
            Log.v(TAG, "get location from queue is null, get last known location")
            val location = if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                null
            } else {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (location == null) {
                return null
            } else {
                locationInfo = LocationInfo(
                    0, location, location.longitude, location.latitude,
                    getCurrentTm()
                )
            }
        }
        return locationInfo
    }

    fun onLocationPermissionGrant() {
        locationPermissionState = true
        serviceHandler.post {
            if (requestLocationUpdate()) {
                locationUpdateListener?.onLocationEnabledSuccess()
            } else {
                locationUpdateListener?.onLocationEnabledFailed()
            }
        }
    }

    fun onLocationPermissionReject() {
        locationPermissionState = false
        serviceHandler.post {
            locationManager.removeUpdates(this)
        }
    }


}