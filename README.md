An Android app (Kotlin) that converts currencies using real-time exchange rates from the AwesomeAPI.

Features


Convert between currencies using live exchange rates
Simple two-screen flow (main screen → converter screen)


Tech Stack


Kotlin
Android Views (Activities + XML layouts)
Retrofit (networking) consuming AwesomeAPI
Coroutines (async calls)


How to Run


Open the project in Android Studio.
Let Gradle sync and download dependencies.
Run on an emulator or physical device.


Project Structure

app/src/main/java/
├── com/.../MainActivity.kt
├── com/.../ConverterActivity.kt
├── data/            # Response models
└── network/         # Retrofit API service + client

Notes

Uses the free AwesomeAPI for exchange rate data — no API key required.
