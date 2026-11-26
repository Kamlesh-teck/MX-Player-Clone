package com.example.mxclone

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: AppPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = AppPrefs(this)
        prefs.applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val rowTheme = findViewById<LinearLayout>(R.id.rowTheme)
        val tvThemeValue = findViewById<TextView>(R.id.tvThemeValue)
        val swFloatingPlay = findViewById<SwitchMaterial>(R.id.swFloatingPlay)
        val swRememberBrightness = findViewById<SwitchMaterial>(R.id.swRememberBrightness)
        val swRememberAspect = findViewById<SwitchMaterial>(R.id.swRememberAspect)
        val rowSeekTime = findViewById<LinearLayout>(R.id.rowSeekTime)
        val tvSeekValue = findViewById<TextView>(R.id.tvSeekValue)
        val swAutoPip = findViewById<SwitchMaterial>(R.id.swAutoPip)
        val swGestureControl = findViewById<SwitchMaterial>(R.id.swGestureControl)

        val themes = arrayOf("Follow system", "Light", "Dark")
        tvThemeValue.text = themes[prefs.themeMode]
        tvSeekValue.text = "${prefs.seekSeconds}s"

        swFloatingPlay.isChecked = prefs.showFloatingPlay
        swRememberBrightness.isChecked = prefs.rememberBrightness
        swRememberAspect.isChecked = prefs.rememberAspectRatio
        swAutoPip.isChecked = prefs.autoPip
        swGestureControl.isChecked = prefs.gestureControl

        rowTheme.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setSingleChoiceItems(themes, prefs.themeMode) { dialog, which ->
                    prefs.themeMode = which
                    tvThemeValue.text = themes[which]
                    prefs.applyTheme()
                    dialog.dismiss()
                }
                .show()
        }

        rowSeekTime.setOnClickListener {
            val times = arrayOf("5s", "10s", "15s", "30s")
            val values = intArrayOf(5, 10, 15, 30)
            val currentIdx = values.indexOf(prefs.seekSeconds).coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("Fast forward / rewind time")
                .setSingleChoiceItems(times, currentIdx) { dialog, which ->
                    prefs.seekSeconds = values[which]
                    tvSeekValue.text = times[which]
                    dialog.dismiss()
                }
                .show()
        }

        swFloatingPlay.setOnCheckedChangeListener { _, isChecked -> prefs.showFloatingPlay = isChecked }
        swRememberBrightness.setOnCheckedChangeListener { _, isChecked -> prefs.rememberBrightness = isChecked }
        swRememberAspect.setOnCheckedChangeListener { _, isChecked -> prefs.rememberAspectRatio = isChecked }
        swAutoPip.setOnCheckedChangeListener { _, isChecked -> prefs.autoPip = isChecked }
        swGestureControl.setOnCheckedChangeListener { _, isChecked -> prefs.gestureControl = isChecked }
    }
}
