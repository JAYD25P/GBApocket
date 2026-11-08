package com.pocketgba.home

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class HudService : Service() {
    private lateinit var wm: WindowManager
    private var hud: View? = null
    override fun onCreate() {
        super.onCreate()
        val channel = "pocketgba_hud"
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channel, "PocketGBA controls", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(this, channel).setContentTitle("PocketGBA controller active")
                .setSmallIcon(android.R.drawable.ic_media_play).build()
        else Notification.Builder(this).setContentTitle("PocketGBA controller active")
            .setSmallIcon(android.R.drawable.ic_media_play).build()
        startForeground(41, notification)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showHud()
    }
    private fun showHud() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 12, 16, 12)
            background = GradientDrawable().apply {
                setColor(0xCC202733.toInt()); cornerRadius = 48f
                setStroke(2, 0x667D91A8)
            }
        }
        fun button(label: String, key: String) = TextView(this).apply {
            text = label; textSize = 17f; setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(13, 18, 13, 18)
            background = GradientDrawable().apply { setColor(0xFF465267.toInt()); cornerRadius = 36f }
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
                    sendBroadcast(Intent(ACTION_INPUT).setPackage(packageName)
                        .putExtra("key", key).putExtra("pressed", event.action == MotionEvent.ACTION_DOWN))
                }
                true
            }
        }
        val dpad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        dpad.addView(button("▲", "UP"))
        val middle = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        middle.addView(button("◀", "LEFT")); middle.addView(button("•", "CENTER")); middle.addView(button("▶", "RIGHT"))
        dpad.addView(middle); dpad.addView(button("▼", "DOWN"))
        root.addView(dpad)
        val ab = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        ab.addView(button("B", "B")); ab.addView(button("A", "A"))
        root.addView(ab)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = 70 }
        root.setOnTouchListener(object : View.OnTouchListener {
            var sx=0f; var sy=0f; var ox=0; var oy=0
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_DOWN) { sx=e.rawX; sy=e.rawY; ox=lp.x; oy=lp.y; return true }
                if (e.action == MotionEvent.ACTION_MOVE) {
                    lp.x = ox + (e.rawX-sx).toInt(); lp.y = oy - (e.rawY-sy).toInt()
                    wm.updateViewLayout(root, lp); return true
                }
                return false
            }
        })
        hud = root
        wm.addView(root, lp)
    }
    override fun onDestroy() { hud?.let { runCatching { wm.removeView(it) } }; hud=null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { const val ACTION_INPUT = "com.pocketgba.home.INPUT" }
}
