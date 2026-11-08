# Native emulator integration status

## Implemented in this repository

- CMake fetches upstream mGBA tag `0.10.5` and builds the GBA libretro core for Android `arm64-v8a`.
- JNI exposes initialization, in-memory ROM loading, frame stepping, joypad input, and 240×160 RGB565 framebuffer conversion.
- `GbaEmulatorService` owns one native core instance and runs the frame loop on a dedicated worker thread.
- The live wallpaper binds to the service and renders the most recent frame with nearest-neighbour scaling.
- The HUD broadcasts button-down and button-up events; the emulator service maps these to libretro joypad IDs.
- ROMs are read from a user-selected Android document URI and are not included in the repository.

## Remaining work and known limitations

1. Run and fix the first GitHub Actions native build; the code has not yet been verified by a completed CI build.
2. Install on a physical Android device and validate ROM loading, frame output, input latency, wallpaper lifecycle, and overlay behavior.
3. Implement audio output using Android AudioTrack and connect the libretro audio callback.
4. Persist battery-backed save RAM using `retro_get_memory_data(RETRO_MEMORY_SAVE_RAM)` and `retro_get_memory_size`.
5. Add save-state support after ordinary save RAM is reliable.
6. Add lifecycle-safe pause/resume and ensure the emulator worker stops cleanly when the session ends.
7. Expand ABI coverage only after arm64-v8a is stable.

## Architecture

The foreground service is the only owner of the libretro core. The wallpaper is a consumer of the latest framebuffer and must never initialize or step the core itself. The HUD sends input transitions to the service; it does not access JNI directly. This avoids separate emulator instances and keeps frame execution serialized.

The JNI bridge currently returns an ARGB frame array to Kotlin for drawing. This is a correctness-first bridge; a direct buffer or shared-memory path can be considered if profiling shows copy overhead is material.

## Core selection and source

mGBA is pinned to upstream tag `0.10.5` and built through its libretro frontend. The upstream project documents that it does not currently maintain an official Android port; the community Android SDL port is a reference for toolchain configuration, not a dependency of this app.

- Upstream source: https://github.com/mgba-emu/mgba/tree/0.10.5
- mGBA FAQ / Android support status: https://mgba.io/faq.html
- libretro core documentation: https://docs.libretro.com/library/mgba/
- Community Android build reference: https://github.com/Gitsnup/mgba-android
- License: [MPL-2.0](../licenses/MGBA-MPL-2.0.txt)

No commercial ROM or BIOS image is bundled. The test ROM remains user-supplied and local to the device.
