package com.example.locationapptest

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    val permissionState: MutableLiveData<Boolean> = MutableLiveData(false)

    fun onLocationPermissionGrant() {
        permissionState.postValue(true)
    }

    fun onLocationPermissionReject() {
        permissionState.postValue(false)
    }
}