package com.example.motionmouse

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * One accelerometer feeds both features:
 *  - low-passed gravity -> tilt angles -> cursor movement (relative to a calibrated neutral pose)
 *  - sudden jerk on the Z axis -> back tap; two taps in quick succession -> click
 */
class MotionController(
    private val sm: SensorManager,
    private val onMove: (Int, Int) -> Unit,
    private val onClick: () -> Unit,
) : SensorEventListener {

    var enabled = true
    var speed = 1.0f          // pointer speed multiplier
    var tapThreshold = 3.5f   // m/s^2 jump on Z between samples; lower = more sensitive

    private var gx = 0f; private var gy = 0f; private var gz = 9.8f
    private var lastZ = 0f
    private var tiltX = 0f; private var tiltY = 0f      // degrees, current
    private var neutralX = 0f; private var neutralY = 0f // degrees, calibrated
    private var lastSend = 0L
    private var lastSpike = 0L
    private var firstTap = 0L
    private var freezeUntil = 0L

    fun start() {
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_FASTEST)
    }

    fun stop() = sm.unregisterListener(this)

    /** Treat the current hold angle as "cursor stays still". */
    fun calibrate() { neutralX = tiltX; neutralY = tiltY }

    override fun onSensorChanged(e: SensorEvent) {
        val x = e.values[0]; val y = e.values[1]; val z = e.values[2]
        val t = e.timestamp / 1_000_000L // ms

        // --- back tap: sharp change along Z (screen normal) ---
        val jerk = abs(z - lastZ)
        lastZ = z
        if (jerk > tapThreshold && t - lastSpike > 60) {
            lastSpike = t
            freezeUntil = t + 300 // keep the cursor still while tapping
            if (firstTap != 0L && t - firstTap in 80..400) {
                firstTap = 0L
                onClick()
            } else {
                firstTap = t
            }
        }

        // --- tilt: low-pass the accelerometer to get gravity ---
        gx += 0.1f * (x - gx); gy += 0.1f * (y - gy); gz += 0.1f * (z - gz)
        tiltX = Math.toDegrees(atan2(gx.toDouble(), hypot(gy.toDouble(), gz.toDouble()))).toFloat()
        tiltY = Math.toDegrees(atan2(gy.toDouble(), hypot(gx.toDouble(), gz.toDouble()))).toFloat()

        if (t - lastSend < 16) return // ~60 reports/s
        lastSend = t
        if (!enabled || t < freezeUntil) return

        // right edge down -> accel X goes negative -> cursor right; top edge down -> accel Y negative -> cursor up
        val dx = shape(-(tiltX - neutralX))
        val dy = shape(tiltY - neutralY)
        if (dx != 0 || dy != 0) onMove(dx, dy)
    }

    /** Dead zone + gentle acceleration curve, clamped to the HID range. */
    private fun shape(deg: Float): Int {
        val a = abs(deg) - DEAD_ZONE_DEG
        if (a <= 0f) return 0
        val v = (a * 0.3f + a * a * 0.03f) * speed
        val n = min(127f, v).roundToInt()
        return if (deg < 0) -n else n
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private companion object {
        const val DEAD_ZONE_DEG = 3f
    }
}
