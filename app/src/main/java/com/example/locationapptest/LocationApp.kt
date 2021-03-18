package com.example.locationapptest

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import es.dmoral.toasty.Toasty

class LocationApp : Application() {
    private val tag = LocationApp::class.simpleName
    lateinit var locationServiceModel: LocationServiceModel


    override fun onCreate() {
        super.onCreate()
        Log.v(tag, "start service")
        startService(Intent(this, LocationService::class.java))
        locationServiceModel = LocationServiceModel(this)
        locationServiceModel.bindService()
        Toasty.Config.getInstance()
            .tintIcon(true)
            .setTextSize(18)
            .allowQueue(false)
            .apply()
    }
}