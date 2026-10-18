package com.kamalesh.bleadvertise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

/** 设备列表条目：可能是分组标题，也可能是一个设备。 */
sealed class DeviceListItem {
    data class Header(val protocol: Protocol, val count: Int) : DeviceListItem()
    data class Device(val profile: DeviceProfile) : DeviceListItem()
}

class DeviceAdapter(
    private val selectedId: () -> String?,
    private val onClick: (DeviceProfile) -> Unit
) : ListAdapter<DeviceListItem, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_DEVICE = 1
        private const val PAYLOAD_SELECT = "select"

        private val DIFF = object : DiffUtil.ItemCallback<DeviceListItem>() {
            override fun areItemsTheSame(oldItem: DeviceListItem, newItem: DeviceListItem): Boolean =
                when {
                    oldItem is DeviceListItem.Header && newItem is DeviceListItem.Header ->
                        oldItem.protocol == newItem.protocol
                    oldItem is DeviceListItem.Device && newItem is DeviceListItem.Device ->
                        oldItem.profile.id == newItem.profile.id
                    else -> false
                }

            override fun areContentsTheSame(oldItem: DeviceListItem, newItem: DeviceListItem): Boolean =
                oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is DeviceListItem.Header) TYPE_HEADER else TYPE_DEVICE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inflater.inflate(R.layout.item_section_header, parent, false))
        } else {
            DeviceVH(inflater.inflate(R.layout.item_device, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DeviceListItem.Header -> (holder as HeaderVH).bind(item)
            is DeviceListItem.Device -> (holder as DeviceVH).bind(item.profile)
        }
    }

    /** 当前选中项变化时刷新勾选状态，无需整表重绘。 */
    fun refreshSelection() {
        for (i in 0 until itemCount) {
            if (getItem(i) is DeviceListItem.Device) notifyItemChanged(i, PAYLOAD_SELECT)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_SELECT) && holder is DeviceVH) {
            holder.bindSelection(getItem(position).let { (it as DeviceListItem.Device).profile })
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.tvHeaderTitle)
        private val count: TextView = view.findViewById(R.id.tvHeaderCount)
        fun bind(item: DeviceListItem.Header) {
            title.setText(ProtocolUi.label(item.protocol))
            count.text = itemView.context.getString(R.string.device_count, item.count)
        }
    }

    inner class DeviceVH(view: View) : RecyclerView.ViewHolder(view) {
        private val card: MaterialCardView = view.findViewById(R.id.deviceCard)
        private val icon: ImageView = view.findViewById(R.id.ivDeviceIcon)
        private val title: TextView = view.findViewById(R.id.tvTitle)
        private val subtitle: TextView = view.findViewById(R.id.tvSubtitle)
        private val check: ImageView = view.findViewById(R.id.ivCheck)

        fun bind(profile: DeviceProfile) {
            val ctx = itemView.context
            icon.setImageResource(ProtocolUi.iconFor(profile))
            title.text = profile.title
            val modelId = profile.payload.serviceData?.let { HexUtils.toHexString(it) }
            subtitle.text = if (modelId != null) {
                ctx.getString(R.string.device_subtitle_model, profile.subtitle, modelId)
            } else {
                ctx.getString(R.string.device_subtitle_protocol,
                    profile.subtitle, ctx.getString(ProtocolUi.shortLabel(profile.protocol)))
            }
            card.isChecked = profile.id == selectedId()
            card.setOnClickListener { onClick(profile) }
            bindSelection(profile)
        }

        fun bindSelection(profile: DeviceProfile) {
            val selected = profile.id == selectedId()
            check.visibility = if (selected) View.VISIBLE else View.INVISIBLE
            card.isChecked = selected
        }
    }
}
