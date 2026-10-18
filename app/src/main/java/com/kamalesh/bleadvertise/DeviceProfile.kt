package com.kamalesh.bleadvertise

/** 应用支持的四类广播协议。 */
enum class Protocol(val key: String) {
    FAST_PAIR("fast_pair"),
    APPLE("apple"),
    SAMSUNG("samsung"),
    MICROSOFT("microsoft")
}

class DeviceProfile(
    val id: String,
    val title: String,
    val subtitle: String,
    val protocol: Protocol,
    val payload: AdvertisePayload
) {
    /** 分类标识，用于列表分组。 */
    val category: String get() = protocol.key
}
