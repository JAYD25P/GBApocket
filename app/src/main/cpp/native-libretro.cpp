#include <jni.h>
#include <algorithm>
#include <cstdint>
#include <cstring>
#include <mutex>
#include <vector>

extern "C" {
#include "libretro.h"
}

namespace {
std::mutex coreMutex;
std::mutex frameMutex;
std::vector<uint8_t> romBytes;
std::vector<uint16_t> framePixels(240 * 160, 0);
unsigned frameWidth = 240;
unsigned frameHeight = 160;
uint16_t pressedButtons = 0;
bool initialized = false;
bool loaded = false;

bool environment(unsigned command, void* data) {
    switch (command) {
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
            auto* format = static_cast<retro_pixel_format*>(data);
            return format && *format == RETRO_PIXEL_FORMAT_RGB565;
        }
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            if (data) *static_cast<bool*>(data) = false;
            return true;
        case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:
            if (data) *static_cast<unsigned*>(data) = 0;
            return true;
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            if (data) *static_cast<const char**>(data) = nullptr;
            return true;
        default:
            return false;
    }
}

void videoRefresh(const void* data, unsigned width, unsigned height, size_t pitch) {
    if (!data || width == 0 || height == 0 || width > 240 || height > 160) return;
    std::lock_guard<std::mutex> lock(frameMutex);
    frameWidth = width;
    frameHeight = height;
    const auto* source = static_cast<const uint8_t*>(data);
    for (unsigned y = 0; y < height; ++y) {
        const auto* row = reinterpret_cast<const uint16_t*>(source + y * pitch);
        std::copy(row, row + width, framePixels.begin() + y * 240);
    }
}

size_t audioBatch(const int16_t*, size_t frames) { return frames; }
void inputPoll() {}
int16_t inputState(unsigned port, unsigned device, unsigned index, unsigned id) {
    if (port != 0 || device != RETRO_DEVICE_JOYPAD || index != 0 || id >= 16) return 0;
    return (pressedButtons & (1u << id)) ? 1 : 0;
}
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_pocketgba_home_NativeGbaCore_nativeInit(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(coreMutex);
    if (initialized) return JNI_TRUE;
    retro_set_environment(environment);
    retro_set_video_refresh(videoRefresh);
    retro_set_audio_sample_batch(audioBatch);
    retro_set_input_poll(inputPoll);
    retro_set_input_state(inputState);
    retro_init();
    initialized = true;
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_pocketgba_home_NativeGbaCore_nativeLoadRom(JNIEnv* env, jclass, jbyteArray bytes) {
    if (!bytes) return JNI_FALSE;
    std::lock_guard<std::mutex> lock(coreMutex);
    if (!initialized) return JNI_FALSE;
    if (loaded) {
        retro_unload_game();
        loaded = false;
    }
    const jsize size = env->GetArrayLength(bytes);
    if (size <= 0) return JNI_FALSE;
    romBytes.resize(static_cast<size_t>(size));
    env->GetByteArrayRegion(bytes, 0, size, reinterpret_cast<jbyte*>(romBytes.data()));
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        romBytes.clear();
        return JNI_FALSE;
    }
    retro_game_info game{};
    game.data = romBytes.data();
    game.size = romBytes.size();
    if (!retro_load_game(&game)) {
        romBytes.clear();
        return JNI_FALSE;
    }
    loaded = true;
    pressedButtons = 0;
    std::fill(framePixels.begin(), framePixels.end(), 0);
    frameWidth = 240;
    frameHeight = 160;
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_pocketgba_home_NativeGbaCore_nativeRunFrame(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(coreMutex);
    if (initialized && loaded) retro_run();
}

extern "C" JNIEXPORT void JNICALL
Java_com_pocketgba_home_NativeGbaCore_nativeSetButton(JNIEnv*, jclass, jint id, jboolean down) {
    if (id < 0 || id >= 16) return;
    std::lock_guard<std::mutex> lock(coreMutex);
    const uint16_t mask = static_cast<uint16_t>(1u << id);
    if (down) pressedButtons |= mask;
    else pressedButtons &= static_cast<uint16_t>(~mask);
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_pocketgba_home_NativeGbaCore_nativeGetFrame(JNIEnv* env, jclass) {
    std::lock_guard<std::mutex> lock(frameMutex);
    const jsize count = 240 * 160;
    jintArray result = env->NewIntArray(count);
    if (!result) return nullptr;
    std::vector<jint> argb(static_cast<size_t>(count), 0xFF000000);
    for (unsigned y = 0; y < frameHeight; ++y) {
        for (unsigned x = 0; x < frameWidth; ++x) {
            const uint16_t p = framePixels[y * 240 + x];
            const int r = ((p >> 11) & 0x1F) * 255 / 31;
            const int g = ((p >> 5) & 0x3F) * 255 / 63;
            const int b = (p & 0x1F) * 255 / 31;
            argb[y * 240 + x] = static_cast<jint>(0xFF000000u | (r << 16) | (g << 8) | b);
        }
    }
    env->SetIntArrayRegion(result, 0, count, argb.data());
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_pocketgba_home_NativeGbaCore_nativeShutdown(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(coreMutex);
    if (loaded) retro_unload_game();
    loaded = false;
    if (initialized) retro_deinit();
    initialized = false;
    pressedButtons = 0;
    romBytes.clear();
}
