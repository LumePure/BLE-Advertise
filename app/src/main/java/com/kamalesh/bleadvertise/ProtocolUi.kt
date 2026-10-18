package com.kamalesh.bleadvertise

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** 协议的界面呈现：名称、图标与配色语义。 */
object ProtocolUi {

    @StringRes
    fun label(protocol: Protocol): Int = when (protocol) {
        Protocol.FAST_PAIR -> R.string.protocol_fast_pair
        Protocol.APPLE -> R.string.protocol_apple
        Protocol.SAMSUNG -> R.string.protocol_samsung
        Protocol.MICROSOFT -> R.string.protocol_swift
    }

    @StringRes
    fun shortLabel(protocol: Protocol): Int = when (protocol) {
        Protocol.FAST_PAIR -> R.string.protocol_short_fast_pair
        Protocol.APPLE -> R.string.protocol_short_apple
        Protocol.SAMSUNG -> R.string.protocol_short_samsung
        Protocol.MICROSOFT -> R.string.protocol_short_swift
    }

    @DrawableRes
    fun icon(protocol: Protocol): Int = when (protocol) {
        Protocol.FAST_PAIR -> R.drawable.ic_headphones
        Protocol.APPLE -> R.drawable.ic_headphones
        Protocol.SAMSUNG -> R.drawable.ic_watch
        Protocol.MICROSOFT -> R.drawable.ic_computer
    }

    /** 设备类型的展示图标：手表优先，其余按耳机 / 电脑处理。 */
    @DrawableRes
    fun iconFor(profile: DeviceProfile): Int = when {
        profile.title.contains("Watch", ignoreCase = true) -> R.drawable.ic_watch
        profile.protocol == Protocol.MICROSOFT &&
            !profile.title.contains("Headphones", ignoreCase = true) &&
            !profile.title.contains("Earbuds", ignoreCase = true) -> R.drawable.ic_computer
        else -> R.drawable.ic_headphones
    }
}
