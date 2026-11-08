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
    private lateinit var romButton: Button
    private var selectedRom: Uri? = null

    private val romPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedRom = uri
            getSharedPreferences("session", MODE_PRIVATE).edit()
                .putString("rom_uri", uri.toString()).apply()
            status.text = "ROM selected. Start the emulator session to load it."
            romButton.text = "ROM selected — choose another"
        } catch (_: SecurityException) {
            status.text = "Android did not grant persistent access. Choose the file again."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedRom = getSharedPreferences("session", MODE_PRIVATE).getString("rom_uri", null)?.let(Uri::parse)

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
            text = if (selectedRom != null) "ROM selected on this device. Start the emulator session to load it."
                else "Live wallpaper display + floating controller"
            textSize = 14f
            setTextColor(0xFFB8C5BC.toInt())
            setPadding(0, 18, 0, 24)
        }
        fun action(label: String, run: () -> Unit): Button {
            val button = Button(this).apply { text = label; setOnClickListener { run() } }
            layout.addView(button)
            return button
        }
        layout.addView(title)
        layout.addView(status)
        romButton = action(if (selectedRom == null) "Choose GBA ROM (.gba)" else "ROM selected — choose another") {
            romPicker.launch(arrayOf("application/octet-stream", "*/*"))
        }
        action("Start emulator session") {
            val uri = selectedRom
            if (uri == null) {
                status.text = "Choose a GBA ROM first."
            } else {
                val intent = Intent(this, GbaEmulatorService::class.java)
                    .setAction(GbaEmulatorService.ACTION_LOAD_ROM)
                    .setData(uri)
                startForegroundService(intent)
                status.text = "Starting mGBA and loading the selected ROM…"
            }
        }
        action("Grant overlay permission") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else status.text = "Overlay permission is already enabled."
        }
        action("Start floating controller") {
            if (!Settings.canDrawOverlays(this)) {
                status.text = "Grant overlay permission before starting the controller."
            } else {
                startForegroundService(Intent(this, HudService::class.java))
                status.text = "Controller started. Its buttons are routed to the emulator session."
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
        action("Stop emulator session") {
            startService(Intent(this, GbaEmulatorService::class.java).setAction(GbaEmulatorService.ACTION_STOP))
            status.text = "Emulator session stopped."
        }
        setContentView(layout)
    }
}
