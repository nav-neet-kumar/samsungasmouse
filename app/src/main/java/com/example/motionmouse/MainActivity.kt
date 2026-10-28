package com.example.motionmouse

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var mouse: HidMouse
    private lateinit var motion: MotionController
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // sensors stop when the app is backgrounded

        mouse = HidMouse(this) { status.text = it }
        motion = MotionController(
            getSystemService(SENSOR_SERVICE) as SensorManager,
            onMove = { dx, dy -> mouse.move(dx, dy) },
            onClick = { mouse.leftClick() },
        )
        setContentView(buildUi())

        if (missingPermissions().isEmpty()) startMouse() else requestPermissions(missingPermissions(), REQ_PERMS)
    }

    override fun onResume() { super.onResume(); if (started) motion.start() }
    override fun onPause() { super.onPause(); motion.stop() }
    override fun onDestroy() { super.onDestroy(); mouse.stop() }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == REQ_PERMS && missingPermissions().isEmpty()) startMouse()
        else status.text = "Bluetooth permissions are required"
    }

    private fun startMouse() {
        started = true
        mouse.start()
        motion.start()
    }

    private fun missingPermissions(): Array<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyArray()
        return arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            .toTypedArray()
    }

    // ---- UI (built in code to keep the project to a handful of files) ----

    private fun buildUi(): View {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        fun add(v: View) = root.addView(v, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad })

        status = TextView(this).apply { text = "Starting..."; textSize = 18f; gravity = Gravity.CENTER }
        add(status)

        add(Button(this).apply {
            text = "1. Make discoverable (pair from Windows)"
            setOnClickListener {
                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                    .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300))
            }
        })
        add(Button(this).apply {
            text = "2. Reconnect to a paired PC"
            setOnClickListener { pickPairedDevice() }
        })
        add(Button(this).apply {
            text = "3. Calibrate (hold phone how you like, then tap)"
            setOnClickListener { motion.calibrate() }
        })
        add(Switch(this).apply {
            text = "Cursor movement on"
            isChecked = true
            setOnCheckedChangeListener { _, on -> motion.enabled = on }
        })

        add(TextView(this).apply { text = "Pointer speed" })
        add(SeekBar(this).apply {
            max = 40; progress = 10 // 0.5x .. 2.5x
            setOnSeekBarChangeListener(simpleSeek { motion.speed = 0.5f + it * 0.05f })
        })

        add(TextView(this).apply { text = "Tap sensitivity (right = more sensitive)" })
        add(SeekBar(this).apply {
            max = 100; progress = 75 // threshold 8.0 .. 2.0 m/s^2 -> 75 = 3.5 (default)
            setOnSeekBarChangeListener(simpleSeek { motion.tapThreshold = 8f - it * 0.06f })
        })

        add(TextView(this).apply {
            text = "Tilt the phone to move. Tap the back twice quickly to left-click. " +
                "Keep this app open while using it."
            textSize = 14f
        })
        return root
    }

    private fun pickPairedDevice() {
        @Suppress("MissingPermission")
        val devices = mouse.bondedDevices
        if (devices.isEmpty()) { status.text = "No paired devices. Pair from Windows first."; return }
        @Suppress("MissingPermission")
        val names = devices.map { it.name ?: it.address }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Connect to")
            .setItems(names) { _, i -> mouse.connect(devices[i]) }
            .show()
    }

    private fun simpleSeek(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) = onChange(p)
        override fun onStartTrackingTouch(s: SeekBar) {}
        override fun onStopTrackingTouch(s: SeekBar) {}
    }

    private companion object { const val REQ_PERMS = 1 }
}
