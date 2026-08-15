# BLE Advertise

An Android app that advertises **fake Bluetooth Low Energy (BLE) devices** to nearby phones so you can trigger the exact popups a real device would show: Google Fast Pair, Apple AirPods, Samsung Galaxy Watch / Buds and Microsoft Swift Pair.

This is an open, educational, security-testing companion to apps like [Bluetooth-LE-Spam](https://github.com/simondankelmann/Bluetooth-LE-Spam). Use it to test your own devices, learn how BLE advertising works, and for demos — not to harass people.

> **What it does NOT do:** a phone that sees (or even connects to) a BLE advertisement never gives the advertiser access to its microphone, speaker, storage or any personal data. Bluetooth simply does not work that way, and no app built like this one — including this one — can "extract data from a connected phone". Treat any app that claims this as a red flag.

## Features

- Spoof Google **Fast Pair** devices (Pixel Buds, Sony, Bose, JBL, Beats, OnePlus, Xiaomi, realme, Jabra, soundcore, TicWatch, Fitbit, Galaxy …)
- Spoof **Apple AirPods** (AirPods, AirPods Pro, AirPods Max …) via the Apple Continuity protocol
- Spoof **Samsung Easy Setup** Galaxy Watch 4/5/6 and Galaxy Buds
- Spoof **Microsoft Swift Pair** devices
- Advertising runs in a foreground service, so it keeps working with the screen off

## How it works

BLE advertising packets are just bytes. By advertising the 3-byte Fast Pair *model ID* of a real product (service UUID `0xFE2C`), a nearby Android phone looks the ID up in Google's database and shows a pairing notification for that product. The same trick works for Apple Continuity (`0x004C`), Samsung Easy Setup (`0x0075`) and Microsoft Swift Pair (`0x0006`) manufacturer-data packets.

## APK / builds

A **debug APK is built automatically by GitHub Actions** on every push to `main`:

1. Open the **Actions** tab of this repository
2. Open the latest **Build APK** run
3. Download the **ble-advertise-debug-apk** artifact

The debug APK is signed with the standard Android debug key and can be sideloaded directly.

### Build locally

1. Open this folder in Android Studio (it will set up the Gradle wrapper for you)
2. Run the `app` run configuration, or
3. From a terminal with Gradle 8.9+ and JDK 17: `gradle assembleDebug`

## Permissions

- `BLUETOOTH_ADVERTISE` / `BLUETOOTH_CONNECT` (Android 12+)
- Location (Android 11 and older — the platform requires it for BLE)
- Notifications (Android 13+) — for the foreground-service status

## Device model IDs / credits

Model IDs and advertisement formats come from the public [Google Fast Pair](https://developers.google.com/nearby/fast-pair) ecosystem and the open-source [Flipper Xtreme BLE Spam](https://github.com/Flipper-XFW/Xtreme-Firmware) / [Bluetooth-LE-Spam](https://github.com/simondankelmann/Bluetooth-LE-Spam) projects. This project is an independent implementation written for educational purposes.

## License

MIT — see [LICENSE](LICENSE).
