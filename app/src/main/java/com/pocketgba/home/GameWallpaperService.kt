package com.pocketgba.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

/** Renders the latest frame produced by the single foreground emulator session. */
class GameWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = GameEngine()

    inner class GameEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint().apply { isFilterBitmap = false; isDither = false }
        private var visible = false
        private var emulator: GbaEmulatorService.LocalBinder? = null
        private var bound = false
        private val bitmap = Bitmap.createBitmap(240, 160, Bitmap.Config.ARGB_8888)
        private val destination = RectF()

        private val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                emulator = service as? GbaEmulatorService.LocalBinder
                emulator?.setDisplayVisible(visible)
                if (visible) {
                    handler.removeCallbacks(tick)
                    handler.post(tick)
                }
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                emulator = null
            }
        }

        private val tick = object : Runnable {
            override fun run() {
                if (visible) {
                    drawFrame()
                    handler.postDelayed(this, 16L)
                }
            }
        }

        init {
            bound = bindService(
                Intent(this@GameWallpaperService, GbaEmulatorService::class.java),
                connection, Context.BIND_AUTO_CREATE
            )
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            emulator?.setDisplayVisible(isVisible)
            handler.removeCallbacks(tick)
            if (visible) handler.post(tick)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (visible) drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            emulator?.setDisplayVisible(false)
            handler.removeCallbacks(tick)
            super.onSurfaceDestroyed(holder)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK)
                    val frame = emulator?.frame()
                    if (frame != null && frame.size == 240 * 160) {
                        bitmap.setPixels(frame, 0, 240, 0, 0, 240, 160)
                        val scale = minOf(canvas.width / 240f, canvas.height / 160f)
                        val drawWidth = 240f * scale
                        val drawHeight = 160f * scale
                        val left = (canvas.width - drawWidth) / 2f
                        val top = (canvas.height - drawHeight) / 2f
                        destination.set(left, top, left + drawWidth, top + drawHeight)
                        canvas.drawBitmap(bitmap, null, destination, paint)
                    } else {
                        paint.color = Color.rgb(210, 230, 210)
                        paint.textAlign = Paint.Align.CENTER
                        paint.textSize = canvas.width * 0.055f
                        canvas.drawText("POCKETGBA", canvas.width / 2f, canvas.height / 2f, paint)
                        paint.textSize = canvas.width * 0.027f
                        canvas.drawText("START A ROM SESSION", canvas.width / 2f, canvas.height / 2f + 34f, paint)
                    }
                }
            } catch (_: Exception) {
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
        }

        override fun onDestroy() {
            handler.removeCallbacks(tick)
            emulator?.setDisplayVisible(false)
            if (bound) runCatching { unbindService(connection) }
            bitmap.recycle()
            super.onDestroy()
        }
    }
}
