package com.flowtv.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.flowtv.launcher.ui.*

class SettingsActivity : ComponentActivity() {
    private var revision by mutableIntStateOf(0)
    override fun onResume() { super.onResume(); revision++ }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent {
            FlowTheme {
                revision
                Column(Modifier.fillMaxSize().background(Navy).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Flow Prototype Settings", style = MaterialTheme.typography.headlineMedium)
                    Text(if (SafeMode.isActive(this@SettingsActivity)) "Recovery mode: this app’s Home alias is disabled" else "Normal app mode", color = Lavender)
                    Text(SafeMode.currentHomeLabel(this@SettingsActivity), color = Muted)
                    Text(if (SafeMode.isTv(this@SettingsActivity)) "Enabling the TV Home option does not select a default. Choose it yourself in Android Home settings."
                        else "Phone testing: Flow opens as a normal app. TV Home integration is disabled on this device.", color = Muted)
                    FlowButton(onClick = { SafeMode.enableTvCandidate(this@SettingsActivity); revision++ }, enabled = SafeMode.isTv(this@SettingsActivity)) {
                        Text("Enable TV Home option…") }
                    FlowButton(onClick = { SafeMode.enter(this@SettingsActivity); revision++ }) { Text("Disable Flow Home / open other Home") }
                    FlowButton(onClick = { SafeMode.openHomeSettings(this@SettingsActivity) }) { Text("Open Android Home settings") }
                    FlowButton(onClick = { startActivity(Intent(this@SettingsActivity, HomeActivity::class.java)) }) { Text("Open Flow prototype") }
                    Text("TV cards use only OS-permitted published metadata. No root, privileged app installation, notification access or private streaming logins are used.", color = Muted)
                    Text("Crash-loop recovery disables only this app’s optional Home alias. Long Volume Up / Home-intent shortcuts still require physical-TV verification. No Dashboard or audio player is included in this smaller prototype.", color = Muted)
                }
            }
        }
    }
}
