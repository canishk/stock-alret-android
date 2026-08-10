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
