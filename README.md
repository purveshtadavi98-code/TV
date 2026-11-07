# Tadavi's TV - Android TV app (WebView wrapper)

1. Download index.html from https://github.com/purveshtadavi98-code/Tadavi-TV
   (open the file -> "Download raw file") and REPLACE:
   app/src/main/assets/index.html
2. Open this folder in Android Studio. Let Gradle sync.
3. Build > Build APK(s)
   Output: app/build/outputs/apk/debug/app-debug.apk
4. Copy the APK to your TV (Downloader / Send Files to TV / adb install) and install.
5. It appears in your Apps row as "Tadavi's TV".

Optional: uncomment the HOME intent-filter in AndroidManifest.xml to use it as a home screen.
