# Emulator core integration decision

## Current state

The Android shell is a live-wallpaper surface plus a movable application-overlay controller. It can remember a user-selected ROM URI. It does not currently execute a ROM, render emulator frames, or produce game audio. The HUD currently emits package-scoped key events, but no emulator input consumer is connected.

## Core choice

Use mGBA as the target emulator core. mGBA is MPL-2.0 licensed, but upstream does not currently provide an official Android port. The upstream FAQ explicitly says Android/iOS are not supported as official ports at present. The libretro mGBA core is a viable alternative core, but it requires a frontend implementation.

A community SDL2 Android port exists and documents a working Android build based on a static mGBA library, SDL2, Android NDK, and CMake. It is useful as a technical reference, not as a drop-in library dependency: its SDL activity and touch UI are designed around a standalone emulator app, whereas this project needs a shared core feeding a wallpaper surface and a separate overlay.

## Required integration work

1. Build mGBA as a native Android library for arm64-v8a (and optionally armeabi-v7a) using the Android NDK and CMake.
2. Add a JNI wrapper with explicit lifecycle methods: create, load ROM from an app-readable file descriptor/path, advance frames, set/release buttons, serialize state, and destroy.
3. Run the emulation loop on a dedicated foreground service thread. Do not run emulation on the wallpaper main thread.
4. Publish a 240x160 framebuffer (or a correctly sized render buffer) to the wallpaper renderer. Keep the wallpaper renderer responsible only for scaling/cropping and drawing.
5. Route HUD key-down/key-up events to the same emulator session. Ensure ACTION_CANCEL, overlay teardown, and service shutdown release all held buttons.
6. Persist battery-backed save RAM in app-private storage. Add save-state support only after ordinary save RAM is reliable.
7. Add audio output and lifecycle handling after video and input are stable.
8. Test with user-selected ROMs only. Do not commit copyrighted game ROMs or BIOS images to this repository.

## Acceptance criteria

- The selected ROM is actually loaded by the core, not merely stored as a URI.
- A known-good homebrew ROM reaches its title screen and renders continuously on the wallpaper surface.
- D-pad, A/B, L/R, Start and Select produce the expected input transitions without stuck buttons.
- The emulation loop pauses when the wallpaper is not visible and resumes without losing the session.
- Save RAM survives process restart.
- Debug and release APK builds pass in CI for the declared Android ABIs.

## References

- mGBA FAQ and platform status: https://mgba.io/faq.html
- mGBA source and MPL-2.0 license: https://github.com/mgba-emu/mgba
- Libretro mGBA core documentation: https://docs.libretro.com/library/mgba/
- Community SDL2 Android port build notes: https://github.com/Gitsnup/mgba-android
