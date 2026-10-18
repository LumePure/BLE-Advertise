package com.kamalesh.bleadvertise

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var statusCard: MaterialCardView
    private lateinit var pulseDot: View
    private lateinit var btnToggle: MaterialButton
    private lateinit var btnScan: MaterialButton
    private lateinit var btnAbout: ImageButton
    private lateinit var tvSelected: TextView
    private lateinit var tvSelectedMeta: TextView
    private lateinit var ivSelectedIcon: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var rvDevices: RecyclerView
    private lateinit var filterChips: ChipGroup
    private lateinit var etSearch: TextInputEditText
    private lateinit var emptyState: View

    private lateinit var adapter: DeviceAdapter

    private var selectedProfile: DeviceProfile? = null
    private var advertising = false
    private var filterProtocol: Protocol? = null
    private var query: String = ""
    private var cardAnimator: ValueAnimator? = null
    private var pulseAnimator: ObjectAnimator? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            updateUi()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusCard = findViewById(R.id.statusCard)
        pulseDot = findViewById(R.id.pulseDot)
        btnToggle = findViewById(R.id.btnToggle)
        btnScan = findViewById(R.id.btnScan)
        btnAbout = findViewById(R.id.btnAbout)
        tvSelected = findViewById(R.id.tvSelected)
        tvSelectedMeta = findViewById(R.id.tvSelectedMeta)
        ivSelectedIcon = findViewById(R.id.ivSelectedIcon)
        tvStatus = findViewById(R.id.tvStatus)
        rvDevices = findViewById(R.id.rvDevices)
        filterChips = findViewById(R.id.filterChips)
        etSearch = findViewById(R.id.etSearch)
        emptyState = findViewById(R.id.emptyState)

        adapter = DeviceAdapter(
            selectedId = { selectedProfile?.id },
            onClick = { profile -> selectProfile(profile) }
        )
        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = adapter

        buildFilterChips()
        etSearch.doAfterTextChanged {
            query = it?.toString()?.trim().orEmpty()
            refreshList()
        }

        btnToggle.setOnClickListener { toggleAdvertising() }
        btnScan.setOnClickListener { startActivity(Intent(this, ScanActivity::class.java)) }
        btnAbout.setOnClickListener { showAbout() }

        requestPermissionsIfNeeded()
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        advertising = AdvertisementForegroundService.isRunning
        AdvertisementForegroundService.currentDeviceId?.let { id ->
            DeviceProfiles.getById(id)?.let { selectedProfile = it }
        }
        refreshList()
        updateUi()
    }

    override fun onPause() {
        super.onPause()
        stopPulse()
    }

    private fun buildFilterChips() {
        DeviceProfiles.categories.forEach { protocol ->
            val chip = Chip(this).apply {
                id = View.generateViewId()
                tag = protocol
                isCheckable = true
                text = getString(ProtocolUi.shortLabel(protocol)) +
                    " · " + DeviceProfiles.countOf(protocol)
            }
            filterChips.addView(chip)
        }
        filterChips.setOnCheckedStateChangeListener { _, checked ->
            val protocol = checked.mapNotNull { id -> filterChips.findViewById<Chip>(id)?.tag }
                .filterIsInstance<Protocol>()
                .firstOrNull()
            filterProtocol = protocol
            refreshList()
        }
    }

    private fun refreshList() {
        val needle = query.lowercase()
        val items = mutableListOf<DeviceListItem>()

        DeviceProfiles.categories.forEach { protocol ->
            if (filterProtocol != null && filterProtocol != protocol) return@forEach
            val matching = DeviceProfiles.profiles.filter { it.protocol == protocol }
                .filter { matches(it, needle) }
            if (matching.isEmpty()) return@forEach
            items += DeviceListItem.Header(protocol, matching.size)
            items += matching.map { DeviceListItem.Device(it) }
        }

        adapter.submitList(items)
        emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        rvDevices.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun matches(profile: DeviceProfile, needle: String): Boolean {
        if (needle.isEmpty()) return true
        return profile.title.lowercase().contains(needle) ||
            profile.subtitle.lowercase().contains(needle) ||
            getString(ProtocolUi.label(profile.protocol)).lowercase().contains(needle) ||
            profile.payload.serviceData?.let { HexUtils.toHexString(it).lowercase().contains(needle) } == true
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

    private fun selectProfile(profile: DeviceProfile) {
        if (selectedProfile?.id == profile.id) {
            // 再次点击已选中的设备时直接切换广播状态
            toggleAdvertising()
            return
        }
        selectedProfile = profile
        adapter.refreshSelection()
        updateUi()
        if (advertising) {
            AdvertisementForegroundService.start(this, profile.id)
        }
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

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about_title)
            .setMessage(R.string.about_message)
            .setPositiveButton(R.string.got_it, null)
            .show()
    }

    private fun updateUi() {
        val profile = selectedProfile

        tvSelected.text = profile?.title ?: getString(R.string.none_selected)
        ivSelectedIcon.visibility = if (profile != null) View.VISIBLE else View.GONE
        profile?.let { ivSelectedIcon.setImageResource(ProtocolUi.iconFor(it)) }

        if (profile != null) {
            val modelId = profile.payload.serviceData?.let { HexUtils.toHexString(it) }
            tvSelectedMeta.text = if (modelId != null) {
                getString(R.string.selected_meta_fast_pair, profile.subtitle, modelId)
            } else {
                getString(R.string.selected_meta_generic,
                    profile.subtitle, getString(ProtocolUi.label(profile.protocol)))
            }
            tvSelectedMeta.visibility = View.VISIBLE
        } else {
            tvSelectedMeta.visibility = View.GONE
        }

        btnToggle.setText(if (advertising) R.string.stop_advertising else R.string.start_advertising)
        btnToggle.setIconResource(if (advertising) R.drawable.ic_stop else R.drawable.ic_play)
        tvStatus.setText(if (advertising) R.string.advertising_in_progress else R.string.not_advertising)

        animateStatus(advertising)
    }

    /** 状态卡片在「广播中」与「空闲」之间做颜色与脉冲过渡。 */
    private fun animateStatus(live: Boolean) {
        val liveBg = ContextCompat.getColor(this, R.color.status_live_bg)
        val idleBg = ContextCompat.getColor(this, R.color.brand_primary_container)
        val target = if (live) liveBg else idleBg

        cardAnimator?.cancel()
        cardAnimator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            (statusCard.cardBackgroundColor?.defaultColor ?: idleBg),
            target
        ).apply {
            duration = 320
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { statusCard.setCardBackgroundColor(it.animatedValue as Int) }
            start()
        }

        if (live) startPulse() else stopPulse()
    }

    private fun startPulse() {
        if (pulseAnimator?.isRunning == true) return
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            pulseDot,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.55f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.55f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.4f)
        ).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        pulseDot.scaleX = 1f
        pulseDot.scaleY = 1f
        pulseDot.alpha = 1f
    }
}
