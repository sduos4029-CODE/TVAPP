package tv.flow.mobile;

import android.app.*;
import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.media.*;
import android.media.session.*;
import android.net.Uri;
import android.os.*;
import java.util.*;
import org.json.*;

/** User-started local playback, with a real media session and foreground media notification. */
public final class LocalAudioService extends Service {
  static final String PLAY = "tv.flow.PLAY", TOGGLE = "tv.flow.TOGGLE", STOP = "tv.flow.STOP", RESUME="tv.flow.RESUME", NEXT="tv.flow.NEXT", PREVIOUS="tv.flow.PREVIOUS";
  final Binder binder = new LocalBinder();
  MediaSession session;
  MediaController controller;
  MediaPlayer player;
  AudioManager audio;
  AudioFocusRequest focus;
  boolean resumeAfterFocus, foreground, prepared;
  int track;
  Runnable changed;
  final ArrayList<Integer> queue = new ArrayList<>();
  final ArrayList<JSONObject> library = new ArrayList<>();
  public final class LocalBinder extends Binder { LocalAudioService service() { return LocalAudioService.this; } }
  @Override public IBinder onBind(Intent intent) { return binder; }
  @Override public void onCreate() {
    super.onCreate();
    audio = (AudioManager)getSystemService(AUDIO_SERVICE);
    focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setOnAudioFocusChangeListener(change -> {
          if (change == AudioManager.AUDIOFOCUS_LOSS) { resumeAfterFocus = false; pause(); }
          else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) { resumeAfterFocus = playing(); pause(); }
          else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) { if(player != null) player.setVolume(.2f,.2f); }
          else if (change == AudioManager.AUDIOFOCUS_GAIN) { if(player != null) player.setVolume(1,1); if(resumeAfterFocus) { resumeAfterFocus=false; resume(); } }
        }).build();
    NotificationManager notifications = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    notifications.createNotificationChannel(new NotificationChannel("local-audio", "Local audio playback", NotificationManager.IMPORTANCE_LOW));
    session = new MediaSession(this, "Flow local audio");
    session.setCallback(new MediaSession.Callback() {
      @Override public void onPlay() { startForegroundService(new Intent(LocalAudioService.this,LocalAudioService.class).setAction(RESUME)); }
      @Override public void onPause() { pause(); }
      @Override public void onStop() { stopPlayback(); }
      @Override public void onSkipToNext() { startForegroundService(new Intent(LocalAudioService.this,LocalAudioService.class).setAction(NEXT)); }
      @Override public void onSkipToPrevious() { startForegroundService(new Intent(LocalAudioService.this,LocalAudioService.class).setAction(PREVIOUS)); }
      @Override public void onSeekTo(long position) { if(prepared) { player.seekTo((int)Math.max(0,Math.min(player.getDuration(),position))); publish(); } }
    });
    session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
    session.setActive(false);
    controller = new MediaController(this, session.getSessionToken());
    refreshLibrary(); publish();
  }
  void refreshLibrary() {
    library.clear();
    try {
      String[] names={"Mint Horizon", "Slate Rain", "Lavender Night"};
      String[] files={"mint.wav","slate.wav","lavender.wav"};
      for(int n=0;n<3;n++) library.add(new JSONObject().put("name",names[n]).put("artist","Flow Studio").put("album","Original bundled samples").put("asset",files[n]));
      JSONArray saved = new JSONArray(getSharedPreferences("flow",0).getString("localAudio", "[]"));
      for(int n=0;n<saved.length();n++) if(saved.optJSONObject(n)!=null) library.add(saved.optJSONObject(n));
    } catch(JSONException ignored) {}
  }
  @Override public int onStartCommand(Intent intent, int flags, int id) {
    if(intent==null) { stopSelf(); return START_NOT_STICKY; }
    // Android requires prompt foreground promotion after startForegroundService().
    startForeground(41, notification()); foreground=true;
    if(PLAY.equals(intent.getAction())) play(intent.getIntExtra("track",0));
    else if(RESUME.equals(intent.getAction())) resume();
    else if(NEXT.equals(intent.getAction())) next();
    else if(PREVIOUS.equals(intent.getAction())) play((track+library.size()-1)%library.size());
    else if(TOGGLE.equals(intent.getAction())) { if(playing()) pause(); else resume(); }
    else if(STOP.equals(intent.getAction())) stopPlayback();
    return START_NOT_STICKY;
  }
  void play(int index) {
    refreshLibrary();
    if(index<0 || index>=library.size()) return;
    if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { pause(); return; }
    if(player!=null) player.release();
    prepared=false; track=index;
    session.setActive(true);
    JSONObject item=library.get(track);
    session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,item.optString("name","Local audio")).putString(MediaMetadata.METADATA_KEY_ARTIST,item.optString("artist","Artist unavailable")).build());
    try {
      player=new MediaPlayer();
      player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
      player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
      if(item.has("asset")) {
        try(AssetFileDescriptor file=getAssets().openFd(item.getString("asset"))) { player.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength()); }
      } else player.setDataSource(this,Uri.parse(item.getString("uri")));
      player.setOnPreparedListener(mp -> {
        prepared=true;
        MediaMetadata.Builder metadata=new MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE,item.optString("name","Local audio"))
            .putString(MediaMetadata.METADATA_KEY_ARTIST,item.optString("artist","Artist unavailable"))
            .putString(MediaMetadata.METADATA_KEY_ALBUM,item.optString("album",""))
            .putLong(MediaMetadata.METADATA_KEY_DURATION,mp.getDuration());
        if(item.has("uri")) {
          MediaMetadataRetriever retriever=new MediaMetadataRetriever();
          try {
            retriever.setDataSource(LocalAudioService.this,Uri.parse(item.getString("uri")));
            byte[] bytes=retriever.getEmbeddedPicture();
            if(bytes!=null && bytes.length<8*1024*1024) {
              BitmapFactory.Options bounds=new BitmapFactory.Options(); bounds.inJustDecodeBounds=true;
              BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
              int factor=1; while(bounds.outWidth/factor>512 || bounds.outHeight/factor>512) factor*=2;
              BitmapFactory.Options options=new BitmapFactory.Options(); options.inSampleSize=factor;
              metadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,BitmapFactory.decodeByteArray(bytes,0,bytes.length,options));
            }
          } catch(Exception ignored) {} finally { try{retriever.release();}catch(Exception ignored){} }
        }
        session.setMetadata(metadata.build()); mp.start(); publish();
      });
      player.setOnCompletionListener(mp -> next());
      player.setOnErrorListener((mp,what,extra) -> { fail(); return true; });
      player.prepareAsync(); publish();
    } catch(Exception e) { fail(); }
  }
  void fail() {
    if(player!=null) {player.release();player=null;} prepared=false;
    session.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_ERROR,0,0).setErrorMessage("Cannot open this local file. Choose it again if access was removed.").build());
    if(foreground) {stopForeground(STOP_FOREGROUND_REMOVE);foreground=false;} audio.abandonAudioFocusRequest(focus);
    if(changed!=null) changed.run(); stopSelf();
  }
  boolean playing() { try{return player!=null && prepared && player.isPlaying();}catch(IllegalStateException e){return false;} }
  void resume() {
    if(player==null) {play(track);return;}
    if(!prepared) return;
    if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return;
    player.start();publish();
  }
  void pause() {
    if(playing()) player.pause(); publish();
    if(!resumeAfterFocus) audio.abandonAudioFocusRequest(focus);
    if(foreground) {stopForeground(STOP_FOREGROUND_DETACH);foreground=false;}
  }
  void next() { play(queue.isEmpty() ? (track+1)%library.size() : queue.remove(0)); }
  void enqueue(int index) { if(index>=0 && index<library.size()) {queue.add(index); if(changed!=null)changed.run();} }
  void stopPlayback() {
    if(player!=null) {player.release();player=null;} prepared=false;
    queue.clear(); audio.abandonAudioFocusRequest(focus); publish();
    session.setActive(false);
    session.setMetadata(null);
    stopForeground(STOP_FOREGROUND_REMOVE);foreground=false;
    ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(41);stopSelf();
  }
  // MediaStyle notifications attached to a media session are exempt from Android 13's
  // notification runtime permission. No general-purpose notifications are posted.
  @android.annotation.SuppressLint("NotificationPermission")
  void publish() {
    long actions=PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP;
    if(prepared) actions|=PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SEEK_TO | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS;
    long pos=prepared && player!=null ? player.getCurrentPosition() : 0;
    session.setPlaybackState(new PlaybackState.Builder().setActions(actions)
        .setState(playing()?PlaybackState.STATE_PLAYING : player==null?PlaybackState.STATE_STOPPED: prepared?PlaybackState.STATE_PAUSED:PlaybackState.STATE_BUFFERING,pos,playing()?1:0,SystemClock.elapsedRealtime()).build());
    if(playing()) {startForeground(41,notification());foreground=true;}
    else if(foreground) ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(41,notification());
    if(changed!=null)changed.run();
  }
  Notification notification() {
    PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    PendingIntent toggle=PendingIntent.getService(this,1,new Intent(this,LocalAudioService.class).setAction(TOGGLE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    PendingIntent stop=PendingIntent.getService(this,2,new Intent(this,LocalAudioService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    String name=track<library.size()?library.get(track).optString("name","Local audio"):"Local audio";
    return new Notification.Builder(this,"local-audio").setSmallIcon(R.drawable.ic_flow)
        .setContentTitle(name).setContentText("Flow TV • local audio").setContentIntent(open)
        .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0))
        .addAction(new Notification.Action.Builder(null,playing()?"Pause":"Play",toggle).build())
        .addAction(new Notification.Action.Builder(null,"Stop",stop).build())
        .setVisibility(Notification.VISIBILITY_PUBLIC).setOnlyAlertOnce(true).setOngoing(playing()).build();
  }
  @Override public void onDestroy() {
    if(player!=null)player.release();
    audio.abandonAudioFocusRequest(focus);session.release();changed=null;super.onDestroy();
  }
}
