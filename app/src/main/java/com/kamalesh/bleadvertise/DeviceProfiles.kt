package com.kamalesh.bleadvertise

import android.os.ParcelUuid
import java.util.UUID

object DeviceProfiles {

    private val fastPairUuid: ParcelUuid =
        ParcelUuid(UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb"))

    const val CATEGORY_FAST_PAIR = "Google Fast Pair (Android)"
    const val CATEGORY_AIRPODS = "Apple Continuity (AirPods)"
    const val CATEGORY_SAMSUNG = "Samsung Easy Setup"
    const val CATEGORY_SWIFT_PAIR = "Microsoft Swift Pair (Windows)"

    private fun fastPair(modelIdHex: String): AdvertisePayload = AdvertisePayload(
        title = "Fast Pair device",
        description = "Google Fast Pair, model ID $modelIdHex",
        serviceUuid = fastPairUuid,
        serviceData = HexUtils.decodeHex(modelIdHex),
        includeTxPower = true
    )

    private fun airPods(deviceId: String, name: String): AdvertisePayload {
        val prefix = "07"
        val color = "00"
        val status = "55"
        val battery = ((0..9).random() shl 4) + (0..9).random()
        val case = ((0..7).random() shl 4) + (0..9).random()
        val lid = (0..255).random()
        val random16 = ByteArray(16) { (0..255).random().toByte() }

        val payloadHex = prefix + "19" + "07" + deviceId + status +
            HexUtils.toHexString(battery.toByte()) +
            HexUtils.toHexString(case.toByte()) +
            HexUtils.toHexString(lid.toByte()) +
            color + "00"

        return AdvertisePayload(
            title = name,
            description = "Apple Continuity proximity pairing",
            manufacturerId = 0x004C,
            manufacturerData = HexUtils.decodeHex(payloadHex) + random16,
            scanable = true
        )
    }

    private fun samsungWatch(modelHex: String, name: String): AdvertisePayload {
        val prepend = HexUtils.decodeHex("010002000101FF000043")
        return AdvertisePayload(
            title = name,
            description = "Samsung Easy Setup watch",
            manufacturerId = 0x0075,
            manufacturerData = prepend + HexUtils.decodeHex(modelHex)
        )
    }

    private fun samsungBuds(modelHex: String, name: String): AdvertisePayload {
        val prepend = HexUtils.decodeHex("42098102141503210109")
        val append = HexUtils.decodeHex("063C948E00000000C700")
        val payload = HexUtils.decodeHex(modelHex.substring(0, 4) + "01" + modelHex.substring(4))
        return AdvertisePayload(
            title = name,
            description = "Samsung Easy Setup buds",
            manufacturerId = 0x0075,
            manufacturerData = prepend + payload + append,
            scanResponseManufacturerData = HexUtils.decodeHex("0000000000000000000000000000"),
            scanable = true
        )
    }

    private fun swiftPair(deviceName: String): AdvertisePayload {
        return AdvertisePayload(
            title = deviceName,
            description = "Microsoft Swift Pair",
            manufacturerId = 0x0006,
            manufacturerData = HexUtils.decodeHex("030080") + deviceName.toByteArray(Charsets.US_ASCII)
        )
    }

    val profiles: List<DeviceProfile> = listOf(
        // Google Fast Pair
        DeviceProfile("fp_pixel_buds_pro", "Pixel Buds Pro", "Google", CATEGORY_FAST_PAIR, fastPair("C8E228")),
        DeviceProfile("fp_pixel_buds_a", "Pixel Buds A-Series", "Google", CATEGORY_FAST_PAIR, fastPair("8B66AB")),
        DeviceProfile("fp_sony_wf_xm4", "Sony WF-1000XM4", "Sony", CATEGORY_FAST_PAIR, fastPair("2D7A23")),
        DeviceProfile("fp_sony_wf_xm5", "Sony WF-1000XM5", "Sony", CATEGORY_FAST_PAIR, fastPair("8A8F23")),
        DeviceProfile("fp_sony_wh_xm5", "Sony WH-1000XM5", "Sony", CATEGORY_FAST_PAIR, fastPair("5C7CDC")),
        DeviceProfile("fp_beats_studio_buds", "Beats Studio Buds", "Beats", CATEGORY_FAST_PAIR, fastPair("038F16")),
        DeviceProfile("fp_oneplus_buds_z", "OnePlus Buds Z", "OnePlus", CATEGORY_FAST_PAIR, fastPair("E07634")),
        DeviceProfile("fp_jbl_tune230", "JBL TUNE 230NC TWS", "JBL", CATEGORY_FAST_PAIR, fastPair("A9394A")),
        DeviceProfile("fp_jbl_flip6", "JBL Flip 6", "JBL", CATEGORY_FAST_PAIR, fastPair("821F66")),
        DeviceProfile("fp_bose_qc_ultra", "Bose QC Ultra Earbuds", "Bose", CATEGORY_FAST_PAIR, fastPair("5BACD6")),
        DeviceProfile("fp_bose_nc700", "Bose NC 700 Headphones", "Bose", CATEGORY_FAST_PAIR, fastPair("A7E52B")),
        DeviceProfile("fp_ticwatch_pro3", "TicWatch Pro 3", "Mobvoi", CATEGORY_FAST_PAIR, fastPair("6AD226")),
        DeviceProfile("fp_fitbit_charge4", "Fitbit Charge 4", "Fitbit", CATEGORY_FAST_PAIR, fastPair("5CEE3C")),
        DeviceProfile("fp_xiaomi_buds4", "Xiaomi Redmi Buds 4 Active", "Xiaomi", CATEGORY_FAST_PAIR, fastPair("D90617")),
        DeviceProfile("fp_realme_t100", "realme Buds T100", "realme", CATEGORY_FAST_PAIR, fastPair("D8F4E8")),
        DeviceProfile("fp_jabra_elite4", "Jabra Elite 4", "Jabra", CATEGORY_FAST_PAIR, fastPair("6BA5C3")),
        DeviceProfile("fp_soundcore_lib4nc", "soundcore Liberty 4 NC", "Anker", CATEGORY_FAST_PAIR, fastPair("06D8FC")),
        DeviceProfile("fp_galaxy_s22u", "Galaxy S22 Ultra", "Samsung", CATEGORY_FAST_PAIR, fastPair("99F098")),

        // Apple AirPods (Continuity)
        DeviceProfile("ap_airpods", "AirPods", "Apple", CATEGORY_AIRPODS, airPods("0220", "AirPods")),
        DeviceProfile("ap_airpods2", "AirPods 2nd Gen", "Apple", CATEGORY_AIRPODS, airPods("0F20", "AirPods 2nd Gen")),
        DeviceProfile("ap_airpods3", "AirPods 3rd Gen", "Apple", CATEGORY_AIRPODS, airPods("1320", "AirPods 3rd Gen")),
        DeviceProfile("ap_airpods_pro", "AirPods Pro", "Apple", CATEGORY_AIRPODS, airPods("0E20", "AirPods Pro")),
        DeviceProfile("ap_airpods_pro2", "AirPods Pro 2nd Gen", "Apple", CATEGORY_AIRPODS, airPods("1420", "AirPods Pro 2nd Gen")),
        DeviceProfile("ap_airpods_max", "AirPods Max", "Apple", CATEGORY_AIRPODS, airPods("0A20", "AirPods Max")),

        // Samsung Easy Setup
        DeviceProfile("sw_watch4_classic", "Galaxy Watch4 Classic 44mm", "Samsung", CATEGORY_SAMSUNG, samsungWatch("01", "Galaxy Watch4 Classic 44mm")),
        DeviceProfile("sw_watch4", "Galaxy Watch4 44mm", "Samsung", CATEGORY_SAMSUNG, samsungWatch("04", "Galaxy Watch4 44mm")),
        DeviceProfile("sw_watch5", "Galaxy Watch5 44mm", "Samsung", CATEGORY_SAMSUNG, samsungWatch("11", "Galaxy Watch5 44mm")),
        DeviceProfile("sw_watch5_pro", "Galaxy Watch5 Pro 45mm", "Samsung", CATEGORY_SAMSUNG, samsungWatch("15", "Galaxy Watch5 Pro 45mm")),
        DeviceProfile("sw_watch6", "Galaxy Watch6 44mm", "Samsung", CATEGORY_SAMSUNG, samsungWatch("1D", "Galaxy Watch6 44mm")),
        DeviceProfile("sw_watch6_classic", "Galaxy Watch6 Classic 43mm", "Samsung", CATEGORY_SAMSUNG, samsungWatch("1E", "Galaxy Watch6 Classic 43mm")),
        DeviceProfile("sb_buds2_purple", "Galaxy Buds2 Light Purple", "Samsung", CATEGORY_SAMSUNG, samsungBuds("39EA48", "Galaxy Buds2 Light Purple")),
        DeviceProfile("sb_buds2_silver", "Galaxy Buds2 Bluish Silver", "Samsung", CATEGORY_SAMSUNG, samsungBuds("A7C62C", "Galaxy Buds2 Bluish Silver")),
        DeviceProfile("sb_buds_live", "Galaxy Buds Live", "Samsung", CATEGORY_SAMSUNG, samsungBuds("850116", "Galaxy Buds Live Black")),
        DeviceProfile("sb_buds2_white", "Galaxy Buds2 Pure White", "Samsung", CATEGORY_SAMSUNG, samsungBuds("EAAA17", "Galaxy Buds2 Pure White")),

        // Microsoft Swift Pair
        DeviceProfile("sp_xbox", "Xbox Controller", "Microsoft", CATEGORY_SWIFT_PAIR, swiftPair("Xbox Controller")),
        DeviceProfile("sp_surface_headphones", "Surface Headphones", "Microsoft", CATEGORY_SWIFT_PAIR, swiftPair("Surface Headphones")),
        DeviceProfile("sp_surface_earbuds", "Surface Earbuds", "Microsoft", CATEGORY_SWIFT_PAIR, swiftPair("Surface Earbuds"))
    )

    fun getById(id: String): DeviceProfile? = profiles.firstOrNull { it.id == id }

    fun findByModelId(modelIdHex: String): DeviceProfile? {
        val target = modelIdHex.uppercase().replace(" ", "")
        return profiles.firstOrNull {
            it.payload.serviceData?.let { data ->
                HexUtils.toHexString(data).uppercase() == target
            } == true
        }
    }
}
