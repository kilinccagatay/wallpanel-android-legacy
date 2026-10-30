/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.iot.wallpanellegacy.ui.activities

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Toast
import com.thanksmister.iot.wallpanellegacy.R
import com.thanksmister.iot.wallpanellegacy.network.WallPanelService
import com.thanksmister.iot.wallpanellegacy.network.WallPanelService.Companion.BROADCAST_ALERT_MESSAGE
import com.thanksmister.iot.wallpanellegacy.network.WallPanelService.Companion.BROADCAST_CLEAR_ALERT_MESSAGE
import com.thanksmister.iot.wallpanellegacy.network.WallPanelService.Companion.BROADCAST_TOAST_MESSAGE
import com.thanksmister.iot.wallpanellegacy.persistence.Configuration
import com.thanksmister.iot.wallpanellegacy.utils.DialogUtils
import com.thanksmister.iot.wallpanellegacy.ui.views.BatteryBadgeView
import dagger.android.support.DaggerAppCompatActivity
import timber.log.Timber
import java.io.File
import javax.inject.Inject

abstract class BrowserActivity : DaggerAppCompatActivity() {

    @Inject lateinit var dialogUtils: DialogUtils
    @Inject lateinit var configuration: Configuration

    var mOnScrollChangedListener: ViewTreeObserver.OnScrollChangedListener? = null
    private var wallPanelService: Intent? = null
    private var decorView: View? = null
    private var batteryBadge: BatteryBadgeView? = null
    private var systemBarProgress: ProgressDialog? = null
    private var batteryReceiverRegistered = false
    private val systemUiHandler = Handler()
    private var systemBarVisible = false
    private val rehideSystemBar = Runnable {
        if (!systemBarVisible) {
            applySystemBarVisibility()
        }
    }
    private val autoHideSystemBar = Runnable {
        systemBarVisible = false
        dismissSystemBarProgress()
        requestRootSystemBar(false)
        applySystemBarVisibility()
        scheduleSystemBarRehide()
    }
    private val powerKeyHandler = Handler()
    private var powerKeyLongPressed = false
    private val powerKeyLongPress = Runnable {
        powerKeyLongPressed = true
        try {
            // A CAMERA-mapped key can reach the app while the Allwinner LCD is
            // physically off. Wake the panel before the root helper opens the
            // system power dialog so the user can actually see it.
            resetScreen()
            File(Environment.getExternalStorageDirectory(), POWER_MENU_TRIGGER).createNewFile()
            Timber.d("Dashboard power key long press requested")
        } catch (error: Exception) {
            Timber.e(error, "Unable to request the Android power menu")
        }
    }
    var displayProgress = true
    var zoomLevel = 1.0f

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val percentage = if (scale > 0) (level * 100 / scale) else 0
            val status = intent.getIntExtra(
                    BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            batteryBadge?.setBatteryState(percentage, charging)
        }
    }

    // handler for received data from service
    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BROADCAST_ACTION_LOAD_URL == intent.action) {
                val url = intent.getStringExtra(BROADCAST_ACTION_LOAD_URL)
                Timber.d("Browsing to $url")
                loadUrl(url)
            } else if (BROADCAST_ACTION_JS_EXEC == intent.action) {
                val js = intent.getStringExtra(BROADCAST_ACTION_JS_EXEC)
                Timber.d("Executing javascript in current browser: $js")
                evaluateJavascript(js)
            } else if (BROADCAST_ACTION_CLEAR_BROWSER_CACHE == intent.action) {
                Timber.d("Clearing browser cache")
                clearCache()
            } else if (BROADCAST_ACTION_RELOAD_PAGE == intent.action) {
                Timber.d("Browser page reloading.")
                reload()
            } else if (BROADCAST_TOAST_MESSAGE == intent.action && !isFinishing) {
                val message = intent.getStringExtra(BROADCAST_TOAST_MESSAGE)
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            } else if (BROADCAST_ALERT_MESSAGE == intent.action && !isFinishing) {
                val message = intent.getStringExtra(BROADCAST_ALERT_MESSAGE)
                dialogUtils.showAlertDialog(this@BrowserActivity, message)
            } else if (BROADCAST_CLEAR_ALERT_MESSAGE == intent.action && !isFinishing) {
                dialogUtils.clearDialogs()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        displayProgress = configuration.appShowActivity
        zoomLevel = configuration.testZoomLevel

        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        decorView = window.decorView
        decorView?.setOnSystemUiVisibilityChangeListener { visibility ->
            if (!systemBarVisible && !usesRootLegacySystemBar() &&
                    visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0) {
                // Android 4.1's lean-back mode reveals the navigation bar on
                // any touch. Reapply the requested hidden state after the tap
                // has reached the dashboard.
                // Jelly Bean finishes revealing the legacy SystemBar well
                // after this callback returns. Reapply once after that touch
                // cycle; repeatedly restarting a short retry prevents the
                // request from ever reaching the ROM's reliable threshold.
                scheduleSystemBarRehide()
            }
        }

        if(configuration.cameraEnabled || configuration.hasCameraDetections() && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            window.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        }

        if (configuration.appPreventSleep) {
            window.addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        lifecycle.addObserver(dialogUtils)
    }

    override fun onResume() {
        super.onResume()
        systemUiHandler.removeCallbacks(autoHideSystemBar)
        systemBarVisible = false
        requestRootSystemBar(false)
        batteryBadge = findViewById<View>(R.id.battery_badge) as BatteryBadgeView
        batteryBadge?.setOnLongClickListener {
            systemBarVisible = !systemBarVisible
            systemUiHandler.removeCallbacks(rehideSystemBar)
            requestRootSystemBar(systemBarVisible)
            applySystemBarVisibility()
            if (systemBarVisible) {
                showSystemBarProgress()
                systemUiHandler.postDelayed(
                        autoHideSystemBar, SYSTEM_BAR_AUTO_HIDE_DELAY_MS)
            } else {
                dismissSystemBarProgress()
                systemUiHandler.removeCallbacks(autoHideSystemBar)
            }
            true
        }
        if (!batteryReceiverRegistered) {
            val batteryIntent = registerReceiver(
                    batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryReceiverRegistered = true
            if (batteryIntent != null) {
                batteryReceiver.onReceive(this, batteryIntent)
            }
        }
        val filter = IntentFilter()
        filter.addAction(BROADCAST_ACTION_LOAD_URL)
        filter.addAction(BROADCAST_ACTION_JS_EXEC)
        filter.addAction(BROADCAST_ACTION_CLEAR_BROWSER_CACHE)
        filter.addAction(BROADCAST_ACTION_RELOAD_PAGE)
        filter.addAction(BROADCAST_CLEAR_ALERT_MESSAGE)
        filter.addAction(BROADCAST_ALERT_MESSAGE)
        filter.addAction(BROADCAST_TOAST_MESSAGE)
        val bm = LocalBroadcastManager.getInstance(this)
        bm.registerReceiver(mBroadcastReceiver, filter)
        // Some Android 4.1 builds do not deliver a fresh focus callback when
        // this activity is restored. Enforce the requested kiosk state on
        // every resume instead of relying on onWindowFocusChanged alone.
        applySystemBarVisibility()
        scheduleSystemBarRehide()
    }

    override fun onPause() {
        super.onPause()
        // If the user leaves WallPanel through the temporarily visible Android
        // controls, leave SystemUI available outside the kiosk. onResume()
        // requests the hidden kiosk state again when WallPanel returns.
        systemUiHandler.removeCallbacks(autoHideSystemBar)
        dismissSystemBarProgress()
        if (batteryReceiverRegistered) {
            unregisterReceiver(batteryReceiver)
            batteryReceiverRegistered = false
        }
        val bm = LocalBroadcastManager.getInstance(this)
        bm.unregisterReceiver(mBroadcastReceiver)
    }

    override fun onStart() {
        super.onStart()
        wallPanelService = Intent(this, WallPanelService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(wallPanelService)
        } else {
            startService(wallPanelService)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applySystemBarVisibility()
        }
    }

    private fun applySystemBarVisibility() {
        if (systemBarVisible) {
            decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            return
        }

        val visibility: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            visibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else if (usesRootLegacySystemBar()) {
            // Jelly Bean creates a TYPE_HIDDEN_NAV_CONSUMER input window for
            // HIDE_NAVIGATION. It consumes the next touch before our Activity.
            // The installed root helper already owns actual SystemUI visibility;
            // request only stable layout/fullscreen, never the touch consumer.
            visibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            visibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        } else {
            visibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        decorView?.systemUiVisibility = visibility
    }

    private val rootLegacySystemBar by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT &&
                File("/system/xbin/wallpanel-systemui").canExecute() &&
                File("/system/framework/wallpanel-systemui.jar").isFile
    }

    private fun usesRootLegacySystemBar(): Boolean = rootLegacySystemBar

    private fun scheduleSystemBarRehide() {
        systemUiHandler.removeCallbacks(rehideSystemBar)
        systemUiHandler.postDelayed(rehideSystemBar, SYSTEM_BAR_REHIDE_DELAY_MS)
    }

    private fun requestRootSystemBar(show: Boolean) {
        try {
            val trigger = if (show) SYSTEM_BAR_SHOW_TRIGGER else SYSTEM_BAR_HIDE_TRIGGER
            File(Environment.getExternalStorageDirectory(), trigger).createNewFile()
        } catch (error: Exception) {
            Timber.e(error, "Unable to request rooted SystemUI state")
        }
    }

    @Suppress("DEPRECATION")
    private fun showSystemBarProgress() {
        dismissSystemBarProgress()
        systemBarProgress = ProgressDialog(this).apply {
            setMessage("Android kontrolleri açılıyor…")
            setIndeterminate(true)
            setCancelable(false)
            show()
        }
        systemUiHandler.postDelayed(
                { dismissSystemBarProgress() }, SYSTEM_BAR_PROGRESS_DELAY_MS)
    }

    private fun dismissSystemBarProgress() {
        if (systemBarProgress?.isShowing == true && !isFinishing) {
            systemBarProgress?.dismiss()
        }
        systemBarProgress = null
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val handled = super.dispatchTouchEvent(event)
        if (!systemBarVisible && !usesRootLegacySystemBar()) {
            // ViewRootImpl clears Jelly Bean's HIDE_NAVIGATION bit immediately
            // before dispatching a touch to the Activity. Restore it after the
            // dashboard has received the same event but before the next frame
            // can draw the SystemBar.
            systemUiHandler.removeCallbacks(rehideSystemBar)
            applySystemBarVisibility()
        }
        return handled
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // The Denver's physical power key is remapped to CAMERA by the rooted
        // panel profile. A short press toggles only the physical LCD so the
        // legacy camera remains active. A long press asks the root helper to
        // inject a real held POWER key, preserving Android's global power menu.
        if (event.keyCode == KeyEvent.KEYCODE_CAMERA) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                powerKeyLongPressed = false
                powerKeyHandler.postDelayed(
                        powerKeyLongPress, ViewConfiguration.getLongPressTimeout().toLong())
            } else if (event.action == KeyEvent.ACTION_UP) {
                powerKeyHandler.removeCallbacks(powerKeyLongPress)
                if (!powerKeyLongPressed) {
                    Timber.d("Dashboard power key short press")
                    toggleScreen()
                }
            }
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            Timber.d("dispatchKeyEvent")
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            startActivity(Intent(this@BrowserActivity, SettingsActivity::class.java))
            finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    internal fun resetScreen() {
        Timber.d("resetScreen Called")
        val intent = Intent(WallPanelService.BROADCAST_EVENT_SCREEN_TOUCH)
        intent.putExtra(WallPanelService.BROADCAST_EVENT_SCREEN_TOUCH, true)
        val bm = LocalBroadcastManager.getInstance(applicationContext)
        bm.sendBroadcast(intent)
    }

    private fun toggleScreen() {
        val intent = Intent(WallPanelService.BROADCAST_EVENT_POWER_TOGGLE)
        val bm = LocalBroadcastManager.getInstance(applicationContext)
        bm.sendBroadcast(intent)
    }

    internal fun pageLoadComplete(url: String) {
        Timber.d("pageLoadComplete currentUrl $url")
        val intent = Intent(WallPanelService.BROADCAST_EVENT_URL_CHANGE)
        intent.putExtra(WallPanelService.BROADCAST_EVENT_URL_CHANGE, url)
        val bm = LocalBroadcastManager.getInstance(applicationContext)
        bm.sendBroadcast(intent)
        complete()
    }

    protected abstract fun configureWebSettings(userAgent: String)
    protected abstract fun loadUrl(url: String)
    protected abstract fun evaluateJavascript(js: String)
    protected abstract fun clearCache()
    protected abstract fun reload()
    protected abstract fun complete()

    companion object {
        const val BROADCAST_ACTION_LOAD_URL = "BROADCAST_ACTION_LOAD_URL"
        const val BROADCAST_ACTION_JS_EXEC = "BROADCAST_ACTION_JS_EXEC"
        const val BROADCAST_ACTION_CLEAR_BROWSER_CACHE = "BROADCAST_ACTION_CLEAR_BROWSER_CACHE"
        const val BROADCAST_ACTION_RELOAD_PAGE = "BROADCAST_ACTION_RELOAD_PAGE"
        private const val POWER_MENU_TRIGGER = ".wallpanel-power-longpress"
        private const val SYSTEM_BAR_REHIDE_DELAY_MS = 700L
        private const val SYSTEM_BAR_AUTO_HIDE_DELAY_MS = 12000L
        private const val SYSTEM_BAR_PROGRESS_DELAY_MS = 2500L
        private const val SYSTEM_BAR_SHOW_TRIGGER = ".wallpanel-systemui-show"
        private const val SYSTEM_BAR_HIDE_TRIGGER = ".wallpanel-systemui-hide"
    }
}
