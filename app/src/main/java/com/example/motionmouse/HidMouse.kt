package com.example.motionmouse

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Registers the phone as a Bluetooth HID mouse (3 buttons, relative X/Y) and sends reports.
 * Report layout (no report ID): [buttons, dx, dy]
 */
@SuppressLint("MissingPermission") // permissions are requested in MainActivity before start()
class HidMouse(
    private val ctx: Context,
    private val onStatus: (String) -> Unit,
) {
    private val ui = Handler(Looper.getMainLooper())
    private val adapter: BluetoothAdapter? =
        ctx.getSystemService(BluetoothManager::class.java)?.adapter
    private var hid: BluetoothHidDevice? = null
    private var host: BluetoothDevice? = null
    private var registered = false

    val bondedDevices: List<BluetoothDevice>
        get() = adapter?.bondedDevices?.toList().orEmpty()

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            this@HidMouse.registered = registered
            status(if (registered) "Ready. Pair from Windows or tap Reconnect." else "HID not registered")
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    host = device
                    status("Connected to ${device.name ?: device.address}")
                }
                BluetoothProfile.STATE_CONNECTING -> status("Connecting to ${device.name ?: device.address}...")
                BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.STATE_DISCONNECTING -> {
                    if (device == host) host = null
                    status("Disconnected. Tap Reconnect.")
                }
            }
        }
    }

    fun start() {
        val a = adapter
        if (a == null || !a.isEnabled) {
            status("Turn Bluetooth on, then reopen the app")
            return
        }
        val ok = a.getProfileProxy(ctx, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                val h = proxy as BluetoothHidDevice
                hid = h
                val sdp = BluetoothHidDeviceAppSdpSettings(
                    "Motion Mouse", "Phone tilt mouse", "Android",
                    BluetoothHidDevice.SUBCLASS1_MOUSE, DESCRIPTOR
                )
                if (!h.registerApp(sdp, null, null, Executors.newSingleThreadExecutor(), callback)) {
                    status("registerApp failed (this phone may not support HID device mode)")
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                hid = null
                registered = false
                status("HID service lost")
            }
        }, BluetoothProfile.HID_DEVICE)
        if (!ok) status("Bluetooth HID profile unavailable on this phone")
    }

    fun stop() {
        hid?.let {
            try { it.unregisterApp() } catch (_: Exception) {}
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
        }
        hid = null
        host = null
        registered = false
    }

    fun connect(device: BluetoothDevice) {
        if (!registered) { status("Not ready yet"); return }
        hid?.connect(device)
    }

    fun move(dx: Int, dy: Int) = send(0, dx, dy)

    fun leftClick() {
        send(1, 0, 0)
        ui.postDelayed({ send(0, 0, 0) }, 40)
    }

    private fun send(buttons: Int, dx: Int, dy: Int) {
        val d = host ?: return
        hid?.sendReport(d, 0, byteArrayOf(buttons.toByte(), dx.toByte(), dy.toByte()))
    }

    private fun status(s: String) = ui.post { onStatus(s) }

    companion object {
        // Standard boot-style mouse: 3 buttons + 5 pad bits, then signed 8-bit X and Y (relative)
        private val DESCRIPTOR = byteArrayOf(
            0x05, 0x01,             // Usage Page (Generic Desktop)
            0x09, 0x02,             // Usage (Mouse)
            0xA1.toByte(), 0x01,    // Collection (Application)
            0x09, 0x01,             //   Usage (Pointer)
            0xA1.toByte(), 0x00,    //   Collection (Physical)
            0x05, 0x09,             //     Usage Page (Buttons)
            0x19, 0x01,             //     Usage Min (1)
            0x29, 0x03,             //     Usage Max (3)
            0x15, 0x00,             //     Logical Min (0)
            0x25, 0x01,             //     Logical Max (1)
            0x95.toByte(), 0x03,    //     Report Count (3)
            0x75, 0x01,             //     Report Size (1)
            0x81.toByte(), 0x02,    //     Input (Data,Var,Abs)
            0x95.toByte(), 0x01,    //     Report Count (1)
            0x75, 0x05,             //     Report Size (5)
            0x81.toByte(), 0x03,    //     Input (Const)  padding
            0x05, 0x01,             //     Usage Page (Generic Desktop)
            0x09, 0x30,             //     Usage (X)
            0x09, 0x31,             //     Usage (Y)
            0x15, 0x81.toByte(),    //     Logical Min (-127)
            0x25, 0x7F,             //     Logical Max (127)
            0x75, 0x08,             //     Report Size (8)
            0x95.toByte(), 0x02,    //     Report Count (2)
            0x81.toByte(), 0x06,    //     Input (Data,Var,Rel)
            0xC0.toByte(),          //   End Collection
            0xC0.toByte(),          // End Collection
        )
    }
}
