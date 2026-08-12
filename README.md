# Stock Price Alert

Android app that watches Indian stock prices and sends vibrating banner notifications when NSE price crosses your target during a configurable trading window (default Mon–Fri, 11:00–15:00 IST).

## Setup

1. Open the project in Android Studio.
2. Ensure `local.properties` contains:
   ```
   sdk.dir=C\:\\path\\to\\Android\\Sdk
   RAPIDAPI_KEY=your_rapidapi_key
   ```
3. Build and run on a device or emulator.

## Features

- Add, edit, and delete stock price watchers
- Configurable trading window and background check interval (default every 15 minutes)
- Background checks only run inside the trading window; manual fetch is also gated
- High-priority notifications with vibration showing NSE and BSE prices
- Skips checks outside market hours or when the exchange API is unavailable

## Settings

Open **Settings** from the watcher list overflow menu (⋮) to configure:

- Trading window start and end time (IST)
- Weekdays only (Mon–Fri)
- Background check interval (15, 30, 45, or 60 minutes)

**Test Background Check** bypasses the trading window for debugging.

## API

Uses [Indian Stock Exchange API2 on RapidAPI](https://rapidapi.com/linuz/api/indian-stock-exchange-api2):

```
GET https://indian-stock-exchange-api2.p.rapidapi.com/stock?name={stockName}
```

## Notes

- WorkManager polling may drift slightly under battery optimization.
- Rotate your RapidAPI key if it was ever shared publicly.
