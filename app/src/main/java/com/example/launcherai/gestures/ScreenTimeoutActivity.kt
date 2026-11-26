package com.example.launcherai.gestures

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Fallback Screen Lock Timeout strategy (matching Nova TimeOutActivity):
 * Briefly dims/blacks out screen and minimizes screen-off timeout so device sleeps cleanly
 * without invalidating biometrics, then restores the user's previous timeout.
 */
class ScreenTimeoutActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var previousTimeoutMs: Int = -1
    private var hasChangedTimeout = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.System.canWrite(this)) {
            Toast.makeText(
                this,
                "Allow 'Modify system settings' for Timeout screen lock",
                Toast.LENGTH_LONG
            ).show()
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:$packageName")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
            }
            finish()
            return
        }

        val blackoutView = View(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            setOnClickListener { finish() }
        }
        setContentView(blackoutView)

        WindowCompat.getInsetsController(window, blackoutView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF
        }

        try {
            previousTimeoutMs = Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 1000)
            hasChangedTimeout = true
        } catch (_: Exception) {
            finish()
            return
        }

        // Automatically restore and close if user didn't let it sleep after 12s
        handler.postDelayed({ finish() }, 12_000L)
    }

    override fun onPause() {
        super.onPause()
        restoreTimeout()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        restoreTimeout()
        super.onDestroy()
    }

    private fun restoreTimeout() {
        if (hasChangedTimeout && previousTimeoutMs > 0) {
            hasChangedTimeout = false
            try {
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, previousTimeoutMs)
            } catch (_: Exception) {
            }
        }
    }
}
