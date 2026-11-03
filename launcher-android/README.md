# Başlatıcı

An Android home screen with one way in: type an app's name and it opens. There is no app list or drawer.

## How it works

- The keyboard comes up whenever the home screen appears.
- One line above the search bar names the app that Enter would open; nothing else is shown.
- When what you typed (two letters or more) fits exactly one app, that app opens on its own after 0.3 s. A further letter cancels it.
- Enter opens the best match. With no match it searches the web (can be turned off).
- Matching follows AOSP Launcher3 (`StringMatcherUtility`): the text must match the start of a word in the app's name (`maps` → Google Maps, `tube` → YouTube), ignoring case and accents. Turkish i, ı, I and İ are treated as the same letter, so `insta` finds Instagram on a Turkish keyboard.
- Ranking: the full name typed out, then names that start with the text, then a later word; within each, apps you open more often first.
- The keyboard is set up like Launcher3's search box (`textNoSuggestions`, `flagNoExtractUi`), so suggestions and autocorrect do not change what you type.
- Work profile apps are included while the profile is on.
- Settings: become the default home app (the HOME role on Android 10+), auto-open, web search, clock, dynamic color, theme.

Built on Material 3 Expressive components (`MaterialExpressiveTheme`, `SearchBar`, `ModalBottomSheet`, `ListItem`, connected `ToggleButton`s). There are no custom animations; motion comes from the components.

## Build

Needs JDK 25 and the Android SDK with platform 37.2.

```bash
./gradlew :app:assembleRelease     # app/build/outputs/apk/release/app-release.apk
./gradlew :app:testDebugUnitTest
```

The matching code is ported from AOSP Launcher3 (Apache License 2.0).
