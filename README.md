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

## Local test ROM

A user-supplied Pokémon FireRed US GBA image was inspected for integration testing. It is intentionally **not committed** to this public repository. Select the ROM from the app's file picker on the device once the emulator core is integrated.

- Archive member: `1636 - Pokemon Fire Red (U)(Squirrels).gba`
- Size: 16,777,216 bytes
- SHA-256: `3d0c79f1627022e18765766f6cb5ea067f6b5bf7dca115552189ad65a5c3a8ac`

No copyrighted ROMs or Nintendo BIOS files are included in this repository.
