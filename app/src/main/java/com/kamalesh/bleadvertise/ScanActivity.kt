package com.kamalesh.bleadvertise

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.util.UUID

class ScanActivity : AppCompatActivity() {

    private lateinit var rvResults: RecyclerView
    private lateinit var btnScan: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvStatus: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var emptyState: View

    private lateinit var adapter: ScanAdapter

    private var scanning = false
    private val devices = LinkedHashMap<String, ScanResult>()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) {
                startScan()
            } else {
                Toast.makeText(this, R.string.scan_permission_required, Toast.LENGTH_LONG).show()
            }
        }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val scanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            runOnUiThread {
                devices[result.device.address] = result
                render()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                scanning = false
                updateUi()
                Toast.makeText(this@ScanActivity,
                    getString(R.string.scan_failed, errorCode), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        rvResults = findViewById(R.id.rvResults)
        btnScan = findViewById(R.id.btnScan)
        btnBack = findViewById(R.id.btnBack)
        tvStatus = findViewById(R.id.tvStatus)
        tvEmpty = findViewById(R.id.tvEmpty)
        emptyState = findViewById(R.id.emptyState)

        adapter = ScanAdapter { position ->
            sortedResults().getOrNull(position)?.let { onDeviceTapped(it) }
        }
        rvResults.layoutManager = LinearLayoutManager(this)
        rvResults.adapter = adapter

        btnScan.setOnClickListener {
            if (scanning) stopScan() else requestScanPermissions()
        }
        btnBack.setOnClickListener { finish() }

        updateUi()
    }

    override fun onPause() {
        super.onPause()
        if (scanning) stopScan()
    }

    private fun requestScanPermissions() {
        if (!isBluetoothEnabled()) {
            try {
                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } catch (e: Exception) {
                Toast.makeText(this, R.string.bluetooth_permission_required, Toast.LENGTH_LONG).show()
            }
            Toast.makeText(this, R.string.enable_bluetooth_hint, Toast.LENGTH_LONG).show()
            return
        }
        val missing = scanPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            startScan()
        }
    }

    private fun scanPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun isBluetoothEnabled(): Boolean {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter?.isEnabled == true
    }

    private fun startScan() {
        val scanner = scanner
        if (scanner == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show()
            return
        }
        devices.clear()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanning = true
        try {
            scanner.startScan(null, settings, scanCallback)
        } catch (e: SecurityException) {
            scanning = false
            Toast.makeText(this, R.string.scan_permission_required, Toast.LENGTH_LONG).show()
        }
        render()
    }

    private fun stopScan() {
        try {
            scanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            // 忽略：权限缺失时无法停止，也不影响界面
        }
        scanning = false
        updateUi()
    }

    private fun sortedResults(): List<ScanResult> =
        devices.values.sortedByDescending { it.rssi }

    private fun updateUi() {
        btnScan.setText(if (scanning) R.string.stop_scan else R.string.start_scan)
        btnScan.setIconResource(if (scanning) R.drawable.ic_stop else R.drawable.ic_scan)
        tvStatus.text = when {
            scanning -> getString(R.string.scanning)
            devices.isEmpty() -> getString(R.string.no_devices_found)
            else -> getString(R.string.devices_found, devices.size)
        }
        val showEmpty = devices.isEmpty()
        emptyState.visibility = if (showEmpty) View.VISIBLE else View.GONE
        rvResults.visibility = if (showEmpty) View.GONE else View.VISIBLE
        tvEmpty.setText(if (scanning) R.string.scanning else R.string.scan_empty_hint)
    }

    private fun render() {
        val list = sortedResults().map { result ->
            val spoof = findSpoofProfile(result)
            ScanUiItem(
                name = result.device.name ?: getString(R.string.unknown_device),
                address = result.device.address,
                rssi = result.rssi,
                protocolLabel = protocolFor(result),
                spoofable = spoof != null,
                iconRes = spoof?.let { ProtocolUi.iconFor(it) } ?: R.drawable.ic_bluetooth
            )
        }
        adapter.submit(list)
        updateUi()
    }

    private fun onDeviceTapped(result: ScanResult) {
        val name = result.device.name ?: getString(R.string.unknown_device)
        val protocol = protocolFor(result)
        val spoofProfile = findSpoofProfile(result)

        val builder = AlertDialog.Builder(this)
            .setTitle(name)
            .setMessage(buildMessage(result, protocol))
        if (spoofProfile != null) {
            builder.setPositiveButton(getString(R.string.advertise_action, spoofProfile.title)) { _, _ ->
                AdvertisementForegroundService.start(this, spoofProfile.id)
                Toast.makeText(this, R.string.advertising_started, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    private fun buildMessage(result: ScanResult, protocol: String?): String {
        val sb = StringBuilder()
        sb.append(getString(R.string.message_address, result.device.address))
        sb.append("\n").append(getString(R.string.message_rssi, result.rssi))
        protocol?.let { sb.append("\n").append(getString(R.string.message_protocol, it)) }
        val modelId = fastPairModelId(result)
        if (modelId != null) {
            sb.append("\n").append(getString(R.string.message_model_id, modelId))
        }
        return sb.toString()
    }

    private fun protocolFor(result: ScanResult): String? {
        val record = result.scanRecord ?: return null
        if (record.serviceUuids?.any { it.uuid == FAST_PAIR_UUID.uuid } == true ||
            record.serviceData?.keys?.any { it.uuid == FAST_PAIR_UUID.uuid } == true
        ) {
            return getString(R.string.protocol_short_fast_pair)
        }
        val data = record.manufacturerSpecificData ?: return null
        return when {
            data.indexOfKey(0x004C) >= 0 -> getString(R.string.protocol_short_apple)
            data.indexOfKey(0x0075) >= 0 -> getString(R.string.protocol_short_samsung)
            data.indexOfKey(0x0006) >= 0 -> getString(R.string.protocol_short_swift)
            else -> null
        }
    }

    private fun fastPairModelId(result: ScanResult): String? {
        val record = result.scanRecord ?: return null
        record.getServiceData()?.forEach { (uuid, bytes) ->
            if (uuid.uuid == FAST_PAIR_UUID.uuid && bytes.size >= 3) {
                return HexUtils.toHexString(bytes.copyOfRange(0, 3))
            }
        }
        return null
    }

    private fun findSpoofProfile(result: ScanResult): DeviceProfile? {
        val record = result.scanRecord ?: return null
        fastPairModelId(result)?.let { modelId ->
            DeviceProfiles.findByModelId(modelId)?.let { return it }
        }
        val data = record.manufacturerSpecificData ?: return null
        return when {
            data.indexOfKey(0x004C) >= 0 -> DeviceProfiles.getById("ap_airpods")
            data.indexOfKey(0x0075) >= 0 -> DeviceProfiles.getById("sw_watch5")
            data.indexOfKey(0x0006) >= 0 -> DeviceProfiles.getById("sp_xbox")
            else -> null
        }
    }

    companion object {
        private val FAST_PAIR_UUID: ParcelUuid =
            ParcelUuid(UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb"))
    }
}
