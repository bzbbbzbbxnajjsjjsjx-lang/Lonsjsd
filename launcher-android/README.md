# Başlatıcı

A Material 3 Expressive home screen (launcher) for Android, built around typing: start typing an app's name and it opens.

## How it works

- The keyboard is ready as soon as the home screen appears. Type part of a name and the matching apps show up instantly.
- When only one app matches, it opens by itself after a short pause. Enter opens the best match, or searches the web when nothing matches.
- Matching understands Turkish letters and accents (`calis` finds "Çalışma"), word starts (`maps`), initials (`gm` for Google Maps), letters in order (`wtsp` for WhatsApp), and the distinctive part of the package name. Apps you open more often rank higher.
- Typing a sum (`200*10%`, `(1+2)^2`) shows the result; tap it to copy.
- Home screen: a large clock and date (tap for alarms), the apps you use most, and your own pinned apps.
- Swipe up, or tap the grid button, for every app in alphabetical order with letter headers.
- Long-press any app to pin it to the home screen, reorder it, hide it from the drawer (it stays searchable), open its info page or uninstall it.
- Work profile apps are included.
- Settings: auto-open on a single match, keyboard on home, web search, theme, dynamic color, clock, wallpaper dimming, haptics, and hidden apps.

## Build

Needs JDK 25 and the Android SDK with platform 37.2.

```bash
./gradlew :app:assembleRelease     # app/build/outputs/apk/release/app-release.apk
./gradlew :app:testDebugUnitTest
```

After installing, make it the default home app from its settings ("Ayarla") or in Android Settings → Apps → Default apps → Home app.
