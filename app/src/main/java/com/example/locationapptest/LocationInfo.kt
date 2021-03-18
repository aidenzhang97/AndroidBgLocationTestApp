package com.example.locationapptest

import android.location.Location

data class LocationInfo(
    val id: Int,
    val location: Location,
    val longitude: Double,
    val latitude: Double,
    val timestamp: Long
) {

    override fun toString(): String {
        return "id: $id, location:$location, longitude: $longitude, latitude:$latitude, time: ${
            getTimeStringWithStamp(
                timestamp
            )
        }"
    }
}