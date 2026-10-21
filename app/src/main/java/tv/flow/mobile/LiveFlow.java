package tv.flow.mobile;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.media.*;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.json.*;

/** Default device-backed UI. No sample titles, fake results or playback measurements. */
final class LiveFlow {
  final MainActivity a;
  final FlowDesign d;
  TextView title, artist, sessionStatus, position;
  ImageView artwork;
  TextView artworkStatus;
  SeekBar seek;
  Button play, previous, next, rewind, forward;
  boolean seeking;
  boolean localView;
  Button queueButton;
  LiveFlow(MainActivity activity, FlowDesign design) { a = activity; d = design; }

  LinearLayout card(LinearLayout parent, int padding) {
    LinearLayout c = d.col();
    c.setPadding(d.u(padding), d.u(padding), d.u(padding), d.u(padding));
    c.setBackground(d.border(0xff102131, 0xff293e50, 18, 1));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.setMargins(d.u(4), d.u(6), d.u(4), d.u(8));
    parent.addView(c, lp);
    return c;
  }

  void home() {new CatalogueScreen(a,d).render();}

  static boolean isVideoProvider(String pkg) {
    return Arrays.asList("com.amazon.avod.thirdpartyclient", "com.amazon.amazonvideo.livingroom",
        "in.startv.hotstar", "in.airtel.tv", "com.netflix.mediaclient", "com.netflix.ninja",
        "com.disney.disneyplus", "com.disney.disneyplus.tv", "com.zee5", "com.graymatrix.did",
        "com.sonyliv", "com.sonyliv.tv", "com.jio.media.ondemand", "com.jio.media.stb.ondemand",
        "com.google.android.youtube", "com.google.android.youtube.tv").contains(pkg);
  }

  ArrayList<MainActivity.AppEntry> providers() {
    ArrayList<MainActivity.AppEntry> list = new ArrayList<>();
    for (MainActivity.AppEntry app : a.apps) if (isVideoProvider(app.pkg)) list.add(app);
    return list;
  }

  JSONObject recentData() {
    try { return new JSONObject(a.prefs.getString("recentApps", "{}")); }
    catch (JSONException e) { return new JSONObject(); }
  }
  ArrayList<MainActivity.AppEntry> recentApps() {
    JSONObject times = recentData();
    ArrayList<MainActivity.AppEntry> list = new ArrayList<>();
    for (MainActivity.AppEntry app : a.apps) if (times.optLong(app.pkg) > 0) list.add(app);
    list.sort((x, y) -> Long.compare(times.optLong(y.pkg), times.optLong(x.pkg)));
    return list;
  }

  void appRow(List<MainActivity.AppEntry> list, boolean recent) {
    LinearLayout row = d.row();
    for (MainActivity.AppEntry app : list.subList(0, Math.min(list.size(), 8))) {
      LinearLayout tile = d.col();
      tile.setGravity(Gravity.CENTER);
      tile.setBackground(d.border(0xff102131, 0xff293e50, 14, 1));
      tile.setPadding(d.u(9), d.u(8), d.u(9), d.u(8));
      ImageView icon = new ImageView(a);
      icon.setImageDrawable(app.icon);
      d.add(tile, icon, 34, 34);
      TextView label = d.bold(app.label, 13, FlowDesign.TEXT);
      label.setMaxLines(1); label.setEllipsize(TextUtils.TruncateAt.END);
      tile.addView(label);
      String caption = "Open app";
      if (recent) caption = android.text.format.DateUtils.getRelativeTimeSpanString(
          recentData().optLong(app.pkg), System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS).toString();
      TextView sub = d.label(caption, 10, FlowDesign.MUTED);
      sub.setMaxLines(1); sub.setEllipsize(TextUtils.TruncateAt.END);
      tile.addView(sub);
      d.focus(tile, () -> a.launchApp(app));
      tile.setTag(app.pkg);
      tile.setContentDescription("Open installed app " + app.label);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(d.u(151), d.u(104));
      lp.setMargins(d.u(3), d.u(3), d.u(3), d.u(3));
      row.addView(tile, lp);
    }
    a.horizontal(a.content, row);
  }

