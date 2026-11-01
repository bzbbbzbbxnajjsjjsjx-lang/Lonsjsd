# Notlar

A Material 3 Expressive notes app for Android, written in Kotlin with Jetpack Compose.

## Features

- Text notes and checklists, converted into each other without losing text
- Pin, archive, trash (emptied after 7 days) and permanent delete, with undo on every removal
- Swipe a card right to archive and left to move it to the trash
- Long-press to select several notes, then pin, color, label, archive or delete them together
- 12 note colors, labels with a label manager, label filter chips
- Reminders with quick presets or a date and time picker, as notifications that survive a reboot
- Search across titles, text, checklist items and labels; sort by edit date, creation date or title
- Grid or list layout
- Undo and redo in the editor, word and character count
- Share a note, duplicate it, or share text from another app into a new note
- Launcher shortcuts for a new note and a new list
- JSON export and import
- Settings: system, light or dark theme, dynamic color, pure black, editor text size, preview lines, haptics

Material 3 Expressive pieces in use: `MaterialExpressiveTheme` with `MotionScheme.expressive()`, `FloatingActionButtonMenu` with `ToggleFloatingActionButton`, `HorizontalFloatingToolbar`, `LargeFlexibleTopAppBar`, connected `ToggleButton` groups, `IconButton` shape morphing, `MaterialShapes` polygons and morphs, shared-bounds transition from card to editor, spring motion everywhere, and haptic feedback.

## Build

Needs JDK 25 and the Android SDK with platform 37.2.

```bash
./gradlew :app:assembleRelease     # app/build/outputs/apk/release/app-release.apk
./gradlew :app:testDebugUnitTest
```

The release APK is signed with the debug key so it installs directly. A Play Store build needs a real release key.
