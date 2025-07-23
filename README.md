<div align="center">
<img width="360" alt="Soul" src="home.jpg" />
</div>

# Soul — Encrypted Year-Lock Diary

Everything you need to build and run Soul locally.

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Let Android Studio sort out any import incompatibilities
4. Create `.env` in the project root, add `GEMINI_API_KEY=your_key_here` (see `.env.example`)
5. Remove this line from `app/build.gradle.kts`: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run on emulator or device