  void catalogue() {
    new CatalogueScreen(a,d).render();
  }
  long installTime(String pkg) {
    try { return a.getPackageManager().getPackageInfo(pkg, 0).firstInstallTime; }
    catch (PackageManager.NameNotFoundException e) { return 0; }
  }
  void store() {
    Intent i = a.getPackageManager().getLaunchIntentForPackage("com.android.vending");
    if (i == null) a.toast("Play Store is unavailable on this device"); else a.safeStart(i);
  }

  static String videoId(String input) {
    try {
      Uri uri = Uri.parse(input.trim());
      if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) return null;
      String host = uri.getHost();
      if (host == null) return null;
      host = host.toLowerCase(Locale.ROOT);
      String id = null;
      List<String> path = uri.getPathSegments();
      if (host.equals("youtu.be") && !path.isEmpty()) id = path.get(0);
      else if (Arrays.asList("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com").contains(host)) {
        if ("/watch".equals(uri.getPath())) id = uri.getQueryParameter("v");
        else if (path.size() == 2 && Arrays.asList("shorts", "live", "embed").contains(path.get(0))) id = path.get(1);
      }
      return id != null && id.matches("[A-Za-z0-9_-]{11}") ? id : null;
    } catch (Exception e) { return null; }
  }
  Intent youtubeIntent(String query, boolean music) {
    Uri uri = Uri.parse("https://www.youtube.com/results").buildUpon().appendQueryParameter("search_query", query.trim()).build();
    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    String[] packages = music
        ? new String[] {"com.google.android.apps.youtube.music", "com.google.android.youtube", "com.google.android.youtube.tv"}
        : new String[] {"com.google.android.youtube", "com.google.android.youtube.tv"};
    for (String pkg : packages) if (a.findApp(pkg) != null) {
      Intent candidate = new Intent(intent).setPackage(pkg);
      if (candidate.resolveActivity(a.getPackageManager()) != null) return candidate;
    }
    return intent;
  }
  void openYouTube(String query, boolean music) {
    if (query.trim().isEmpty()) { a.toast("Enter a song, artist or album first"); return; }
    a.safeStart(youtubeIntent(query, music));
  }
  void openVideo(String id) {
    a.safeStart(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + id)));
  }

  EditText field(LinearLayout parent, String hint) {
    EditText input = new EditText(a);
    input.setSingleLine(true); input.setTextColor(FlowDesign.TEXT); input.setHintTextColor(FlowDesign.MUTED);
    input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, d.u(14)); input.setHint(hint);
    input.setBackground(d.border(0xff101c28, FlowDesign.LINE, 9, 1));
    input.setPadding(d.u(10), d.u(9), d.u(10), d.u(9));
    parent.addView(input, new LinearLayout.LayoutParams(-1, d.u(43)));
    return input;
  }
  void search(boolean music) {
    if(music && a.localMode()){localSearch();return;}
    LinearLayout panel = d.panel(music ? "Search YouTube songs" : "Search apps & YouTube");
    EditText q = field(panel, "Song, artist, album or app");
    Button go = d.mint("⌕  Search on YouTube", () -> openYouTube(q.getText().toString(), music));
    panel.addView(go); go.setEnabled(false);
    d.note(panel, "Opens real results in YouTube or your browser. Flow has no YouTube search API key; no dummy results are listed.");
    LinearLayout results = d.col(); panel.addView(results);
    q.addTextChangedListener(a.watcher(() -> {
      go.setEnabled(!q.getText().toString().trim().isEmpty());
      results.removeAllViews();
      String text = q.getText().toString().trim().toLowerCase(Locale.ROOT);
      if (!text.isEmpty() && !music) for (MainActivity.AppEntry app : a.apps)
        if (app.label.toLowerCase(Locale.ROOT).contains(text)) results.addView(d.button("Open " + app.label, () -> a.launchApp(app)));
    }));
    keyboard(panel, q);
    d.note(panel, "YouTube keeps control of playback. Voice is not enabled.");
  }
  void keyboard(LinearLayout panel, EditText field) {
    d.gap(panel, 10);
    field.setShowSoftInputOnFocus(!a.tv());
    for (String line : new String[] {"qwertyuiop", "asdfghjkl", "zxcvbnm", "1234567890"}) {
      LinearLayout row = d.row();
      for (char ch : line.toCharArray()) {
        Button key = d.button(String.valueOf(ch), () -> field.getText().insert(Math.max(0, field.getSelectionStart()), String.valueOf(ch)));
        key.setPadding(0, 0, 0, 0);
        key.setMinWidth(0); key.setMinimumWidth(0);
        row.addView(key, new LinearLayout.LayoutParams(0, d.u(33), 1));
      }
      panel.addView(row);
    }
    LinearLayout row = d.row();
    row.addView(d.button("Space", () -> field.getText().insert(Math.max(0, field.getSelectionStart()), " ")), new LinearLayout.LayoutParams(0, d.u(34), 1));
    row.addView(d.button("⌫", () -> { int pos = field.getSelectionStart(); if (pos > 0) field.getText().delete(pos - 1, pos); }), new LinearLayout.LayoutParams(0, d.u(34), 1));
    row.addView(d.button("Clear", () -> field.setText("")), new LinearLayout.LayoutParams(0, d.u(34), 1));
    panel.addView(row);
  }

  JSONArray bookmarks() {
    try { return new JSONArray(a.prefs.getString("youtubeBookmarks", "[]")); }
    catch (JSONException e) { return new JSONArray(); }
  }
  void saved() {
    d.section(a.content, "My YouTube list", "Links saved on this device");
    a.content.addView(d.mint("+ Save a YouTube link", this::addBookmark));
    JSONArray entries = bookmarks();
    if (entries.length() == 0) {
      LinearLayout empty = card(a.content, 22);
      empty.addView(d.bold("Your list starts with a real link.", 24, FlowDesign.TEXT));
      empty.addView(d.label("Paste a YouTube video link and give it a name. Flow opens it in YouTube; metadata and streaming availability are not invented.", 14, FlowDesign.MUTED));
    }
    for (int n = 0; n < entries.length(); n++) {
      JSONObject entry = entries.optJSONObject(n);
      if (entry == null) continue;
      String id = entry.optString("id"), name = entry.optString("name", id);
      if (!id.matches("[A-Za-z0-9_-]{11}")) continue;
      LinearLayout row = d.row();
      row.addView(d.button("▶  " + name, () -> openVideo(id)), new LinearLayout.LayoutParams(0, d.u(52), 1));
      row.addView(d.button("Remove", () -> a.confirm("Remove link?", name, () -> {
        JSONArray current = bookmarks(), kept = new JSONArray();
        for (int j=0; j<current.length(); j++) if (current.optJSONObject(j) != null && !id.equals(current.optJSONObject(j).optString("id"))) kept.put(current.optJSONObject(j));
        a.prefs.edit().putString("youtubeBookmarks", kept.toString()).apply(); a.build();
      })));
      a.content.addView(row);
    }
  }
  void addBookmark() {
    LinearLayout p = d.panel("Save a YouTube link");
    EditText url = field(p, "Paste https://youtube.com/watch?v=…");
    EditText name = field(p, "Your name for this video");
    p.addView(d.mint("Save link", () -> {
      String id = videoId(url.getText().toString());
      if (id == null) { a.toast("Enter a valid YouTube video URL"); return; }
      JSONArray entries = bookmarks();
      for (int n=0; n<entries.length(); n++) if (entries.optJSONObject(n) != null && id.equals(entries.optJSONObject(n).optString("id"))) { a.toast("Link already saved"); return; }
      if (entries.length() >= 50) { a.toast("Your list is full (50 links)"); return; }
      try {
        String label = name.getText().toString().trim();
        entries.put(new JSONObject().put("id", id).put("name", label.isEmpty() ? "YouTube video " + id : label));
        a.prefs.edit().putString("youtubeBookmarks", entries.toString()).apply();
        a.navigate("My list");
      } catch (JSONException e) { a.toast("Unable to save link"); }
    }));
    d.note(p, "Stored locally. This is a saved link, not a downloaded video or a YouTube queue.");
  }

  TextView dot(String text, int size, int color) {
    TextView label = d.label(text, size, color); label.setTypeface(FlowDesign.dots); return label;
  }
  void audio() {
    localView=a.localMode();
    LinearLayout sources=d.row();
    for(String source : new String[]{"youtube","local"}) {
      Button select=d.button(source.equals("youtube")?"YouTube":"Local audio", () -> {
        if(!source.equals("local") && a.localAudio!=null) a.localAudio.pause();
        a.prefs.edit().putString("audioSource",source).apply();a.navigate("Audio");
      });
      select.setTypeface(FlowDesign.dots);
      select.setTextColor(source.equals(localView?"local":"youtube")?FlowDesign.RED:FlowDesign.TEXT);
      sources.addView(select,new LinearLayout.LayoutParams(0,d.u(35),1));
    }
    a.content.addView(sources);
    int ivory = 0xffeeeae0;
    LinearLayout deck = card(a.content, 17);
    deck.setBackground(d.border(0xff0b0d0e, 0xff333536, 20, 1));
    LinearLayout head = d.row();
    head.addView(dot(localView?"LOCAL AUDIO":"YOUTUBE", 15, FlowDesign.RED), new LinearLayout.LayoutParams(0, -2, 1));
    head.addView(dot("MUSIC / 01", 13, ivory));
    deck.addView(head);
    LinearLayout main = a.landscape() ? d.row() : d.col();
    artwork = new ImageView(a);
    artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
    FrameLayout artbox=new FrameLayout(a);
    artbox.setBackground(d.border(0xff111415, 0xff3d4142, 14, 1));
    artbox.setClipToOutline(true);
    artbox.addView(artwork,new FrameLayout.LayoutParams(-1,-1));
    artworkStatus=dot("• • •\nNO ARTWORK",15,0xffa8aaa8);
    artworkStatus.setGravity(Gravity.CENTER);
    artbox.addView(artworkStatus,new FrameLayout.LayoutParams(-1,-1));
    main.addView(artbox, new LinearLayout.LayoutParams(a.landscape() ? d.u(170) : -1, d.u(a.landscape() ? 150 : 145)));
    LinearLayout meta = d.col();
    meta.setPadding(d.u(13), d.u(8), 0, 0);
    title = dot("YOUR MUSIC.\nON YOUTUBE.", 29, ivory);
    title.setMaxLines(2); title.setEllipsize(TextUtils.TruncateAt.END);
    artist = d.label("Playback stays in the official YouTube app.", 13, 0xffb8bbb8);
    meta.addView(title); meta.addView(artist);
    sessionStatus = dot("NOT CONNECTED", 12, FlowDesign.RED); meta.addView(sessionStatus);
    Button open = d.button(localView ? "⌕  Library / Search" : "▶  Show video in YouTube", () -> { if(localView) localSearch();else openYouTubeApp(); });
    open.setTextColor(ivory); open.setTypeface(FlowDesign.dots); meta.addView(open);
    main.addView(meta, a.landscape() ? new LinearLayout.LayoutParams(0, -2, 1) : new LinearLayout.LayoutParams(-1, -2));
    deck.addView(main);
    seek = new SeekBar(a);
    seek.setProgressTintList(android.content.res.ColorStateList.valueOf(FlowDesign.RED));
    seek.setThumbTintList(android.content.res.ColorStateList.valueOf(ivory));
    seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      public void onStartTrackingTouch(SeekBar bar) { seeking = true; }
      public void onStopTrackingTouch(SeekBar bar) { a.youtube.seek(bar.getProgress()); seeking = false; }
      public void onProgressChanged(SeekBar bar, int pos, boolean user) { if (user && !seeking) a.youtube.seek(pos); }
    });
    deck.addView(seek, new LinearLayout.LayoutParams(-1, d.u(29)));
    position = dot("— / —", 13, ivory); deck.addView(position);
    LinearLayout controls = d.row();
    previous = d.audioControl(controls, "|◀\nPrevious", () -> a.youtube.act(PlaybackState.ACTION_SKIP_TO_PREVIOUS), false);
    rewind = d.audioControl(controls, "−10s", () -> a.youtube.seek(a.youtube.position()-10000), false);
    play = d.audioControl(controls, "▶\nPlay", () -> {
      if(localView && a.localAudio!=null && a.localAudio.player==null) {playLocal(a.localAudio.track);return;}
      PlaybackState s = a.youtube.state();
      long action = s != null && s.getState() == PlaybackState.STATE_PLAYING ? PlaybackState.ACTION_PAUSE : PlaybackState.ACTION_PLAY;
      a.youtube.act(a.youtube.supports(action) ? action : PlaybackState.ACTION_PLAY_PAUSE);
    }, true);
    forward = d.audioControl(controls, "+10s", () -> a.youtube.seek(a.youtube.position()+10000), false);
    next = d.audioControl(controls, "▶|\nNext", () -> a.youtube.act(PlaybackState.ACTION_SKIP_TO_NEXT), false);
    for (Button b : new Button[] {previous, rewind, play, forward, next}) b.setTypeface(FlowDesign.dots);
    deck.addView(controls);
    LinearLayout widgets = a.landscape() ? d.row() : d.col();
    Button access = d.button(localView?"+ Choose audio files":"YouTube playback limits", () -> {if(localView)pickAudio();else mediaAccess();});
    access.setTypeface(FlowDesign.dots); widgets.addView(access);
    widgets.addView(d.button(localView?"Display / Black screen":"Display options · unverified", this::playbackLimits));
    if(localView) { queueButton=d.button("Queue (0)",this::localQueue);widgets.addView(queueButton); }
    else if(a.youtube.active()!=null && a.youtube.active().getQueue()!=null && !a.youtube.active().getQueue().isEmpty())
      widgets.addView(d.button("Source queue",this::sourceQueue));
    deck.addView(widgets);
    LinearLayout volume=d.row();
    volume.addView(dot("VOL",12,ivory));
    AudioManager manager=(AudioManager)a.getSystemService(Context.AUDIO_SERVICE);
    SeekBar slider=new SeekBar(a);
    slider.setMax(manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
    slider.setProgress(manager.getStreamVolume(AudioManager.STREAM_MUSIC));
    slider.setProgressTintList(android.content.res.ColorStateList.valueOf(FlowDesign.RED));
    slider.setThumbTintList(android.content.res.ColorStateList.valueOf(ivory));
    slider.setEnabled(!manager.isVolumeFixed());
    slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onStartTrackingTouch(SeekBar bar){}
      public void onStopTrackingTouch(SeekBar bar){}
      public void onProgressChanged(SeekBar bar,int value,boolean user){if(user)try{manager.setStreamVolume(AudioManager.STREAM_MUSIC,value,0);}catch(SecurityException e){a.toast("Use device volume buttons");}}
    });
    volume.addView(slider,new LinearLayout.LayoutParams(0,d.u(30),1));deck.addView(volume);
    d.note(deck, localView ? "Local files play through Flow's media service. Display-off behavior still needs testing on your TV. Bundled tracks are original sample audio." : a.tv()?"YouTube background play is documented for mobile apps only. Use YouTube on TV for video; use Local audio for Flow's listening controls. Picture-off is a separate unverified vendor feature.":"YouTube plays in its official app. Flow does not request notification access or read other apps’ media sessions. Use Local audio for Flow’s dot-matrix controls. YouTube has its own privacy practices.");
    updateMedia();
  }

  void openYouTubeApp() {
    for (MainActivity.AppEntry app : a.apps) if (YouTubeMedia.isYouTube(app.pkg)) { a.launchApp(app); return; }
    a.safeStart(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")));
  }
  void playbackLimits() {
    if(a.localMode()) {
      LinearLayout p=d.panel("Display options · local audio");
      d.note(p,"On a phone, use the physical power button to turn the display off. Local audio uses a foreground media service and partial CPU wake lock; test this on your device. Wake with the power button or your normal unlock method.");
      d.note(p,"realme RMV2004: picture-off support and remote wake behavior are not verified. Flow has no documented vendor picture-off integration. Standby may stop playback; it is not the same as picture off.");
      p.addView(d.button("Open device display settings",() -> a.openSettings(Settings.ACTION_DISPLAY_SETTINGS)));
      p.addView(d.button("Black screen · display stays on",this::blackScreen));
      d.note(p,"Black pixels do not turn off the panel or backlight. Tap, press a remote/keyboard key, or Back to leave the black screen. A truly off display has no widgets.");
      return;
    }
    LinearLayout p = d.panel("YouTube playback");
    p.addView(dot("VIDEO / DISPLAY", 21, FlowDesign.TEXT));
    d.note(p, "Video hide: unavailable in Flow. YouTube video must stay visible in its supported player.");
    if(a.tv()) d.note(p,"YouTube's supported-devices documentation limits background and offline play to mobile apps. Flow therefore does not offer background YouTube controls on Android TV. A TV's own picture-off mode while YouTube stays foreground is a different, unverified vendor function.");
    else d.note(p,"Official listening modes: in YouTube for Android, an eligible Premium account can use video Settings → On-the-go. In YouTube Music, eligible music memberships can select Song instead of Video. Flow cannot switch these official-app modes for you.");
    d.note(p, "Screen-off audio: managed by YouTube. Background playback generally requires an eligible YouTube Premium account and supported content, device and app settings. Flow cannot enable or promise it.");
    p.addView(d.button("Official YouTube background-play help",() -> a.safeStart(new Intent(Intent.ACTION_VIEW,Uri.parse("https://support.google.com/youtube/answer/6308116")))));
    p.addView(d.button("Show video / Open YouTube", this::openYouTubeApp));
    d.note(p,"Phone and Android TV support differ. Flow cannot detect your YouTube membership or guarantee background audio. No real YouTube screen-off playback has been verified.");
    d.note(p, "There is no fake hide-video or screen-off switch. A black Flow screen would not power off the display.");
  }
  void mediaAccess() {
    LinearLayout p=d.panel("YouTube playback limits");
    d.note(p,"Notification access has been removed from this build. Flow cannot read or control YouTube playback, artwork, timing or queues. No sensitive cross-app access is requested.");
    d.note(p,"Use the official YouTube app for video and any supported listening modes. Background audio and display-off playback depend on YouTube membership, content, device and settings; Flow cannot enable them.");
    d.note(p,"Flow’s dot-matrix controls work with local audio. YouTube and other apps have their own privacy practices.");
    p.addView(d.mint("Open YouTube",this::openYouTubeApp));
  }
  void pickAudio() {
    Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*");
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
    try{a.startActivityForResult(intent,70);}catch(ActivityNotFoundException e){a.toast("No document picker is available");}
  }
  void importAudio(List<Uri> uris,int flags) {
    a.toast("Reading selected audio files…");
    new Thread(() -> {
      JSONArray saved;
      try{saved=new JSONArray(a.prefs.getString("localAudio","[]"));}catch(JSONException e){saved=new JSONArray();}
      int count=0;
      for(Uri uri:uris.subList(0,Math.min(uris.size(),20))) {
        if(!"content".equals(uri.getScheme()) || saved.length()>=100)continue;
        boolean exists=false;for(int n=0;n<saved.length();n++)if(saved.optJSONObject(n)!=null && uri.toString().equals(saved.optJSONObject(n).optString("uri")))exists=true;
        if(exists)continue;
        try {
          if((flags & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)!=0)
            a.getContentResolver().takePersistableUriPermission(uri,flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
          String name="Selected audio";
          try(android.database.Cursor cursor=a.getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)) {
            if(cursor!=null && cursor.moveToFirst())name=cursor.getString(0);
          }
          MediaMetadataRetriever retriever=new MediaMetadataRetriever();
          try {
            retriever.setDataSource(a,uri);
            String title=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            saved.put(new JSONObject().put("uri",uri.toString()).put("name",title==null?name:title)
                .put("artist",artist==null?"Artist unavailable":artist).put("album",album==null?"":album));count++;
          }finally{retriever.release();}
        }catch(Exception ignored){}
      }
      a.prefs.edit().putString("localAudio",saved.toString()).apply();
      final int imported=count;
      a.handler.post(() -> {
        if(a.isFinishing() || a.isDestroyed())return;
        if(a.localAudio!=null)a.localAudio.refreshLibrary();
        a.toast(imported+" audio files added");
        a.prefs.edit().putString("audioSource","local").apply();a.navigate("Audio");
      });
    },"Flow-audio-import").start();
  }
  void playLocal(int index) {
    Intent intent=new Intent(a,LocalAudioService.class).setAction(LocalAudioService.PLAY).putExtra("track",index);
    a.startForegroundService(intent);
  }
  void localSearch() {
    LinearLayout p=d.panel("Search local songs");
    EditText query=field(p,"Song, artist or album");
    p.addView(d.button("+ Choose audio files",this::pickAudio));
    d.note(p,"Real local library • Play now changes playback • Add to queue plays after this track");
    LinearLayout results=d.col();p.addView(results);
    Runnable refresh=() -> {
      results.removeAllViews();
      if(a.localAudio==null){d.note(results,"Connecting local player…");return;}
      a.localAudio.refreshLibrary();
      String q=query.getText().toString().trim().toLowerCase(Locale.ROOT);
      int found=0;
      for(int n=0;n<a.localAudio.library.size();n++) {
        JSONObject song=a.localAudio.library.get(n);
        if(!(song.optString("name")+" "+song.optString("artist")+" "+song.optString("album")).toLowerCase(Locale.ROOT).contains(q))continue;
        final int index=n;found++;
        results.addView(d.bold(song.optString("name"),15,FlowDesign.TEXT));
        results.addView(d.label(song.optString("artist")+" • "+song.optString("album"),11,FlowDesign.MUTED));
        LinearLayout actions=d.row();
        actions.addView(d.button("▶ Play now",() -> {playLocal(index);a.closeDrawer();}));
        actions.addView(d.button("+ Add to queue",() -> {a.localAudio.enqueue(index);a.toast("Added to local queue");}));results.addView(actions);
      }
      if(found==0)d.note(results,"No matching local audio");
    };
    query.addTextChangedListener(a.watcher(refresh));refresh.run();keyboard(p,query);
  }
  void localQueue() {
    LinearLayout p=d.panel("Local playback queue");
    if(a.localAudio==null || a.localAudio.queue.isEmpty()){d.note(p,"Queue is empty. Add a local song from Search songs.");return;}
    ArrayList<Integer> queue=new ArrayList<>(a.localAudio.queue);
    for(int n=0;n<queue.size();n++) {
      int index=queue.get(n),position=n;
      if(index>=a.localAudio.library.size())continue;
      JSONObject song=a.localAudio.library.get(index);
      p.addView(d.button("▶ "+song.optString("name"),() -> {a.localAudio.queue.remove(position);playLocal(index);a.closeDrawer();}));
      p.addView(d.button("Remove from queue",() -> {a.localAudio.queue.remove(position);localQueue();}));
    }
  }
  void sourceQueue() {
    LinearLayout p=d.panel("YouTube source queue");
    android.media.session.MediaController controller=a.youtube.active();
    if(controller==null || controller.getQueue()==null){d.note(p,"Queue is not exposed by YouTube.");return;}
    for(android.media.session.MediaSession.QueueItem item:controller.getQueue()) {
      String name=item.getDescription().getTitle()==null?"Title unavailable":item.getDescription().getTitle().toString();
      Button button=d.button(name,() -> controller.getTransportControls().skipToQueueItem(item.getQueueId()));
      button.setEnabled(a.youtube.supports(PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM));p.addView(button);
    }
    d.note(p,"Queue supplied by YouTube, separate from search results. Flow cannot add results to a YouTube queue without a supported integration.");
  }
  void blackScreen() {
    if(!a.localMode()){a.toast("Black screen for YouTube is not verified");return;}
    a.closeDrawer();
    View black=new View(a);black.setBackgroundColor(android.graphics.Color.BLACK);
    black.setDefaultFocusHighlightEnabled(false);
    black.setFocusableInTouchMode(true);
    Runnable exit=() -> {if(black.getParent()!=null)((ViewGroup)black.getParent()).removeView(black);a.blackOverlay=null;};
    black.setOnClickListener(v -> exit.run());
    black.setOnKeyListener((v,key,event) -> {if(event.getAction()==android.view.KeyEvent.ACTION_UP)exit.run();return true;});
    ((ViewGroup)a.findViewById(android.R.id.content)).addView(black,new ViewGroup.LayoutParams(-1,-1));
    a.blackOverlay=black;black.requestFocus();
  }
  void updateMedia() {
    if (title == null || !a.page.equals("Audio") || a.demo() || localView!=a.localMode()) return;
    YouTubeMedia media = a.youtube;
    boolean connected = media.active() != null;
    title.setText(media.text(MediaMetadata.METADATA_KEY_TITLE, localView?"YOUR LOCAL\nLIBRARY.":"YOUR MUSIC.\nON YOUTUBE."));
    artist.setText(media.text(MediaMetadata.METADATA_KEY_ARTIST, localView?"Choose a file or a bundled sample.":connected ? "Artist/channel not exposed by YouTube" : "Playback stays in the official YouTube app."));
    PlaybackState state = media.state();
    boolean playing = state != null && state.getState() == PlaybackState.STATE_PLAYING;
    sessionStatus.setText(localView ? (playing?"NOW PLAYING • LOCAL":state!=null && state.getState()==PlaybackState.STATE_ERROR?"FILE UNAVAILABLE":state!=null && state.getState()==PlaybackState.STATE_BUFFERING?"LOADING LOCAL AUDIO":"LOCAL AUDIO • READY") : "YOUTUBE: USE OFFICIAL APP");
    if(queueButton!=null && a.localAudio!=null)queueButton.setText("Queue ("+a.localAudio.queue.size()+")");
    Bitmap art = media.artwork();
    if (art != null) {artwork.setImageBitmap(art);artwork.setContentDescription("Source artwork");artworkStatus.setVisibility(View.GONE);}
    else {artwork.setImageDrawable(null);artwork.setContentDescription("Source artwork unavailable");artworkStatus.setVisibility(View.VISIBLE);}
    long duration = media.duration();
    if (!seeking) { seek.setMax((int) Math.min(Integer.MAX_VALUE, duration)); seek.setProgress((int) Math.min(Integer.MAX_VALUE, media.position())); }
    seek.setEnabled(duration > 0 && media.supports(PlaybackState.ACTION_SEEK_TO));
    seek.setVisibility(seek.isEnabled()?View.VISIBLE:View.GONE);
    position.setText(connected ? a.time((int)media.position()) + " / " + (duration > 0 ? a.time((int)duration) : "—") : "— / —");
    play.setText(playing ? "Ⅱ\nPause" : "▶\nPlay");
    enable(previous, media.supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS));
    enable(next, media.supports(PlaybackState.ACTION_SKIP_TO_NEXT));
    enable(rewind, seek.isEnabled()); enable(forward, seek.isEnabled());
    enable(play, media.supports(playing ? PlaybackState.ACTION_PAUSE : PlaybackState.ACTION_PLAY) || media.supports(PlaybackState.ACTION_PLAY_PAUSE));
  }
  void enable(View view, boolean enabled) { view.setEnabled(enabled); view.setVisibility(enabled?View.VISIBLE:View.GONE); }
}
