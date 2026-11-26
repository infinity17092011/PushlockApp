# Push-up Lock (Android)

Kotlin + Jetpack Compose. Locks the apps you pick until you do push-ups (counted on-device with ML Kit pose detection).

## Build
1. Open this folder in **Android Studio** (Koala or newer). First sync downloads Gradle and dependencies.
2. Plug in a phone (USB debugging on) and press Run, or use Build > Build APK(s) to get an APK to sideload.
3. Your Supabase URL, publishable key and server address are already set in `app/src/main/java/com/pushlock/app/Config.kt`.

## First run on the phone
- Create an account, then allow Camera, "Display over other apps" and the Accessibility service.
- Android 13+ sideloaded apps: if the Accessibility toggle is greyed out, open App info > menu (3 dots) > **Allow restricted settings**, then try again.
- Prop the phone up so your whole body is in frame, side-on works best.

## Notes
- `app-icon-512.png` is the app icon for stores or your website. The in-app icon is the adaptive vector in `res/`.
- Release builds need your own signing key (Build > Generate Signed Bundle / APK).
