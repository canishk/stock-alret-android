# Stock Price Alert — Debug APK

Install the Android app on your phone **without Android Studio**. This branch contains the latest debug APK for sideload testing.

## Download APK

**Direct file:** [releases/stock-price-alert-debug.apk](releases/stock-price-alert-debug.apk)

Or from GitHub Actions:

1. Open [Actions](https://github.com/canishk/stock-alret-android/actions)
2. Select the latest **Build Debug APK** run
3. Download the **stock-price-alert-debug** artifact
4. Extract `app-debug.apk` from the zip

## Install on Android phone

1. Download the APK to your phone (browser, email, or USB transfer).
2. Open **Settings → Security** (or **Apps → Special access**).
3. Allow your browser or **Files** app to **install unknown apps**.
4. Tap the downloaded APK file.
5. Tap **Install**.
6. Open **Stock Price Alert** and allow **Notifications** when prompted (required on Android 13+).

## How to use the app

### Add a stock watcher

1. Tap the **+** button.
2. Enter **stock name** (e.g. `RELIANCE`, `Tata Steel`).
3. Enter **target price** in rupees.
4. Choose alert type:
   - **HIGH** — notify when NSE price reaches or goes above target
   - **LOW** — notify when NSE price reaches or goes below target
5. Tap **Save Watcher**.

### Edit or delete

- Tap a watcher in the list to edit it.
- Tap the **delete** icon to remove it.

### Alerts

When the price crosses your target during market hours, you get a **banner notification with vibration** showing:

- NSE price
- BSE price
- Your target price

### Background checks

The app checks prices **every 30 minutes**, only on:

- **Monday to Friday**
- **11:00 AM to 3:00 PM IST**

Outside these hours, checks are skipped. No alerts are sent when the exchange API is unavailable.

## Debug on your phone

### Quick checks (no computer)

1. **Confirm watchers exist** — open app → list should show saved stocks with target and HIGH/LOW.
2. **Check notification permission** — Settings → Apps → Stock Price Alert → Notifications → **Allowed**.
3. **Disable battery restrictions** — Settings → Apps → Stock Price Alert → Battery → **Unrestricted** (wording varies by phone).
4. **Verify market window** — background checks run only **Mon–Fri, 11:00 AM–3:00 PM IST**. Outside that window, no checks run.
5. **Test alert threshold** — add a watcher with an easy target (e.g. `RELIANCE` + very high LOW price or very low HIGH price) and wait for the next 30-minute check during market hours.
6. **Notification history** — pull down shade → long-press a notification (or Settings → Notifications → Notification history) to see if alerts were blocked.

### View logs with USB (no Android Studio)

You only need **Android Platform Tools** (~10 MB):  
https://developer.android.com/tools/releases/platform-tools

**On phone**

1. Settings → About phone → tap **Build number** 7 times → Developer options enabled.
2. Settings → Developer options → **USB debugging** ON.
3. Connect phone to PC via USB → allow debugging prompt on phone.

**On PC (Windows PowerShell)**

```powershell
# confirm device connected
adb devices

# install or reinstall APK
adb install -r stock-price-alert-debug.apk

# watch app background worker logs (Ctrl+C to stop)
adb logcat -s StockPriceCheck

# filter all app logs by package
adb logcat --pid=$(adb shell pidof -s com.stockpricealert)
```

**Log messages to expect**

| Log message | Meaning |
|-------------|---------|
| `Outside trading window, skipping` | Current time is outside Mon–Fri 11:00–15:00 IST |
| `No active watchers` | No saved watchers in database |
| `Market/API unavailable for RELIANCE, skipping` | API call failed or price missing — exchange may be closed |
| (no logs for ~30 min inside window) | WorkManager may be delayed by battery saver |

**Check scheduled background work**

```powershell
adb shell dumpsys jobscheduler | findstr stockpricealert
```

**Check notification channel**

```powershell
adb shell dumpsys notification | findstr stock_price_alerts
```

### Wireless debugging (Android 11+, no USB cable)

1. Phone: Developer options → **Wireless debugging** ON.
2. Tap **Pair device with pairing code**.
3. On PC:

```powershell
adb pair <phone-ip>:<pairing-port>
adb connect <phone-ip>:<debug-port>
adb logcat -s StockPriceCheck
```

### On-phone log apps (optional, no PC)

Install a log viewer from Play Store (e.g. **MatLog**, **Logcat Reader**) — requires **USB debugging** or **Developer options** on some devices. Filter by tag: `StockPriceCheck`.

> Note: logcat access on newer Android versions may be restricted without adb.

### Common debug scenarios

| Goal | What to do |
|------|------------|
| App won't install | `adb install -r` and check for signature conflict; uninstall old version first |
| No background checks | Battery → Unrestricted; confirm Mon–Fri 11:00–15:00 IST |
| API not working | `adb logcat -s StockPriceCheck` during market hours; re-download latest APK |
| Notification silent | Settings → Notifications → allow; check Do Not Disturb |
| Force fresh install | `adb uninstall com.stockpricealert` then install APK again |

## Troubleshooting

| Problem | What to try |
|---------|-------------|
| Install blocked | Enable “Install unknown apps” for your browser or file manager |
| No notifications | Grant notification permission in Android Settings → Apps → Stock Price Alert |
| No price alerts | Ensure current time is Mon–Fri, 11:00–15:00 IST |
| Alerts delayed | Android battery saver may delay background checks; disable optimization for this app |
| API errors | Re-download the latest APK from this branch (CI rebuild may be needed) |

## Notes

- This is a **debug APK** for testing only, not for Play Store release.
- Source code lives on branch `cursor/stock-price-alert-android`.
- A new APK is published here automatically when the CI workflow builds successfully.
