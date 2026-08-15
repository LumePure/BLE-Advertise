package com.kamalesh.bleadvertise

import android.os.ParcelUuid

data class AdvertisePayload(
    val title: String,
    val description: String,
    val serviceUuid: ParcelUuid? = null,
    val serviceData: ByteArray? = null,
    val manufacturerId: Int? = null,
    val manufacturerData: ByteArray? = null,
    val scanResponseManufacturerData: ByteArray? = null,
    val includeTxPower: Boolean = false,
    val scanable: Boolean = false
)
