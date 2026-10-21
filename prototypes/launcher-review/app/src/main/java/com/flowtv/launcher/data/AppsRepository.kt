package com.flowtv.launcher.data

import android.content.Context
import android.content.Intent
import androidx.core.graphics.drawable.toBitmap
import com.flowtv.launcher.SafeMode

class AppsRepository(private val ctx: Context) {
    fun load(): List<AppItem> {
        val pm = ctx.packageManager
        val discovered = linkedMapOf<String, AppItem>()
        val categories = if (SafeMode.isTv(ctx)) listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
            else listOf(Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER)
        categories.forEach { category ->
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0).forEach { info ->
                val pkg = info.activityInfo.packageName
                if (pkg != ctx.packageName && info.activityInfo.enabled && info.activityInfo.exported && pkg !in discovered) {
                    runCatching {
                        discovered[pkg] = AppItem(info.loadLabel(pm)?.toString() ?: pkg, pkg,
                            info.loadIcon(pm).toBitmap(96, 96), info.activityInfo.loadBanner(pm)?.toBitmap(320, 180))
                    }
                }
            }
        }
        return discovered.values.sortedBy { it.label.lowercase() }
    }
    fun launchIntent(pkg: String): Intent? {
        val pm = ctx.packageManager
        val intent = if (SafeMode.isTv(ctx)) pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
            else pm.getLaunchIntentForPackage(pkg) ?: pm.getLeanbackLaunchIntentForPackage(pkg)
        return intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
