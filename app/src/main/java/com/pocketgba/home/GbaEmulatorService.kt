package com.pocketgba.home

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

/**
 * Owns the single native mGBA session. Wallpaper reads its latest frame;
 * the floating HUD sends button transitions through the app-private broadcast.
 */
class GbaEmulatorService : Service() {
    inner class LocalBinder : Binder() {
        fun frame(): IntArray? = latestFrame
        fun status(): String = sessionStatus
        fun isGameLoaded(): Boolean = gameLoaded
    }

    private val binder = LocalBinder()
    @Volatile private var latestFrame: IntArray? = null
    @Volatile private var sessionStatus = "Waiting for a ROM"
    @Volatile private var gameLoaded = false
    @Volatile private var running = false
    private var coreAvailable = false
    private var foregroundStarted = false
    private var emulationThread: Thread? = null

    private val inputReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != HudService.ACTION_INPUT || !coreAvailable || !gameLoaded) return
            val key = intent.getStringExtra("key") ?: return
            val pressed = intent.getBooleanExtra("pressed", false)
            val id = when (key) {
                "B" -> NativeGbaCore.B
                "A" -> NativeGbaCore.A
                "UP" -> NativeGbaCore.UP
                "DOWN" -> NativeGbaCore.DOWN
                "LEFT" -> NativeGbaCore.LEFT
                "RIGHT" -> NativeGbaCore.RIGHT
                "START" -> NativeGbaCore.START
                "SELECT" -> NativeGbaCore.SELECT
                "L" -> NativeGbaCore.L
                "R" -> NativeGbaCore.R
                else -> return
            }
            NativeGbaCore.nativeSetButton(id, pressed)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this, inputReceiver, IntentFilter(HudService.ACTION_INPUT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForeground()
        ensureCore()
        if (intent?.action == ACTION_LOAD_ROM) {
            val uri = intent.data
            if (uri != null) loadRom(uri)
            else sessionStatus = "No ROM file was supplied"
        }
        return START_NOT_STICKY
    }

    private fun ensureForeground() {
        if (foregroundStarted) return
        val channel = "pocketgba_emulator"
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channel, "PocketGBA emulator", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, channel)
                .setContentTitle("PocketGBA session")
                .setContentText("Emulator service is ready")
                .setSmallIcon(android.R.drawable.ic_media_play).build()
        } else {
            Notification.Builder(this)
                .setContentTitle("PocketGBA session")
                .setSmallIcon(android.R.drawable.ic_media_play).build()
        }
        startForeground(42, notification)
        foregroundStarted = true
    }

    private fun ensureCore() {
        if (coreAvailable) return
        try {
            coreAvailable = NativeGbaCore.nativeInit()
            sessionStatus = if (coreAvailable) "mGBA core initialized; choose a ROM" else "mGBA core initialization failed"
        } catch (error: Throwable) {
            coreAvailable = false
            sessionStatus = "Native core unavailable: ${error.javaClass.simpleName}"
        }
    }

    private fun loadRom(uri: Uri) {
        if (!coreAvailable) {
            sessionStatus = "Native mGBA core did not initialize"
            return
        }
        try {
            val output = ByteArrayOutputStream()
            contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(64 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    require(total <= MAX_ROM_BYTES) { "ROM exceeds 32 MiB limit" }
                    output.write(buffer, 0, read)
                }
            } ?: throw IllegalStateException("Android could not open the selected ROM")
            val bytes = output.toByteArray()
            require(bytes.isNotEmpty()) { "Selected ROM is empty" }
            gameLoaded = NativeGbaCore.nativeLoadRom(bytes)
            if (!gameLoaded) {
                sessionStatus = "mGBA rejected this ROM file"
                return
            }
            sessionStatus = "ROM loaded — rendering gameplay"
            startEmulationLoop()
        } catch (error: Throwable) {
            gameLoaded = false
            sessionStatus = "ROM load failed: ${error.message ?: error.javaClass.simpleName}"
        }
    }

    private fun startEmulationLoop() {
        if (running) return
        running = true
        emulationThread = thread(name = "PocketGBA-Core", isDaemon = true) {
            val frameNanos = 16_742_706L
            while (running && gameLoaded) {
                val start = System.nanoTime()
                NativeGbaCore.nativeRunFrame()
                latestFrame = NativeGbaCore.nativeGetFrame()
                val remaining = frameNanos - (System.nanoTime() - start)
                if (remaining > 0) {
                    try {
                        Thread.sleep(remaining / 1_000_000L, (remaining % 1_000_000L).toInt())
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
            running = false
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        running = false
        emulationThread?.interrupt()
        runCatching { emulationThread?.join(500) }
        runCatching { unregisterReceiver(inputReceiver) }
        if (coreAvailable) runCatching { NativeGbaCore.nativeShutdown() }
        gameLoaded = false
        latestFrame = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_LOAD_ROM = "com.pocketgba.home.LOAD_ROM"
        private const val MAX_ROM_BYTES = 32 * 1024 * 1024
    }
}
