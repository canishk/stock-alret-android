# Stock Price Alert

Android app that watches Indian stock prices and sends vibrating banner notifications when NSE price crosses your target during market hours (Mon–Fri, 11:00–15:00 IST).

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
- Background checks every 30 minutes during trading window
- High-priority notifications with vibration showing NSE and BSE prices
- Skips checks outside market hours or when the exchange API is unavailable

## API

Uses [Indian Stock Exchange API2 on RapidAPI](https://rapidapi.com/linuz/api/indian-stock-exchange-api2):

```
GET https://indian-stock-exchange-api2.p.rapidapi.com/stock?name={stockName}
```

## Notes

- WorkManager polling may drift slightly under battery optimization.
- Rotate your RapidAPI key if it was ever shared publicly.
