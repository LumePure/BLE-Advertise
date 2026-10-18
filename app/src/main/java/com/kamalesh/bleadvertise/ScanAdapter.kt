package com.kamalesh.bleadvertise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

/** 扫描结果的展示数据。 */
data class ScanUiItem(
    val name: String,
    val address: String,
    val rssi: Int,
    val protocolLabel: String?,
    val spoofable: Boolean,
    val iconRes: Int
)

class ScanAdapter(
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<ScanAdapter.VH>() {

    private val items = mutableListOf<ScanUiItem>()

    fun submit(list: List<ScanUiItem>) {
        items.clear()
        items += list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_result, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position)

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val card: MaterialCardView = view as MaterialCardView
        private val icon: ImageView = view.findViewById(R.id.ivIcon)
        private val name: TextView = view.findViewById(R.id.tvName)
        private val address: TextView = view.findViewById(R.id.tvAddress)
        private val protocol: TextView = view.findViewById(R.id.tvProtocol)
        private val rssi: TextView = view.findViewById(R.id.tvRssi)

        fun bind(item: ScanUiItem, position: Int) {
            val ctx = itemView.context
            icon.setImageResource(item.iconRes)
            name.text = item.name
            address.text = item.address
            rssi.text = ctx.getString(R.string.scan_signal_strength, item.rssi)

            if (item.protocolLabel != null) {
                protocol.visibility = View.VISIBLE
                protocol.text = if (item.spoofable) {
                    ctx.getString(R.string.scan_protocol_spoofable, item.protocolLabel)
                } else {
                    item.protocolLabel
                }
            } else {
                protocol.visibility = View.GONE
            }
            card.setOnClickListener { onClick(position) }
        }
    }
}
