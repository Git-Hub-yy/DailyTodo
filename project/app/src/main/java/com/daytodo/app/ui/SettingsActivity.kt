package com.daytodo.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.daytodo.app.R
import com.daytodo.app.databinding.ActivitySettingsBinding
import com.daytodo.app.prefs.Prefs
import com.daytodo.app.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val prefs by lazy { Prefs(this) }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) saveImage(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.toggleTheme.check(
            when (prefs.themeMode) {
                Prefs.THEME_LIGHT -> R.id.btnThemeLight
                Prefs.THEME_DARK -> R.id.btnThemeDark
                else -> R.id.btnThemeSystem
            }
        )
        binding.toggleTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            prefs.themeMode = when (checkedId) {
                R.id.btnThemeLight -> Prefs.THEME_LIGHT
                R.id.btnThemeDark -> Prefs.THEME_DARK
                else -> Prefs.THEME_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(prefs.nightMode)
        }

        binding.sliderDim.value = prefs.widgetDim.toFloat().coerceIn(0f, 90f)
        binding.sliderDim.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefs.widgetDim = value.toInt()
                WidgetUpdater.refresh(this)
            }
        }

        binding.toggleTextColor.check(
            when (prefs.widgetTextColor) {
                Prefs.TEXT_DARK -> R.id.btnTextDark
                Prefs.TEXT_AUTO -> R.id.btnTextAuto
                else -> R.id.btnTextLight
            }
        )
        binding.toggleTextColor.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            prefs.widgetTextColor = when (checkedId) {
                R.id.btnTextDark -> Prefs.TEXT_DARK
                Prefs.TEXT_AUTO -> Prefs.TEXT_AUTO
                else -> Prefs.TEXT_LIGHT
            }
            WidgetUpdater.refresh(this)
        }

        binding.switchShowDone.isChecked = prefs.widgetShowDone
        binding.switchShowDone.setOnCheckedChangeListener { _, checked ->
            prefs.widgetShowDone = checked
            WidgetUpdater.refresh(this)
        }

        binding.btnPickImage.setOnClickListener { pickImage.launch("image/*") }
        binding.btnClearImage.setOnClickListener { clearImage() }
        binding.btnRefreshWidget.setOnClickListener {
            lifecycleScope.launch {
                WidgetUpdater.updateAll(applicationContext)
                Toast.makeText(this@SettingsActivity, R.string.widget_refreshed, Toast.LENGTH_SHORT)
                    .show()
            }
        }
        binding.btnAutoStart.setOnClickListener { openAutoStartSettings() }
        binding.btnBatteryFree.setOnClickListener { requestIgnoreBatteryOptimization() }

        updatePreview()
    }

    /** 打开各家系统的「自启动 / 关联启动」白名单页面 */
    private fun openAutoStartSettings() {
        val candidates = listOf(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ),
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            ),
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            ),
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            ),
            ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
        )
        for (component in candidates) {
            val intent = Intent().setComponent(component)
            if (packageManager.resolveActivity(intent, 0) != null) {
                val ok = runCatching { startActivity(intent) }.isSuccess
                if (ok) return
            }
        }
        openAppDetails()
    }

    /** 申请「忽略电池优化」，让系统不要压制本应用的后台广播 */
    private fun requestIgnoreBatteryOptimization() {
        val power = getSystemService(PowerManager::class.java)
        if (power != null && power.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, R.string.msg_battery_already, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:$packageName"))
        val ok = runCatching { startActivity(intent) }.isSuccess
        if (!ok) openAppDetails()
    }

    private fun openAppDetails() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        runCatching { startActivity(intent) }
    }

    private fun updatePreview() {
        val path = prefs.widgetBgPath
        val file = if (path != null) File(path) else null
        if (file != null && file.exists()) {
            binding.imagePreview.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            binding.textBgState.text = "已设置背景图"
        } else {
            binding.imagePreview.setImageDrawable(null)
            binding.textBgState.text = getString(R.string.no_bg)
        }
    }

    private fun clearImage() {
        prefs.widgetBgPath?.let { runCatching { File(it).delete() } }
        prefs.widgetBgPath = null
        updatePreview()
        WidgetUpdater.refresh(this)
    }

    private fun saveImage(uri: Uri) {
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes == null) return@runCatching null

                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

                    var sample = 1
                    while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) {
                        sample *= 2
                    }
                    val bitmap = BitmapFactory.decodeByteArray(
                        bytes,
                        0,
                        bytes.size,
                        BitmapFactory.Options().apply { inSampleSize = sample }
                    )
                    if (bitmap == null) return@runCatching null

                    val file = File(filesDir, "widget_bg.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    file.absolutePath
                }.getOrNull()
            }

            if (saved != null) {
                prefs.widgetBgPath = saved
                updatePreview()
                WidgetUpdater.refresh(this@SettingsActivity)
            } else {
                Toast.makeText(this@SettingsActivity, "图片设置失败，换一张试试", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }
}
