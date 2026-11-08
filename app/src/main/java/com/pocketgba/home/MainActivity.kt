package com.pocketgba.home

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private val romPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            getSharedPreferences("session", MODE_PRIVATE).edit().putString("rom_uri", it.toString()).apply()
            status.text = "ROM selected. Native emulator core is not connected in this prototype."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32)
            setBackgroundColor(0xFF111820.toInt())
        }
        val title = TextView(this).apply {
            text = "POCKETGBA HOME"
            textSize = 25f
            setTextColor(0xFFE8F0E8.toInt())
        }
        status = TextView(this).apply {
            text = "Hybrid live-display + floating HUD prototype"
            textSize = 14f
            setTextColor(0xFFB8C5BC.toInt())
            setPadding(0, 18, 0, 24)
        }
        fun action(label: String, run: () -> Unit) {
            layout.addView(Button(this).apply { text = label; setOnClickListener { run() } })
        }
        layout.addView(title); layout.addView(status)
        action("Choose GBA ROM (.gba)") { romPicker.launch(arrayOf("*/*")) }
        action("Grant overlay permission") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        action("Start floating controller") {
            if (!Settings.canDrawOverlays(this)) status.text = "Grant overlay permission first."
            else {
                startForegroundService(Intent(this, HudService::class.java))
                status.text = "Controller overlay started. Set the live wallpaper separately."
            }
        }
        action("Set live game display wallpaper") {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, GameWallpaperService::class.java))
            }
            startActivity(intent)
        }
        action("Stop controller") { stopService(Intent(this, HudService::class.java)) }
        setContentView(layout)
    }
}
