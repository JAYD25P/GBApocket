# PocketGBA Home

Android hybrid live-wallpaper game display + floating controller prototype.

> Status: bootstrap shell only. The wallpaper currently renders a diagnostic frame; it does not execute GBA ROMs. Native mGBA integration, framebuffer transfer, audio, save states and live input bridge remain required before this is a playable emulator.

## Build
GitHub Actions builds a debug APK on pushes to `main` and manual dispatch. Download the `PocketGBA-Home-debug` artifact from the workflow run.

## Architecture
- Android live wallpaper display service.
- Movable floating overlay controller.
- ROM document picker (user-provided ROMs only).
- Planned native mGBA/libretro JNI bridge.

No copyrighted ROMs or Nintendo BIOS files are included.