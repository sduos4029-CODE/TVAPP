package tv.flow.mobile;

import android.app.NotificationManager;
import android.content.*;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.*;
import java.util.*;

/** Reads only active official YouTube media sessions after explicit Android notification access. */
final class YouTubeMedia {
  final MainActivity a;
  final ComponentName component;
  final MediaSessionManager manager;
  MediaController controller;
  boolean listening;
  final MediaController.Callback callback = new MediaController.Callback() {
    @Override public void onMetadataChanged(MediaMetadata metadata) { changed(); }
    @Override public void onPlaybackStateChanged(PlaybackState state) { changed(); }
    @Override public void onSessionDestroyed() { bind(Collections.emptyList()); }
  };
  final MediaSessionManager.OnActiveSessionsChangedListener sessions = this::bind;

  YouTubeMedia(MainActivity activity) {
    a = activity;
    component = new ComponentName(a, MediaAccessService.class);
    manager = (MediaSessionManager) a.getSystemService(Context.MEDIA_SESSION_SERVICE);
  }

  static boolean isYouTube(String pkg) {
    return Arrays.asList("com.google.android.youtube", "com.google.android.youtube.tv",
        "com.google.android.youtube.tvmusic", "com.google.android.apps.youtube.music").contains(pkg);
  }

  boolean allowed() {
    if (Build.VERSION.SDK_INT >= 27)
      return ((NotificationManager) a.getSystemService(Context.NOTIFICATION_SERVICE))
          .isNotificationListenerAccessGranted(component);
    String enabled = android.provider.Settings.Secure.getString(a.getContentResolver(), "enabled_notification_listeners");
    if (enabled != null) for (String item : enabled.split(":"))
      if (component.equals(ComponentName.unflattenFromString(item))) return true;
    return false;
  }

  void start() {
    stop();
    // YouTube's supported-devices help limits background play to mobile apps.
    // TV picture-off, if a vendor supports it, is a separate in-source device function.
    if(a.tv()) {changed();return;}
    if (!allowed() || manager == null) { changed(); return; }
    try {
      manager.addOnActiveSessionsChangedListener(sessions, component, a.handler);
      listening = true;
      bind(manager.getActiveSessions(component));
    } catch (SecurityException e) { stop(); changed(); }
  }

  void stop() {
    if (listening && manager != null) manager.removeOnActiveSessionsChangedListener(sessions);
    listening = false;
    if (controller != null) controller.unregisterCallback(callback);
    controller = null;
  }

  void bind(List<MediaController> sessions) {
    if (controller != null) controller.unregisterCallback(callback);
    controller = null;
    if (sessions != null) for (MediaController candidate : sessions) {
      if (!isYouTube(candidate.getPackageName())) continue;
      if (controller == null) controller = candidate;
      PlaybackState state = candidate.getPlaybackState();
      if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
        controller = candidate;
        break;
      }
    }
    if (controller != null) controller.registerCallback(callback, a.handler);
    changed();
  }

  void changed() { if (a.live != null) a.live.updateMedia(); }
  MediaController active() { return a.localMode() ? (a.localAudio==null?null:a.localAudio.controller) : controller; }
  PlaybackState state() { MediaController c=active(); return c == null ? null : c.getPlaybackState(); }
  MediaMetadata metadata() { MediaController c=active(); return c == null ? null : c.getMetadata(); }
  String text(String key, String empty) {
    MediaMetadata m = metadata();
    String s = m == null ? null : m.getString(key);
    if(m!=null && (s==null || s.trim().isEmpty())) {
      if(MediaMetadata.METADATA_KEY_TITLE.equals(key)) s=m.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
      if(MediaMetadata.METADATA_KEY_ARTIST.equals(key)) s=m.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE);
    }
    return s == null || s.trim().isEmpty() ? empty : s;
  }
  long duration() {
    MediaMetadata m = metadata();
    return m == null ? 0 : Math.max(0, m.getLong(MediaMetadata.METADATA_KEY_DURATION));
  }
  long position() {
    PlaybackState s = state();
    if (s == null) return 0;
    long pos = s.getPosition();
    if (s.getState() == PlaybackState.STATE_PLAYING && s.getLastPositionUpdateTime() > 0)
      pos += (long) ((SystemClock.elapsedRealtime() - s.getLastPositionUpdateTime()) * s.getPlaybackSpeed());
    return Math.max(0, duration() > 0 ? Math.min(duration(), pos) : pos);
  }
  Bitmap artwork() {
    MediaMetadata m = metadata();
    if (m == null) return null;
    Bitmap b = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
    if(b==null)b=m.getBitmap(MediaMetadata.METADATA_KEY_ART);
    return b==null?m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON):b;
  }
  boolean supports(long action) { PlaybackState s = state(); return s != null && (s.getActions() & action) != 0; }
  void act(long action) {
    if (active() == null || !supports(action)) { a.toast("This source is not exposing this control"); return; }
    MediaController.TransportControls t = active().getTransportControls();
    if (action == PlaybackState.ACTION_PLAY) t.play();
    else if (action == PlaybackState.ACTION_PAUSE) t.pause();
    else if (action == PlaybackState.ACTION_PLAY_PAUSE) {
      if (state().getState() == PlaybackState.STATE_PLAYING) t.pause(); else t.play();
    } else if (action == PlaybackState.ACTION_SKIP_TO_NEXT) t.skipToNext();
    else if (action == PlaybackState.ACTION_SKIP_TO_PREVIOUS) t.skipToPrevious();
  }
  void seek(long position) {
    if (active() != null && supports(PlaybackState.ACTION_SEEK_TO))
      active().getTransportControls().seekTo(Math.max(0, duration() > 0 ? Math.min(duration(), position) : position));
    else a.toast("This source is not exposing seek controls");
  }
}
