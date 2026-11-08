package com.pocketgba.home

/**
 * JNI boundary for the bundled mGBA libretro core.
 *
 * Calls are synchronous and must be serialized by the eventual emulator-session
 * service. The wallpaper and HUD must not call the native core independently.
 */
object NativeGbaCore {
    init {
        System.loadLibrary("mgba_libretro")
        System.loadLibrary("pocketgba")
    }

    external fun nativeInit(): Boolean
    external fun nativeLoadRom(bytes: ByteArray): Boolean
    external fun nativeRunFrame()
    external fun nativeSetButton(libretroJoypadId: Int, pressed: Boolean)
    external fun nativeGetFrame(): IntArray
    external fun nativeShutdown()

    const val B = 0
    const val Y = 1
    const val SELECT = 2
    const val START = 3
    const val UP = 4
    const val DOWN = 5
    const val LEFT = 6
    const val RIGHT = 7
    const val A = 8
    const val X = 9
    const val L = 10
    const val R = 11
}
