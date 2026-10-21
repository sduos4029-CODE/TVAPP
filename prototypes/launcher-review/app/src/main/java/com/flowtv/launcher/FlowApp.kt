package com.flowtv.launcher

import android.app.Application
import android.content.Context
import android.os.Process

object CrashHistory {
    fun recent(raw: String, now: Long): List<Long> = raw.split(',').mapNotNull { it.toLongOrNull() }
        .filter { it <= now && now - it < 60_000 }.takeLast(3)
    // The process may terminate immediately after this write.
    @android.annotation.SuppressLint("ApplySharedPref")
    fun record(context: Context, now: Long = System.currentTimeMillis()) {
        val p = context.getSharedPreferences("guard", Context.MODE_PRIVATE)
        p.edit().putString("crashes", (recent(p.getString("crashes", "").orEmpty(), now) + now)
            .takeLast(3).joinToString(",")).commit()
    }
}

class FlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val p = getSharedPreferences("guard", Context.MODE_PRIVATE)
        if (CrashHistory.recent(p.getString("crashes", "").orEmpty(), System.currentTimeMillis()).size >= 3) {
            SafeMode.enter(this, openOther = false)
            p.edit().remove("crashes").apply()
        }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try { CrashHistory.record(this) } finally {
                if (previous != null) previous.uncaughtException(thread, error)
                else Process.killProcess(Process.myPid())
            }
        }
    }
}
