# PocketGBA Home

Android hybrid GBA home-screen setup: mGBA native core, live-wallpaper game display, and floating controller overlay.

## Current implementation

- Native build configuration fetches mGBA 0.10.5 and builds its libretro core for Android `arm64-v8a`.
- JNI bridge exposes core initialization, ROM loading, frame execution, RGB565 framebuffer conversion, and joypad input.
- A foreground emulator-session service owns the single core instance.
- The live wallpaper binds to that service and scales the latest 240×160 frame using nearest-neighbour rendering.
- The floating HUD includes D-pad, A/B, L/R, Start and Select; button transitions are forwarded to the emulator service.
- ROM access is user-provided through Android's document picker; raw `.gba` files and ZIP archives containing a `.gba` file are supported. No ROM or BIOS is included in this repository.

## Build and verification

GitHub Actions builds the debug APK with Android NDK 27.2.12479018 and CMake 3.22.1. It also checks that both native shared libraries are present in the APK. Download the `PocketGBA-Home-debug` artifact from the Actions run.

**Build status:** native integration has been committed, but this repository change has not yet been verified by a completed CI build or on-device test. Audio output, persistent save RAM, save states, lifecycle pause/resume, and full runtime validation remain outstanding.

## Usage after a successful build

1. Install the debug APK.
2. Open PocketGBA Home and choose a legally obtained `.gba` ROM.
3. Tap **Start emulator session**. ZIP archives are unpacked locally to their first `.gba` entry.
4. Grant overlay permission and start the floating controller.
5. Set PocketGBA Live Game as the home-screen live wallpaper.

The emulator service must remain active for the wallpaper to receive frames.

## Native core

mGBA is pinned to upstream tag `0.10.5`, built through its libretro frontend, and linked to the JNI bridge. See [the integration plan](docs/EMULATOR_CORE_INTEGRATION.md).

## User ROM test image

The supplied Pokémon FireRed test image was inspected earlier, but it is intentionally not committed or redistributed. Select it locally on the Android device.

- Archive member: `1636 - Pokemon Fire Red (U)(Squirrels).gba`
- Size: 16,777,216 bytes
- SHA-256: `3d0c79f1627022e18765766f6cb5ea067f6b5bf7dca115552189ad65a5c3a8ac`

## Licensing

mGBA is Copyright © 2013–2026 Jeffrey Pfau and is licensed under MPL-2.0. The full license is included in the repository and APK assets at `licenses/MGBA-MPL-2.0.txt`. See [the full Mozilla Public License 2.0](https://www.mozilla.org/MPL/2.0/) and the upstream [mGBA source repository](https://github.com/mgba-emu/mgba). The project must preserve applicable source and license notices when distributing builds.
