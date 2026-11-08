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

    private fun emitKey(key: String, pressed: Boolean) {
        sendBroadcast(Intent(ACTION_INPUT).setPackage(packageName)
            .putExtra("key", key).putExtra("pressed", pressed))
    }

    private fun showHud() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 10, 12, 10)
            background = GradientDrawable().apply {
                setColor(0xCC171D27.toInt())
                cornerRadius = 42f
                setStroke(2, 0x667D91A8)
            }
        }

        fun button(label: String, key: String): TextView = TextView(this).apply {
            text = label
            textSize = if (label.length > 1) 12f else 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(12, 15, 12, 15)
            background = GradientDrawable().apply {
                setColor(0xFF465267.toInt())
                cornerRadius = 32f
            }
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> emitKey(key, true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> emitKey(key, false)
                }
                true
            }
        }

        val dpad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        dpad.addView(button("▲", "UP"))
        val middle = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        middle.addView(button("◀", "LEFT"))
        middle.addView(button("▶", "RIGHT"))
        dpad.addView(middle)
        dpad.addView(button("▼", "DOWN"))
        root.addView(dpad)

        val system = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(8, 0, 8, 0)
        }
        val systemTop = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        systemTop.addView(button("SELECT", "SELECT"))
        systemTop.addView(button("START", "START"))
        system.addView(systemTop)
        val shoulders = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        shoulders.addView(button("L", "L"))
        shoulders.addView(button("R", "R"))
        system.addView(shoulders)
        root.addView(system)

        val ab = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        ab.addView(button("B", "B"))
        ab.addView(button("A", "A"))
        root.addView(ab)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = 70 }

        // Drag from the controller's outer padding/background; button touches remain key presses.
        root.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f
            private var startY = 0f
            private var originX = 0
            private var originY = 0
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        originX = lp.x
                        originY = lp.y
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        lp.x = originX + (event.rawX - startX).toInt()
                        lp.y = originY - (event.rawY - startY).toInt()
                        runCatching { wm.updateViewLayout(root, lp) }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> return true
                }
                return false
            }
        })
        hud = root
        wm.addView(root, lp)
    }

    override fun onDestroy() {
        hud?.let { runCatching { wm.removeView(it) } }
        hud = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_INPUT = "com.pocketgba.home.INPUT"
    }
}
