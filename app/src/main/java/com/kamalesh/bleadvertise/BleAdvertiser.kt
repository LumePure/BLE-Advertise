package com.kamalesh.bleadvertise

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.util.Log

class BleAdvertiser(context: Context) {

    interface Listener {
        fun onAdvertisingStarted()
        fun onAdvertisingStopped()
        fun onAdvertisingFailed(reason: String)
    }

    var listener: Listener? = null
    var isAdvertising: Boolean = false
        private set

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private val advertiser: BluetoothLeAdvertiser?
        get() = bluetoothAdapter?.bluetoothLeAdvertiser

    private val callback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            if (status == ADVERTISE_SUCCESS) {
                isAdvertising = true
                listener?.onAdvertisingStarted()
            } else {
                isAdvertising = false
                listener?.onAdvertisingFailed("Start failed (status $status)")
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            isAdvertising = false
            listener?.onAdvertisingStopped()
        }
    }

    fun start(payload: AdvertisePayload) {
        val advertiser = advertiser
        if (advertiser == null) {
            listener?.onAdvertisingFailed("BLE advertising is not supported on this device")
            return
        }

        val dataBuilder = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(payload.includeTxPower)

        payload.serviceUuid?.let { uuid ->
            dataBuilder.addServiceUuid(uuid)
            payload.serviceData?.let { dataBuilder.addServiceData(uuid, it) }
        }

        payload.manufacturerId?.let { id ->
            dataBuilder.addManufacturerData(id, payload.manufacturerData ?: ByteArray(0))
        }

        val parameters = AdvertisingSetParameters.Builder()
            .setLegacyMode(true)
            .setConnectable(false)
            .setScanable(payload.scanable)
            .setInterval(AdvertisingSetParameters.INTERVAL_MIN)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
            .setSecondaryPhy(BluetoothDevice.PHY_LE_1M)
            .build()

        val scanResponse = if (payload.scanResponseManufacturerData != null && payload.manufacturerId != null) {
            AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addManufacturerData(payload.manufacturerId, payload.scanResponseManufacturerData)
                .build()
        } else {
            null
        }

        try {
            advertiser.startAdvertisingSet(
                parameters,
                dataBuilder.build(),
                scanResponse,
                null,
                null,
                callback,
                null
            )
        } catch (e: Exception) {
            Log.e("BleAdvertiser", "Failed to start advertising", e)
            listener?.onAdvertisingFailed(e.message ?: "Unknown error")
        }
    }

    fun stop() {
        if (isAdvertising) {
            advertiser?.stopAdvertisingSet(callback)
        }
        isAdvertising = false
        listener?.onAdvertisingStopped()
    }
}
