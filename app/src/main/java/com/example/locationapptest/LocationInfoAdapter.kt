package com.example.locationapptest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.locationapptest.databinding.LocationInfoItemBinding
import kotlin.math.round

class LocationInfoAdapter : RecyclerView.Adapter<LocationInfoAdapter.ViewHolder>() {
    var locationInfoList: ArrayList<LocationInfo> = arrayListOf()

    inner class ViewHolder(private val binding: LocationInfoItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val sn: TextView = binding.sn
        val longitude: TextView = binding.longitude
        val latitude: TextView = binding.latitude
        val tm: TextView = binding.tm
        fun bind(position: Int, locationInfo: LocationInfo) {
            sn.text = position.toString()
            longitude.text = (round(locationInfo.longitude * 100000) / 100000).toString()
            latitude.text = (round(locationInfo.latitude * 100000) / 100000).toString()
            tm.text = getTimeStringWithStamp(locationInfo.timestamp)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LocationInfoItemBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val locationInfo = locationInfoList[position]
        holder.bind(position, locationInfo)
    }

    override fun getItemCount(): Int {
        return locationInfoList.size
    }
}