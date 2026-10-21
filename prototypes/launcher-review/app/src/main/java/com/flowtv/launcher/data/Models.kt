package com.flowtv.launcher.data

import android.graphics.Bitmap
import android.net.Uri

data class AppItem(val label: String, val pkg: String, val icon: Bitmap, val banner: Bitmap?)
data class TitleCard(val id: Long, val title: String, val posterUri: Uri?, val launchUri: String,
                     val source: String, val watchType: Int = -1, val preview: Boolean = false) {
    val key get() = "$source:$preview:$id"
}
data class TitleFeed(val watchNext: List<TitleCard> = emptyList(), val previews: List<TitleCard> = emptyList(),
                     val status: String = "Not checked")
