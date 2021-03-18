package com.example.locationapptest

import android.Manifest
import android.app.Instrumentation
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.locationapptest.databinding.ActivityMainBinding
import es.dmoral.toasty.Toasty

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationServiceModel: LocationServiceModel
    private lateinit var viewModel: MainViewModel
    private val tag = MainActivity::class.simpleName
    private lateinit var adapter: LocationInfoAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        adapter = LocationInfoAdapter()
        linearLayoutManager = LinearLayoutManager(this)

        locationServiceModel = (application as LocationApp).locationServiceModel
        locationServiceModel.serviceState.observe(this, {
            if (it) {
                binding.serviceState.text = "已启动"
            } else {
                binding.serviceState.text = "已停止"
            }
        })
        locationServiceModel.locationProviderState.observe(this, {
            it.keys.forEach { provider ->
                Toast.makeText(
                    this,
                    "$provider ${
                        if (it[provider] as Boolean) {
                            "enable"
                        } else {
                            "disable"
                        }
                    }",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        locationServiceModel.currentLocationInfo.observe(this, {
            if (it != null) {
                binding.locationLongitude.text = it.longitude.toString()
                binding.locationLatitude.text = it.latitude.toString()
                binding.locationTm.text = getTimeStringWithStamp(it.timestamp)
                adapter.locationInfoList.add(it)
                adapter.notifyDataSetChanged()
                linearLayoutManager.scrollToPosition(adapter.locationInfoList.size - 1)
                adapter.notifyItemChanged(adapter.locationInfoList.size - 1)
            } else {
                binding.locationLongitude.text = ""
                binding.locationLatitude.text = ""
                binding.locationTm.text = ""
            }
        })
        locationServiceModel.locationUpdateEnable.observe(this, {
            if (it) {
                binding.locationState.text = "更新中"
            } else {
                binding.locationState.text = "停止中"
            }
        })
        viewModel.permissionState.observe(this, {
            if (it) {
                binding.permissionState.text = "已获取"
            } else {
                binding.permissionState.text = "未获取"
            }
        })
        binding.requestPermission.setOnClickListener {
            checkLocationPermission()
        }
        binding.usableLocation.setOnClickListener {
            Toasty.info(
                this,
                locationServiceModel.getUsableLocation().toString(),
                Toasty.LENGTH_SHORT
            ).show()
        }

        binding.locationHistory.apply {
            layoutManager = this@MainActivity.linearLayoutManager
            adapter = this@MainActivity.adapter!!
            setHasFixedSize(true)
        }

        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission_group.LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 已经获取权限
                Log.v(tag, "permission already granted")
                viewModel.onLocationPermissionGrant()
                locationServiceModel.locationService?.onLocationPermissionGrant()
            }
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(
                Manifest.permission_group.LOCATION
            )) -> {
                Log.v(tag, "show permission rationale ui")
                TODO()
            }
            else -> {
                Log.v(tag, "request permission")
                var permissions = arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    permissions = arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                }
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    1
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "获取位置权限成功", Toast.LENGTH_SHORT).show()
                    viewModel.onLocationPermissionGrant()
                    locationServiceModel.locationService?.onLocationPermissionGrant()
                } else {
                    Toast.makeText(this, "获取位置权限失败", Toast.LENGTH_SHORT).show()
                    viewModel.onLocationPermissionReject()
                    locationServiceModel.locationService?.onLocationPermissionReject()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}