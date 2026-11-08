cmake_minimum_required(VERSION 3.22.1)
project(pocketgba LANGUAGES C CXX)

include(FetchContent)

# Build only the GBA libretro core; PocketGBA supplies its own Android frontend.
set(BUILD_QT OFF CACHE BOOL "" FORCE)
set(BUILD_SDL OFF CACHE BOOL "" FORCE)
set(BUILD_LIBRETRO ON CACHE BOOL "" FORCE)
set(BUILD_PERF OFF CACHE BOOL "" FORCE)
set(BUILD_TEST OFF CACHE BOOL "" FORCE)
set(BUILD_SUITE OFF CACHE BOOL "" FORCE)
set(BUILD_CINEMA OFF CACHE BOOL "" FORCE)
set(BUILD_HEADLESS OFF CACHE BOOL "" FORCE)
set(BUILD_EXAMPLE OFF CACHE BOOL "" FORCE)
set(BUILD_PYTHON OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED OFF CACHE BOOL "" FORCE)
set(BUILD_STATIC OFF CACHE BOOL "" FORCE)
set(SKIP_LIBRARY ON CACHE BOOL "" FORCE)
set(M_CORE_GBA ON CACHE BOOL "" FORCE)
set(M_CORE_GB OFF CACHE BOOL "" FORCE)
set(ENABLE_DEBUGGERS OFF CACHE BOOL "" FORCE)
set(ENABLE_SCRIPTING OFF CACHE BOOL "" FORCE)
set(USE_DISCORD_RPC OFF CACHE BOOL "" FORCE)
set(USE_FFMPEG OFF CACHE BOOL "" FORCE)
set(USE_ZLIB OFF CACHE BOOL "" FORCE)
set(USE_MINIZIP OFF CACHE BOOL "" FORCE)
set(USE_PNG OFF CACHE BOOL "" FORCE)
set(USE_LIBZIP OFF CACHE BOOL "" FORCE)
set(USE_SQLITE3 OFF CACHE BOOL "" FORCE)
set(USE_ELF OFF CACHE BOOL "" FORCE)
set(USE_LUA OFF CACHE BOOL "" FORCE)
set(USE_JSON_C OFF CACHE BOOL "" FORCE)
set(USE_FREETYPE OFF CACHE BOOL "" FORCE)
set(USE_LZMA OFF CACHE BOOL "" FORCE)
set(BUILD_GL OFF CACHE BOOL "" FORCE)
set(BUILD_GLES2 OFF CACHE BOOL "" FORCE)
set(BUILD_GLES3 OFF CACHE BOOL "" FORCE)
set(USE_EPOXY OFF CACHE BOOL "" FORCE)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

FetchContent_Declare(
    mgba
    GIT_REPOSITORY https://github.com/mgba-emu/mgba.git
    GIT_TAG 0.10.5
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(mgba)

if(NOT TARGET mgba_libretro)
    message(FATAL_ERROR "mGBA did not create the expected mgba_libretro target")
endif()

add_library(pocketgba SHARED native-libretro.cpp)
target_compile_features(pocketgba PRIVATE cxx_std_17)
target_include_directories(pocketgba PRIVATE
    ${mgba_SOURCE_DIR}/src/platform/libretro
)
target_link_libraries(pocketgba PRIVATE mgba_libretro android log)
