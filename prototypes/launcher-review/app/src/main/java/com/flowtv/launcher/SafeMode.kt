package com.flowtv.launcher

import android.app.UiModeManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.provider.Settings
import android.widget.Toast

object SafeMode {
    fun isTv(ctx: Context) = (ctx.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager)
        .currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    private fun alias(ctx: Context) = ComponentName(ctx, "${ctx.packageName}.HomeAlias")
    fun homeCandidateEnabled(ctx: Context) = ctx.packageManager.getComponentEnabledSetting(alias(ctx)) ==
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    fun isActive(ctx: Context) = ctx.getSharedPreferences("guard", 0).getBoolean("safe_mode", false)
    // Persist recovery state before handing control to another Home activity.
    @android.annotation.SuppressLint("ApplySharedPref")
    fun enter(ctx: Context, openOther: Boolean = true) {
        ctx.getSharedPreferences("guard", 0).edit().putBoolean("safe_mode", true).commit()
        ctx.packageManager.setComponentEnabledSetting(alias(ctx),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        if (openOther) openOtherHome(ctx)
    }
    fun enableTvCandidate(ctx: Context): Boolean {
        if (!isTv(ctx)) return false
        ctx.getSharedPreferences("guard", 0).edit().putBoolean("safe_mode", false).apply()
        ctx.packageManager.setComponentEnabledSetting(alias(ctx),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        openHomeSettings(ctx)
        return true
    }
    fun resolvedHome(ctx: Context): ComponentName? {
        val info = ctx.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY) ?: return null
        return ComponentName(info.activityInfo.packageName, info.activityInfo.name)
    }
    fun currentHomeLabel(ctx: Context): String {
        val component = resolvedHome(ctx) ?: return "No Home activity resolved"
        return if (component.packageName == ctx.packageName) "Android resolves Home to Flow TV Prototype"
        else "Android resolves Home to ${component.packageName}"
    }
    fun openOtherHome(ctx: Context): Boolean {
        val component = resolvedHome(ctx)
        if (component == null || component.packageName == ctx.packageName) {
            openHomeSettings(ctx); return false
        }
        return launch(ctx, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(component))
    }
    fun openHomeSettings(ctx: Context) {
        if (!launch(ctx, Intent(Settings.ACTION_HOME_SETTINGS)))
            launch(ctx, Intent(Settings.ACTION_SETTINGS))
    }
    fun launch(ctx: Context, intent: Intent): Boolean = try {
        ctx.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(ctx, "This device does not expose that screen", Toast.LENGTH_SHORT).show(); false
    } catch (_: SecurityException) {
        Toast.makeText(ctx, "Android restricts this action", Toast.LENGTH_SHORT).show(); false
    }
}

class HomePressTracker {
    private val times = ArrayDeque<Long>()
    fun press(now: Long): Boolean {
        times.addLast(now)
        while (times.isNotEmpty() && (times.first() > now || now - times.first() > 2000)) times.removeFirst()
        return (times.size >= 3).also { if (it) times.clear() }
    }
}
