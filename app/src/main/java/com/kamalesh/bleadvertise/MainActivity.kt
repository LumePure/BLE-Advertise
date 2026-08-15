package com.kamalesh.bleadvertise

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var btnToggle: MaterialButton
    private lateinit var tvSelected: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnScan: MaterialButton

    private var selectedProfile: DeviceProfile? = null
    private var advertising = false
    private val profileButtons = mutableMapOf<String, MaterialButton>()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            updateUi()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.container)
        btnToggle = findViewById(R.id.btnToggle)
        tvSelected = findViewById(R.id.tvSelected)
        tvStatus = findViewById(R.id.tvStatus)
        btnScan = findViewById(R.id.btnScan)

        buildDeviceList()
        requestPermissionsIfNeeded()

        btnToggle.setOnClickListener { toggleAdvertising() }
        btnScan.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        advertising = AdvertisementForegroundService.isRunning
        AdvertisementForegroundService.currentDeviceId?.let { id ->
            DeviceProfiles.getById(id)?.let { selectProfile(it) }
        }
        updateUi()
    }

    private fun requiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun requestPermissionsIfNeeded() {
        val missing = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun buildDeviceList() {
        var lastCategory: String? = null
        DeviceProfiles.profiles.forEach { profile ->
            if (profile.category != lastCategory) {
                lastCategory = profile.category
                container.addView(makeHeader(profile.category))
            }
            container.addView(makeDeviceButton(profile))
        }
    }

    private fun makeHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            setTextSize(14f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF757575.toInt())
            setPadding(dp(0), dp(16), dp(0), dp(4))
        }
    }

    private fun makeDeviceButton(profile: DeviceProfile): MaterialButton {
        val button = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        button.apply {
            text = "${profile.title} (${profile.subtitle})"
            isAllCaps = false
            isCheckable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
            setOnClickListener { selectProfile(profile) }
        }
        profileButtons[profile.id] = button
        return button
    }

    private fun selectProfile(profile: DeviceProfile) {
        selectedProfile = profile
        profileButtons.forEach { (id, button) -> button.isChecked = id == profile.id }
        updateUi()
    }

    private fun toggleAdvertising() {
        if (advertising) {
            AdvertisementForegroundService.stop(this)
            advertising = false
        } else {
            val profile = selectedProfile
            if (profile == null) {
                Toast.makeText(this, R.string.pick_device_first, Toast.LENGTH_SHORT).show()
                return
            }
            if (!isBluetoothEnabled()) {
                try {
                    startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } catch (e: Exception) {
                    Toast.makeText(this, R.string.bluetooth_permission_required, Toast.LENGTH_LONG).show()
                }
                Toast.makeText(this, R.string.enable_bluetooth_hint, Toast.LENGTH_LONG).show()
                return
            }
            AdvertisementForegroundService.start(this, profile.id)
            advertising = true
        }
        updateUi()
    }

    private fun isBluetoothEnabled(): Boolean {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter?.isEnabled == true
    }

    private fun updateUi() {
        val profile = selectedProfile
        tvSelected.text = profile?.let { "${it.title} (${it.subtitle})" } ?: getString(R.string.none_selected)
        btnToggle.text = if (advertising) getString(R.string.stop_advertising) else getString(R.string.start_advertising)
        tvStatus.text = if (advertising) getString(R.string.advertising_in_progress) else getString(R.string.not_advertising)
        tvStatus.setTextColor(if (advertising) 0xFF1B5E20.toInt() else 0xFF757575.toInt())
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
