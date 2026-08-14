# Stock Price Alert

Android app that watches Indian stock prices and sends high-priority vibrating notifications when NSE price crosses your target during a configurable trading window (default Mon–Fri, 11:00–15:00 IST).

Bull (blue) = **HIGH** alerts when price rises to or above a target. Bear (red) = **LOW** alerts when price falls to or below a target. After an alert fires, the watcher pauses until you resume or edit it.

## Requirements

- Android Studio (Ladybug or newer recommended)
- JDK 17
- Android SDK 35 (API level 35)
- [RapidAPI](https://rapidapi.com/) account with access to [Indian Stock Exchange API2](https://rapidapi.com/linuz/api/indian-stock-exchange-api2)

## Quick start

1. Clone the repository:

   ```bash
   git clone https://github.com/your-org/StockPriceAlert.git
   cd StockPriceAlert
   ```

2. Create `local.properties` from the template:

   ```bash
   cp local.properties.example local.properties
   ```

3. Edit `local.properties`:

   ```properties
   sdk.dir=C\:\\path\\to\\Android\\Sdk
   RAPIDAPI_KEY=your_rapidapi_key_here
   ```

   On macOS/Linux, use a normal path for `sdk.dir` (for example `/Users/you/Library/Android/sdk`).

4. Open the project in Android Studio and run on a device or emulator.

   Or build from the command line:

   ```bash
   ./gradlew assembleDebug
   ```

   Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Configuration

| Setting | Location | Default |
|---------|----------|---------|
| RapidAPI key | `local.properties` → `RAPIDAPI_KEY` | — (required) |
| API host | `app/build.gradle.kts` → `RAPIDAPI_HOST` | `indian-stock-exchange-api2.p.rapidapi.com` |
| Trading window | Settings screen (overflow menu) | Mon–Fri, 11:00–15:00 IST |
| Background check interval | Settings screen | Every 15 minutes |

Background checks run only during the trading window and when the device has network connectivity. Manual fetch is also gated to the trading window.

## Features

- Add, edit, and delete stock price watchers
- Bull/bear themed UI for HIGH and LOW alert types
- Pause-after-alert: watchers stop polling after triggering; resume or edit to re-arm
- Manual price fetch per watcher card (within trading window)
- Configurable trading window and background check interval (15, 30, 45, or 60 minutes)
- Background checks via WorkManager (survives app restart and device boot)
- High-priority notifications with vibration showing NSE and BSE prices
- App health panel: notification permission, battery optimization, last background check status
- Skips checks outside market hours or when the exchange API is unavailable
- Export / import watchers and settings (JSON backup for upgrades)

## Settings

Open **Settings** from the watcher list overflow menu (⋮) to configure:

- Trading window start and end time (IST)
- Weekdays only (Mon–Fri)
- Background check interval (15, 30, 45, or 60 minutes)
- **Export data** / **Import data** — JSON backup of all watchers and settings

**Test Background Check** bypasses the trading window for debugging.

## Upgrading the app

Release APKs are signed with a stable release keystore (from v1.0.2 onward). Earlier releases used ephemeral debug keys, which can cause **"App not installed as package conflicts with an existing package"** when upgrading.

### Migrate without losing watchers or settings

1. On the **currently installed app**: Settings → **Export data** → save `stock-watchers-backup.json`
2. Uninstall the old app (required when signatures differ)
3. Install the latest release APK from [GitHub Releases](https://github.com/canishk/stock-alret-android/releases)
4. Open Settings → **Import data** → select the JSON file

### Normal upgrade (same signing key)

If you installed from a recent GitHub release (v1.0.2+), install the newer APK directly — watchers and settings are preserved.

## Release signing setup (maintainers)

Before tagging `v1.0.2` or later, add these GitHub Actions secrets (Settings → Secrets → Actions):

| Secret | Description |
|--------|-------------|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded `release.keystore` file |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias (`stockpricealert`) |
| `RELEASE_KEY_PASSWORD` | Key password |

Generate the keystore once:

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias stockpricealert \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "YOUR_STORE_PASSWORD" \
  -keypass "YOUR_KEY_PASSWORD" \
  -dname "CN=Stock Price Alert, OU=Mobile, O=canishk, C=IN"
```

Back up `release.keystore` securely — losing it prevents publishing upgrades. See `local.properties.example` for optional local release signing.

## Architecture

```
Compose UI (WatcherListScreen, WatcherFormScreen, SettingsScreen)
    ↓
ViewModels (WatcherListViewModel, WatcherFormViewModel, SettingsViewModel)
    ↓
StockRepository
    ├── Room (StockWatcherDao) — local watcher storage
    ├── Retrofit (ApiClient) — RapidAPI stock quotes
    └── shouldTriggerAlert() — edge-triggered alert logic
    ↓
StockPriceCheckWorker (WorkManager, configurable interval)
    ↓
AlertNotificationManager — high-priority notifications with vibration
```

## Project structure

```
app/src/main/java/com/stockpricealert/
├── data/
│   ├── local/          Room database, entities, DAO
│   ├── backup/         JSON export/import for watchers and settings
│   ├── remote/         Retrofit API client and models
│   └── repository/     StockRepository (domain data access)
├── domain/             StockWatcher, AlertType models
├── notification/       AlertNotificationManager
├── ui/
│   ├── list/           Watcher list screen and ViewModel
│   ├── form/           Add/edit watcher screen
│   ├── settings/       Trading window and interval settings
│   ├── navigation/     NavHost routes
│   └── theme/          Material3 colors (bull blue / bear red)
├── util/               Market hours, permissions, preferences
└── worker/             WorkManager scheduler, background price checks
```

## API

Uses [Indian Stock Exchange API2 on RapidAPI](https://rapidapi.com/linuz/api/indian-stock-exchange-api2):

```
GET https://indian-stock-exchange-api2.p.rapidapi.com/stock?name={stockName}
Headers: x-rapidapi-key, x-rapidapi-host
```

Stock names are passed as typed by the user (for example `RELIANCE`, `Tata Steel`).

## CI

GitHub Actions workflow `.github/workflows/build-apk.yml` builds a debug APK on every push to `main` and on manual dispatch.

- Requires repository secret: `RAPIDAPI_KEY`
- APK is uploaded as a **workflow artifact** (30-day retention), not committed to the repository
- Download from: GitHub → Actions → latest run → Artifacts → `stock-price-alert-debug`

Tag pushes matching `v*` trigger `.github/workflows/release.yml`, which builds a signed release APK and publishes a GitHub Release. Release builds require the signing secrets listed in [Release signing setup](#release-signing-setup-maintainers).

## Security

- **Never commit `local.properties`** — it is gitignored and holds your API key
- Use `local.properties.example` as the template only
- For CI, store `RAPIDAPI_KEY` in GitHub repository secrets (Settings → Secrets → Actions)
- Rotate your RapidAPI key if it was ever exposed in a commit or public log

## License

MIT — see [LICENSE](LICENSE).
