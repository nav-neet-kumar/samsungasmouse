# Samsung Motion Mouse

Turns an Android phone (9+) into a Bluetooth HID mouse for Windows. Tilt moves the cursor; two quick taps on the back send a left-click. Nothing to install on the PC.

## Build and run
1. Open this folder in Android Studio (let it sync / create the Gradle wrapper; accept AGP upgrade prompts if offered).
2. Plug in the phone (USB debugging on) and press Run.
3. Allow the Bluetooth permissions when asked.

## Use
1. Tap **Make discoverable**. On Windows: Settings > Bluetooth & devices > Add device > Bluetooth > pick the phone ("Motion Mouse" / your phone name).
2. Once connected, hold the phone comfortably and tap **Calibrate**.
3. Tilt to move. Double-tap the back to click.
4. Next time: open the app, tap **Reconnect** and choose the PC (or connect from Windows).

## Tuning
- **Pointer speed** and **Tap sensitivity** sliders in the app.
- Dead zone, curve and axis direction: `MotionController.kt` (`DEAD_ZONE_DEG`, `shape()`, signs in `onSensorChanged`).

## Notes
- Needs the phone's Bluetooth "HID device" role. Some Samsung models/One UI versions do not expose it; the app shows "registerApp failed" in that case.
- Sensors pause when the app is in the background, so keep it open (the screen stays on while it runs).
- Windows may remember the phone as a keyboard/mouse; if pairing gets stuck, remove the device on both sides and pair again.
