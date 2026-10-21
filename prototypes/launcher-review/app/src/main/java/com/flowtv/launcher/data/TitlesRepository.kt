package com.flowtv.launcher.data

import android.content.*
import android.database.Cursor
import android.media.tv.TvContract
import android.net.Uri
import com.flowtv.launcher.SafeMode

/** OS-permitted published TV metadata only. Never asks for root or private app data. */
class TitlesRepository(private val ctx: Context) {
    fun load(): TitleFeed {
        if (!SafeMode.isTv(ctx)) return TitleFeed(status = "Phone testing: Android TV shared listings are unavailable. Your streaming-app logins remain private.")
        if (ctx.packageManager.resolveContentProvider("android.media.tv", 0) == null)
            return TitleFeed(status = "No accessible Android TV listings provider on this device")
        val failures = mutableListOf<String>()
        val watch = read(TvContract.WatchNextPrograms.CONTENT_URI, false, failures)
        val previews = read(TvContract.PreviewPrograms.CONTENT_URI, true, failures)
        val status = if (failures.isNotEmpty()) "Some TV listings are restricted or unavailable; only OS-permitted cards appear."
            else if (watch.isEmpty() && previews.isEmpty()) "No readable shared TV cards. This is not a full streaming catalogue."
            else "Limited app-published TV cards, not full catalogues or proof of subscription access."
        return TitleFeed(watch, previews, status)
    }
    private fun read(uri: Uri, preview: Boolean, failures: MutableList<String>): List<TitleCard> = try {
        val columns = mutableListOf("_id", "title", "poster_art_uri", "intent_uri", "package_name", "browsable", "searchable")
        if (!preview) columns += "watch_next_type"
        buildList {
            ctx.contentResolver.query(uri, columns.toTypedArray(), null, null, null)?.use { c ->
                var read = 0
                while (c.moveToNext() && read++ < 200) parse(c, preview, ctx.packageName)?.let { add(it) }
            }
        }.distinctBy { it.key }
    } catch (_: RuntimeException) { failures += uri.lastPathSegment.orEmpty(); emptyList() }

    companion object {
        private fun text(c: Cursor, key: String): String {
            val index = c.getColumnIndex(key)
            return if (index < 0 || c.isNull(index)) "" else c.getString(index).orEmpty()
        }
        private fun number(c: Cursor, key: String, fallback: Int): Int {
            val index = c.getColumnIndex(key)
            return if (index < 0 || c.isNull(index)) fallback else c.getInt(index)
        }
        fun parse(c: Cursor, preview: Boolean, ownPackage: String): TitleCard? {
            val title = text(c, "title"); val source = text(c, "package_name")
            if (title.isBlank() || source.isBlank() || source == ownPackage ||
                number(c, "browsable", 1) == 0 || number(c, "searchable", 1) == 0) return null
            val idIndex = c.getColumnIndex("_id")
            if (idIndex < 0 || c.isNull(idIndex)) return null
            val poster = runCatching { Uri.parse(text(c, "poster_art_uri")) }.getOrNull()
                ?.takeIf { it.scheme in listOf("https", "content", "android.resource") && it.userInfo == null }
            return TitleCard(c.getLong(idIndex), title, poster, text(c, "intent_uri"), source,
                if (preview) -1 else number(c, "watch_next_type", -1), preview)
        }
        fun safeTitleIntent(card: TitleCard): Intent? = runCatching {
            if (!card.source.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) return null
            val parsed = Intent.parseUri(card.launchUri, Intent.URI_INTENT_SCHEME)
            val data = parsed.data ?: return null
            val scheme = data.scheme?.lowercase() ?: return null
            if (scheme in listOf("file", "content", "javascript", "data", "intent", "tel", "sms", "market") || data.userInfo != null) return null
            Intent(Intent.ACTION_VIEW, data).setPackage(card.source).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.getOrNull()
    }
}
