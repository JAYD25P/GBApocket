package com.pocketgba.home

import android.service.wallpaper.WallpaperService
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder

/**
 * Live display surface. Prototype diagnostic frame only.
 * Replace this renderer with an mGBA/libretro JNI framebuffer provider.
 */
class GameWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = GameEngine()

    inner class GameEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var visible = false
        private val tick = object : Runnable {
            override fun run() {
                if (visible) drawFrame()
                handler.postDelayed(this, 33L)
            }
        }
        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            handler.removeCallbacks(tick)
            if (visible) handler.post(tick)
        }
        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (visible) drawFrame()
        }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(tick)
            super.onSurfaceDestroyed(holder)
        }
        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.rgb(19, 36, 29))
                    val w = canvas.width.toFloat()
                    val h = canvas.height.toFloat()
                    paint.color = Color.rgb(43, 75, 56)
                    canvas.drawRoundRect(RectF(w*.06f, h*.20f, w*.94f, h*.76f), 24f, 24f, paint)
                    paint.color = Color.rgb(211, 235, 190)
                    paint.textAlign = Paint.Align.CENTER
                    paint.textSize = w * .09f
                    canvas.drawText("POCKETGBA", w/2, h*.43f, paint)
                    paint.textSize = w * .035f
                    canvas.drawText("EMULATOR CORE NOT CONNECTED", w/2, h*.51f, paint)
                }
            } catch (_: Exception) {
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
