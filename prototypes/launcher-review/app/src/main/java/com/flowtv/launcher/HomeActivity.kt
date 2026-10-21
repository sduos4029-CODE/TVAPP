package com.flowtv.launcher

import android.app.AlertDialog
import android.content.*
import android.database.ContentObserver
import android.media.tv.TvContract
import android.os.*
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.flowtv.launcher.data.*
import com.flowtv.launcher.ui.*
import kotlinx.coroutines.*

class HomeActivity : ComponentActivity() {
    private lateinit var appsRepo: AppsRepository
    private lateinit var titlesRepo: TitlesRepository
    private var apps by mutableStateOf<List<AppItem>>(emptyList())
    private var feed by mutableStateOf(TitleFeed(status = "Checking supported device capabilities…"))
    private var loading by mutableStateOf(false)
    private var allApps by mutableStateOf(false)
    private var refreshJob: Job? = null
    private var loadRevision = 0
    private val presses = HomePressTracker()
    private val handler = Handler(Looper.getMainLooper())
    private var registered = false
    private var observing = false
    private val escape = Runnable { if (SafeMode.isTv(this) && SafeMode.resolvedHome(this)?.packageName == packageName) SafeMode.enter(this) }
    private val packages = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { refresh() }
    }
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) { refresh() }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        appsRepo = AppsRepository(this); titlesRepo = TitlesRepository(this)
        allApps = state?.getBoolean("allApps") ?: false
        setContent {
            FlowTheme { HomeScreen(apps, feed, loading, allApps, ::launchApp, ::openTitle, ::refresh,
                { startActivity(Intent(this, SettingsActivity::class.java)) }, { allApps = true }, { allApps = false }) }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (allApps) allApps = false
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed(); isEnabled = true }
            }
        })
        homeIntent(intent)
    }
    override fun onSaveInstanceState(out: Bundle) { out.putBoolean("allApps", allApps); super.onSaveInstanceState(out) }
    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(this, packages, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED); addDataScheme("package")
        }, ContextCompat.RECEIVER_EXPORTED)
        registered = true
        if (SafeMode.isTv(this)) try {
            contentResolver.registerContentObserver(TvContract.WatchNextPrograms.CONTENT_URI, true, observer)
            contentResolver.registerContentObserver(TvContract.PreviewPrograms.CONTENT_URI, true, observer)
            observing = true
        } catch (_: RuntimeException) { contentResolver.unregisterContentObserver(observer) }
    }
    override fun onResume() { super.onResume(); refresh() }
    override fun onStop() {
        refreshJob?.cancel(); handler.removeCallbacks(escape)
        if (registered) { unregisterReceiver(packages); registered = false }
        if (observing) { contentResolver.unregisterContentObserver(observer); observing = false }
        super.onStop()
    }
    private fun refresh() {
        refreshJob?.cancel()
        val issued = ++loadRevision
        refreshJob = lifecycleScope.launch {
            loading = true
            try {
                val result = withContext(Dispatchers.IO) { appsRepo.load() to titlesRepo.load() }
                apps = result.first; feed = result.second
            } catch (e: CancellationException) { throw e }
            catch (_: RuntimeException) { feed = TitleFeed(status = "Device data unavailable. Retry or open Settings.") }
            finally { if (issued == loadRevision) loading = false }
        }
    }
    private fun launchApp(app: AppItem) {
        val target = appsRepo.launchIntent(app.pkg)
        if (target == null || !SafeMode.launch(this, target)) {
            Toast.makeText(this, "App unavailable; refreshing installed apps", Toast.LENGTH_SHORT).show(); refresh()
        }
    }
    private fun openTitle(card: TitleCard) {
        AlertDialog.Builder(this).setTitle(card.title)
            .setMessage("Published by ${card.source}. The source app controls login, availability and playback. This is not a full catalogue or entitlement check.")
            .setPositiveButton("Open in source app") { _, _ ->
                val title = TitlesRepository.safeTitleIntent(card)
                if (title != null && title.resolveActivity(packageManager) != null && SafeMode.launch(this, title)) return@setPositiveButton
                val fallback = appsRepo.launchIntent(card.source)
                if (fallback != null) { SafeMode.launch(this, fallback); Toast.makeText(this, "No supported title link; opened the app", Toast.LENGTH_LONG).show() }
                else Toast.makeText(this, "Source app or title link unavailable", Toast.LENGTH_LONG).show()
            }.setNegativeButton("Close", null).show()
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.repeatCount == 0 && SafeMode.isTv(this) && SafeMode.resolvedHome(this)?.packageName == packageName)
            handler.postDelayed(escape, 1500)
        return super.onKeyDown(keyCode, event)
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) handler.removeCallbacks(escape)
        return super.onKeyUp(keyCode, event)
    }
    private fun homeIntent(intent: Intent) {
        if (SafeMode.isTv(this) && SafeMode.resolvedHome(this)?.packageName == packageName && intent.hasCategory(Intent.CATEGORY_HOME) && presses.press(SystemClock.uptimeMillis())) SafeMode.enter(this)
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); homeIntent(intent) }
}
