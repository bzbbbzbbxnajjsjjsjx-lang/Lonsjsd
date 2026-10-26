# Prompt: Notlar (Android, Jetpack Compose)

Build a small Android notes app named "Notlar" in Kotlin with Jetpack Compose and Material 3.

## Platform
- Package `com.example.notes`, applicationId `com.example.notes`.
- minSdk 26, targetSdk and compileSdk 37 (minor API 2).
- Kotlin and the Compose compiler plugin 2.4.21, Android Gradle Plugin 9.4.1, Gradle 9.8.2 wrapper.
- JVM toolchain 25 (`sourceCompatibility` and `targetCompatibility` VERSION_25).
- Dependencies: Compose BOM 2026.09.00, material3 (no version pin needed from the BOM), material-icons-extended, activity-compose 1.13.0, core-ktx 1.19.1, lifecycle-viewmodel-compose and lifecycle-runtime-compose 2.11.0. Debug: ui-tooling.
- Release build: minify and shrink on, `proguard-android-optimize.txt`, signed with the debug key so it installs directly.
- Launcher icon: adaptive icon, background `#6750A4`, foreground a white sheet of paper with a folded corner and three purple lines, scaled to 72% for the safe zone.
- Theme parent for the window: `android:Theme.Material.Light.NoActionBar`.

## Data
- One data class: `Note(id: String, title: String, body: String, updated: Long)`.
- Stored as a single JSON array under the key `"notes"` in SharedPreferences named `"notes"`, using `org.json`.
- Load errors fall back to an empty list.
- Notes are always sorted by `updated`, newest first.

## ViewModel (`NotesViewModel`, AndroidViewModel)
- `notes: StateFlow<List<Note>>`.
- `newId()` returns a random UUID.
- `save(id, title, body)`: when both title and body are blank, delete the note; otherwise replace it with `updated = now`.
- `delete(id)`: remove the note.
- Every change writes the whole list back to storage.

## Screens
1. **List**
   - Top app bar titled "Notlar" in bold.
   - Floating action button with a `+` icon, content description "Yeni not". It opens a new editor with a fresh id.
   - Empty state, centered: "Henüz not yok." on one line and "Yeni not için + simgesine dokunun." on the next, in onSurfaceVariant.
   - Lazy column of note cards, 16 dp padding, 12 dp spacing, each item keyed by id and using `animateItem()`.
   - Card: 24 dp rounded corners, surfaceContainerLow background, full width.
     - Title: titleMedium, SemiBold, one line, ellipsis. Shows "Başlıksız not" when blank.
     - Body preview: bodyMedium, onSurfaceVariant, up to three lines, ellipsis. Hidden when blank.
     - Relative time (`DateUtils.getRelativeTimeSpanString`, minute resolution): labelSmall, primary color, right aligned.
     - Press feedback: scales to 0.97 while pressed, springs back with a medium-bouncy spring.
2. **Editor**
   - Replaces the list with an animated transition: the editor fades in and rises from below, the list fades out and sinks.
   - Top app bar with a back arrow (AutoMirrored ArrowBack, "Geri") and a delete icon ("Notu sil").
   - Title field: headlineSmall, SemiBold, placeholder "Başlık". Body field: bodyLarge, placeholder "Notunuzu yazın", fills the rest of the screen.
   - Both fields have transparent indicators and surface container color. Edits save on every keystroke.
   - Delete removes the note and returns to the list.
   - Each note opens with its own state (`key(id)`), so one note's text never appears in another.
   - System back gesture or back arrow closes the editor. Back from the list exits the app.

## Feedback and motion
- Haptics: `HapticFeedbackType.ContextClick` when opening a note, creating a note, closing the editor and back. `HapticFeedbackType.LongPress` for delete.
- Theme: Material 3 color scheme from the wallpaper on Android 12 and later (`dynamicLightColorScheme` and `dynamicDarkColorScheme`). Otherwise `lightColorScheme()` or `darkColorScheme()` following the system dark setting.
- Edge-to-edge enabled. Editor bottom padding respects the navigation bar.

## Tests
- Not required for the first version. Add unit tests for `NoteStore` JSON round trip and for `NotesViewModel.save` deleting blank notes if you add tests.

## Acceptance
- Create a note, type a title and body, close, reopen: the text is still there.
- A note with both fields emptied disappears from the list.
- Notes survive an app restart.
- Release build installs and runs on Android 8.0 (API 26) and later.
