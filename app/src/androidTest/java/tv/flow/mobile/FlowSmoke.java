package tv.flow.mobile;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Dependency-free instrumentation: actual device APIs, MediaPlayer and UI flows. */
public class FlowSmoke extends Instrumentation {
  MainActivity app;
  Bundle options;
  int passed = 0;
  StringBuilder report = new StringBuilder();

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    options = b;
    start();
  }

  void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  void test(String name, Runnable r) {
    final Throwable[] failure = new Throwable[1];
    runOnMainSync(
        () -> {
          try {
            r.run();
          } catch (Throwable e) {
            failure[0] = e;
          }
        });
    if (failure[0] != null) throw new AssertionError(name, failure[0]);
    passed++;
    report.append("PASS ").append(name).append('\n');
    Bundle status = new Bundle();
    status.putString("stream", "PASS " + name + "\n");
    sendStatus(0, status);
  }

  Button find(View view, String needle) {
    if (view instanceof Button && ((Button) view).getText().toString().contains(needle))
      return (Button) view;
    if (view instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) view;
      for (int i = 0; i < g.getChildCount(); i++) {
        Button b = find(g.getChildAt(i), needle);
        if (b != null) return b;
      }
    }
    return null;
  }

  String firstDescription(View view, String part) {
    if (view.getContentDescription() != null
        && view.getContentDescription().toString().contains(part))
      return view.getContentDescription().toString();
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int n = 0; n < group.getChildCount(); n++) {
        String result = firstDescription(group.getChildAt(n), part);
        if (result != null) return result;
      }
    }
    return null;
  }

  EditText editText(View view) {
    if (view instanceof EditText) return (EditText) view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int n = 0; n < group.getChildCount(); n++) {
        EditText result = editText(group.getChildAt(n));
        if (result != null) return result;
      }
    }
    return null;
  }

  boolean contains(View view, String needle) {
    if (view instanceof TextView && ((TextView) view).getText().toString().contains(needle))
      return true;
    if (view instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) view;
      for (int i = 0; i < g.getChildCount(); i++)
        if (contains(g.getChildAt(i), needle)) return true;
    }
    return false;
  }

  void click(String label) {
    Button b = find(app.drawer != null ? app.drawer : app.content, label);
    check(b != null, "Missing button " + label);
    b.performClick();
  }

  void screenshot(String name, Runnable prepare) throws Exception {
    runOnMainSync(prepare);
    waitForIdleSync();
    SystemClock.sleep(350);
    android.graphics.Bitmap bitmap = getUiAutomation().takeScreenshot();
    check(bitmap != null, "Screenshot failed: " + name);
    java.io.File folder = new java.io.File(getTargetContext().getFilesDir(), "visuals");
    folder.mkdirs();
    try (java.io.FileOutputStream output =
        new java.io.FileOutputStream(new java.io.File(folder, name + ".png"))) {
      bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output);
    }
    bitmap.recycle();
    Bundle status = new Bundle();
    status.putString("stream", "CAPTURE " + name + "\n");
    sendStatus(0, status);
  }

  void captureScreens() throws Exception {
    runOnMainSync(
        () -> {
          app.remote = false;
          if (app.player != null && app.player.isPlaying()) app.player.pause();
        });
    screenshot("01-home", () -> app.navigate("Home"));
    screenshot("02-new", () -> app.navigate("New"));
    screenshot("03-apps", () -> app.navigate("Apps"));
    screenshot("04-kids", () -> app.navigate("Kids"));
    screenshot(
        "05-quick-settings",
        () -> {
          app.navigate("Home");
          app.settings();
        });
    screenshot(
        "06-wifi",
        () -> {
          app.navigate("Home");
          app.detail("Wi-Fi");
        });
    screenshot(
        "07-sound-display",
        () -> {
          app.navigate("Home");
          app.settingsSection("Sound");
        });
    screenshot(
        "08-privacy",
        () -> {
          app.navigate("Home");
          app.settingsSection("Privacy");
        });
    screenshot("09-dashboard", () -> app.navigate("Dashboard"));
    screenshot(
        "10-ports",
        () -> {
          app.navigate("Dashboard");
          app.detail("Ports & Inputs");
        });
    screenshot("11-audio", () -> app.navigate("Audio"));
    screenshot(
        "12-song-search",
        () -> {
          app.navigate("Audio");
          app.searchSongs();
        });
    screenshot("13-movies", () -> app.navigate("Movies"));
    screenshot("14-shows", () -> app.navigate("Shows"));
    screenshot("15-my-list", () -> app.navigate("My list"));
    screenshot(
        "16-title-details",
        () -> {
          app.navigate("Movies");
          app.titleDetails(app.catalogue[9]);
        });
    runOnMainSync(
        () -> {
          app.closeDrawer();
          app.preview = false;
          app.setRequestedOrientation(
              android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        });
    long deadline = SystemClock.uptimeMillis() + 15000;
    while (app.landscape() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100);
    check(!app.landscape(), "Portrait transition failed");
    screenshot("17-portrait-home", () -> app.navigate("Home"));
    screenshot("18-portrait-audio", () -> app.navigate("Audio"));
    runOnMainSync(
        () -> {
          app.preview = true;
          app.setRequestedOrientation(
              android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        });
    deadline = SystemClock.uptimeMillis() + 15000;
    while (!app.landscape() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100);
    check(app.landscape(), "Landscape transition failed");
  }

  void runLive(Intent intent) throws Exception {
    runOnMainSync(() -> {
      app.prefs.edit().remove("designDemo").remove("recentApps").remove("youtubeBookmarks").putString("audioSource","youtube").commit();
      app.finish();
    });
    waitForIdleSync();
    app = (MainActivity) startActivitySync(intent);
    waitForIdleSync();
    test("default device mode has no fabricated catalogue or sample playback", () -> {
      check(!app.demo(), "Default mode must be device-backed");
      for (String section : app.sections) {
        app.navigate(section);
        check(!contains(app.content, "The Lost Horizon") && !contains(app.content, "Monsoon Letters"), "Sample title exposed in " + section);
      }
      app.navigate("Home");
      check(contains(app.content, "Favourite apps"), "Real app row absent");
      check(contains(app.content, "Film discovery"), "Provider discovery absent");
      check(app.player == null, "Sample player created in production mode");
    });
    test("content tabs keep app shortcuts distinct from catalogue records and never invent films", () -> {
      for(String page:new String[]{"Movies","Shows","New","Kids"}){
        app.navigate(page);
        check(app.sharedContent.forPage(page).isEmpty(),"Unexpected shared TV feed on phone emulator");
        check(contains(app.content,"Catalogue networking is turned off"),"Unavailable public source not explained in "+page);
        check(!contains(app.content,"Recently installed apps"),"Installed-app cards leaked into content tab");
        check(!contains(app.content,"Find on YouTube"),"Generic trailer search substituted for a catalogue");
      }
      app.settings();check(find(app.drawer,"Design preview")==null,"Sample mode is still exposed in settings");app.closeDrawer();
    });
    test("published programme types and source new flags control each tab", () -> {
      SharedTvContent.Item movie=new SharedTvContent.Item();movie.pkg="fixture.source";movie.name="Movie test fixture";movie.type=android.media.tv.TvContract.PreviewPrograms.TYPE_MOVIE;
      SharedTvContent.Item episode=new SharedTvContent.Item();episode.pkg="fixture.source";episode.name="Episode test fixture";episode.type=android.media.tv.TvContract.PreviewPrograms.TYPE_TV_EPISODE;episode.watchNext=true;episode.watchType=android.media.tv.TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_NEW;
      SharedTvContent.Item kids=new SharedTvContent.Item();kids.pkg="fixture.source";kids.name="Family test fixture";kids.type=android.media.tv.TvContract.PreviewPrograms.TYPE_TV_SERIES;kids.kids=true;
      app.sharedContent.items.add(movie);app.sharedContent.items.add(episode);app.sharedContent.items.add(kids);
      check(app.sharedContent.forPage("Movies").size()==1 && app.sharedContent.forPage("Movies").get(0)==movie,"Movies misclassified");
      check(app.sharedContent.forPage("Shows").size()==2,"TV programmes misclassified");
      check(app.sharedContent.forPage("New").size()==1 && app.sharedContent.forPage("New").get(0)==episode,"New must use source's explicit flag");
      check(app.sharedContent.forPage("Kids").size()==1 && app.sharedContent.forPage("Kids").get(0)==kids,"Kids inferred without family metadata");
      app.sharedContent.items.clear();
    });
    test("shared-title watchlist persists real metadata separately from YouTube links", () -> {
      SharedTvContent.Item item=new SharedTvContent.Item();item.pkg="fixture.source";item.name="Saved metadata fixture";item.type=android.media.tv.TvContract.PreviewPrograms.TYPE_MOVIE;item.source="Instrumentation fixture";item.poster="";item.link="";item.description="Test-only record, not live provider data";
      app.navigate("My list");new SharedContentScreen(app,app.design).details(item);click("+ My list");
      check(app.sharedContent.isSaved(item),"Shared title not saved");app.onBackPressed();
      check(contains(app.content,"Saved metadata fixture"),"Saved programme not rendered immediately");
      new SharedContentScreen(app,app.design).details(item);click("Remove from My list");app.onBackPressed();
      check(!app.sharedContent.isSaved(item) && !contains(app.content,"Saved metadata fixture"),"Shared title removal did not update the list");
      app.prefs.edit().remove("sharedTitleList").apply();app.navigate("Home");
    });
    test("installed app discovery uses actual packages and icons", () -> {
      app.scanApps();
      check(app.apps.size() >= 2, "Missing launchable apps");
      for (MainActivity.AppEntry entry : app.apps) {
        check(entry.icon != null && entry.intent.getComponent() != null, "App lacks real icon/launch component");
        check(!entry.pkg.equals(app.getPackageName()), "Flow listed itself");
        try { app.getPackageManager().getPackageInfo(entry.pkg, 0); }
        catch (android.content.pm.PackageManager.NameNotFoundException e) { throw new AssertionError("Fabricated app"); }
      }
      app.navigate("Apps");
      check(contains(app.content, "Actual installed apps"), "Installed app view missing");
    });
    test("real YouTube search URI encoding and remote keyboard", () -> {
      android.content.Intent query = app.live.youtubeIntent("songs & jazz + தமிழ்", false);
      check("https".equals(query.getData().getScheme()), "Search must use HTTPS");
      check("songs & jazz + தமிழ்".equals(query.getData().getQueryParameter("search_query")), "Query corrupted");
      app.navigate("Audio"); app.live.search(true);
      EditText input = editText(app.drawer);
      check(!find(app.drawer, "Search on YouTube").isEnabled(), "Empty query enabled");
      click("q");
      check(input.getText().toString().equals("q"), "Remote keyboard did not enter text");
      input.setText("artist album");
      check(find(app.drawer, "Search on YouTube").isEnabled(), "Real search disabled");
      check(!contains(app.drawer, "Mint Horizon"), "Dummy result exposed");
      app.closeDrawer();
    });
    test("YouTube URL validation rejects other hosts and unsafe schemes", () -> {
      check("dQw4w9WgXcQ".equals(LiveFlow.videoId("https://youtu.be/dQw4w9WgXcQ?si=a")), "Short URL rejected");
      check("dQw4w9WgXcQ".equals(LiveFlow.videoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ")), "Watch URL rejected");
      check(LiveFlow.videoId("https://youtube.com.evil.example/watch?v=dQw4w9WgXcQ") == null, "Spoofed host accepted");
      check(LiveFlow.videoId("javascript:alert(1)") == null, "Unsafe scheme accepted");
      check(LiveFlow.videoId("https://youtube.com/watch?v=bad") == null, "Invalid video accepted");
    });
    test("saved real YouTube links persist without fabricated metadata", () -> {
      app.navigate("My list"); app.live.addBookmark();
      ArrayList<EditText> fields = new ArrayList<>(); collectFields(app.drawer, fields);
      check(fields.size() == 2, "Link inputs missing");
      fields.get(0).setText("https://youtu.be/dQw4w9WgXcQ");
      fields.get(1).setText("My saved music link"); click("Save link");
      check(app.live.bookmarks().length() == 1, "Link not saved");
      app.build();
      new SharedContentScreen(app,app.design).youtubeLinks();
      check(contains(app.drawer, "My saved music link"), "Link not rendered after recreation");app.closeDrawer();
      check(app.prefs.getString("youtubeBookmarks", "").contains("dQw4w9WgXcQ"), "Link not persisted");
    });
    test("YouTube controls are honest without media-session access", () -> {
      app.navigate("Audio");
      check(app.youtube.active() == null, "Unexpected external media session");
      check(!app.live.play.isEnabled() && !app.live.seek.isEnabled(), "Fake playback controls enabled");
      check(!app.live.next.isEnabled(), "Fake next control enabled");
      check(contains(app.content, "YOUTUBE: USE OFFICIAL APP"), "Missing truthful official-app state");
      app.live.playbackLimits();
      check(contains(app.drawer, "Video hide: unavailable"), "Hidden-video limit not labelled");
      check(contains(app.drawer, "Premium"), "Screen-off account requirements missing");
      app.closeDrawer();
      check(!YouTubeMedia.isYouTube(app.getPackageName()), "Other apps accepted as YouTube");
      check(YouTubeMedia.isYouTube("com.google.android.apps.youtube.music"), "Official Music package rejected");
    });
    test("minimal glass dashboard uses live device values", () -> {
      app.navigate("Dashboard");
      check(contains(app.content, "MINIMAL GLASS"), "Minimal Glass theme absent");
      check(contains(app.content, android.os.Build.HARDWARE), "Actual CPU hardware missing");
      check(!contains(app.content, "SDM665"), "Reference CPU leaked");
      app.design.refreshMetrics();
      check(app.metricViews.size() >= 5, "Live metric widgets missing");
    });
    test("YouTube limits offer official app without a sensitive access prompt", () -> {
      app.navigate("Audio"); app.live.mediaAccess();
      check(contains(app.drawer, "Notification access has been removed"), "Removed access not explained");
      check(find(app.drawer, "Open Android access settings") == null, "Sensitive grant action remains");
      check(find(app.drawer, "Open YouTube") != null, "Official app action missing");
      app.closeDrawer();
    });
    test("real app launch records local recently-opened item", () -> {
      MainActivity.AppEntry settings = app.findApp("com.android.settings");
      check(settings != null, "Emulator Settings app missing");
      app.launchApp(settings);
      check(app.live.recentData().optLong(settings.pkg) > 0, "Launch timestamp missing");
    });
    waitForIdleSync();
    runOnMainSync(() -> app.finish());
    app = (MainActivity) startActivitySync(intent); waitForIdleSync();
    test("recent apps survive returning from a real app launch", () -> {
      app.navigate("Home");
      check(contains(app.content, "Recently opened"), "Local recent row absent");
      check(!app.live.recentApps().isEmpty(), "Launch history not restored");
    });
    test("local source starts a real foreground audio service", () -> {
      app.prefs.edit().putString("audioSource","local").apply();app.navigate("Audio");
      check(app.localAudio!=null,"Local service did not bind");app.live.playLocal(0);
    });
    awaitLocal(0);
    test("installed package exposes no notification listener or sensitive collection permissions", () -> {
      try {
        android.content.pm.PackageInfo info=app.getPackageManager().getPackageInfo(app.getPackageName(),android.content.pm.PackageManager.GET_SERVICES | android.content.pm.PackageManager.GET_PERMISSIONS);
        if(info.services!=null)for(android.content.pm.ServiceInfo service:info.services)
          check(!"android.permission.BIND_NOTIFICATION_LISTENER_SERVICE".equals(service.permission) && !"android.permission.BIND_ACCESSIBILITY_SERVICE".equals(service.permission),"Sensitive service still exposed");
        if(info.requestedPermissions!=null)for(String permission:info.requestedPermissions)
          check(!Arrays.asList("android.permission.READ_SMS","android.permission.RECEIVE_SMS","android.permission.READ_CONTACTS","android.permission.RECORD_AUDIO","android.permission.QUERY_ALL_PACKAGES","android.permission.REQUEST_INSTALL_PACKAGES").contains(permission),"Unexpected sensitive permission: "+permission);
      }catch(android.content.pm.PackageManager.NameNotFoundException e){throw new AssertionError(e);}
      check(app.getPackageManager().queryIntentServices(new Intent("android.service.notification.NotificationListenerService").setPackage(app.getPackageName()),0).isEmpty(),"Listener intent remains");
    });
    test("local session remains controllable without cross-app media access", () -> {
      app.youtube.start();
      check(app.localAudio.controller!=null && app.youtube.active()==app.localAudio.controller,"Own audio control unavailable");
      app.prefs.edit().putString("audioSource","youtube").apply();
      check(app.youtube.active()==null,"External session exposed");
      app.prefs.edit().putString("audioSource","local").apply();
      check(app.youtube.active()==app.localAudio.controller,"Local session not restored");
    });
    test("local playback exposes actual duration, metadata and supported controls", () -> {
      app.live.updateMedia();
      check(app.localAudio.foreground && app.localAudio.playing(),"No foreground playback");
      check(app.youtube.duration()>=29900,"Actual sample duration missing");
      check(contains(app.content,"Mint Horizon"),"Real track metadata absent");
      check(app.live.play.isEnabled() && app.live.seek.isEnabled(),"Supported local controls disabled");
      app.youtube.seek(10000);
    });
    SystemClock.sleep(350);
    test("local seeking and queue use real playback", () -> {
      check(app.localAudio.player.getCurrentPosition()>9000,"Local seek did not execute");
      app.localAudio.enqueue(1);app.youtube.act(android.media.session.PlaybackState.ACTION_SKIP_TO_NEXT);
    });
    awaitLocal(1);
    test("local next consumes queue and search keeps playing", () -> {
      check(app.localAudio.queue.isEmpty(),"Local queue did not consume next");
      app.live.search(true);EditText query=editText(app.drawer);query.setText("slate");
      check(contains(app.drawer,"Slate Rain") && !contains(app.drawer,"Mint Horizon"),"Local song filtering failed");
      query.setText("original bundled");check(contains(app.drawer,"Mint Horizon"),"Album filtering failed");
      check(app.localAudio.playing(),"Search interrupted local playback");app.closeDrawer();
      app.youtube.act(android.media.session.PlaybackState.ACTION_PAUSE);
    });
    SystemClock.sleep(250);
    test("local media-session pause and resume", () -> {
      check(!app.localAudio.playing(),"Local pause did not execute");
      app.youtube.act(android.media.session.PlaybackState.ACTION_PLAY);
    });
    awaitLocal(1);
    test("black screen has no widgets and local playback continues", () -> {
      app.live.blackScreen();check(app.blackOverlay!=null,"Black screen missing");
      check(!(app.blackOverlay instanceof ViewGroup),"Black view contains widgets");
      check(app.localAudio.playing(),"Black screen interrupted local audio");
    });
    waitForIdleSync();SystemClock.sleep(250);
    android.graphics.Bitmap black=getUiAutomation().takeScreenshot();
    check(black!=null,"Black view capture missing");
    boolean pixelsBlack=black.getPixel(black.getWidth()/2,black.getHeight()/2)==android.graphics.Color.BLACK;
    int centerX=black.getWidth()/2,centerY=black.getHeight()/2;black.recycle();
    runOnMainSync(() -> {app.onBackPressed();check(app.blackOverlay==null,"Back failed to leave black screen");app.live.blackScreen();});
    waitForIdleSync();
    long tapTime=SystemClock.uptimeMillis();
    sendPointerSync(MotionEvent.obtain(tapTime,tapTime,MotionEvent.ACTION_DOWN,centerX,centerY,0));
    sendPointerSync(MotionEvent.obtain(tapTime,tapTime+50,MotionEvent.ACTION_UP,centerX,centerY,0));
    waitForIdleSync();
    test("black pixels verified and touch or Back restores the UI", () -> {
      check(pixelsBlack,"Focus/UI overlay tinted the black pixels");
      check(app.blackOverlay==null,"Touch failed to leave black view");
      check(app.localAudio.playing(),"Black view dismissal stopped playback");
    });
    // API 29+ allows creation and deletion of our own MediaStore fixture without storage permission.
    if(Build.VERSION.SDK_INT>=29) {
      final android.net.Uri[] fixture=new android.net.Uri[1];
      test("user-selected content URI imports real audio metadata",() -> {
        android.content.ContentValues values=new android.content.ContentValues();
        values.put(android.provider.MediaStore.Audio.Media.DISPLAY_NAME,"Flow verification.wav");
        values.put(android.provider.MediaStore.Audio.Media.MIME_TYPE,"audio/wav");
        values.put(android.provider.MediaStore.Audio.Media.RELATIVE_PATH,"Music/FlowTVTest");
        fixture[0]=app.getContentResolver().insert(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,values);
        check(fixture[0]!=null,"Could not create own fixture");
        try(java.io.InputStream input=app.getAssets().open("mint.wav");java.io.OutputStream output=app.getContentResolver().openOutputStream(fixture[0])) {
          byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);
        } catch(java.io.IOException e){throw new AssertionError(e);}
        app.live.importAudio(Collections.singletonList(fixture[0]),0);
      });
      long deadline=SystemClock.uptimeMillis()+15000;
      final boolean[] imported=new boolean[1];
      while(SystemClock.uptimeMillis()<deadline) {
        runOnMainSync(() -> imported[0]=app.localAudio.library.size()>3);
        if(imported[0])break;SystemClock.sleep(100);
      }
      check(imported[0],"Import did not finish");
      test("imported file is playable, not a placeholder",() -> app.live.playLocal(3));
      awaitLocal(3);
      test("imported content playback duration is real",() -> {
        check(app.localAudio.player.getDuration()>=29900,"Imported audio did not decode");
        app.localAudio.stopPlayback();app.getContentResolver().delete(fixture[0],null,null);
        app.prefs.edit().remove("localAudio").apply();app.localAudio.refreshLibrary();app.live.playLocal(0);
      });
      awaitLocal(0);
    }
    final long[] beforeSleep=new long[1];
    runOnMainSync(() -> {app.youtube.seek(1000);});SystemClock.sleep(200);
    runOnMainSync(() -> beforeSleep[0]=app.localAudio.player.getCurrentPosition());
    shell("input keyevent KEYCODE_SLEEP");
    long sleepDeadline=SystemClock.uptimeMillis()+15000;
    final boolean[] sleepReady={false};
    while(SystemClock.uptimeMillis()<sleepDeadline){
      runOnMainSync(()->sleepReady[0]=!app.displayInteractive() && !app.handler.hasCallbacks(app.ticker));
      if(sleepReady[0])break;SystemClock.sleep(100);
    }
    SystemClock.sleep(1300);
    test("local audio continues while emulator display is off",() -> {
      android.os.PowerManager power=(android.os.PowerManager)app.getSystemService(android.content.Context.POWER_SERVICE);
      check(!power.isInteractive(),"Display did not become noninteractive");
      check(app.localAudio.playing(),"Local audio stopped with display off");
      check(app.localAudio.player.getCurrentPosition()>beforeSleep[0]+700,"Playback position did not advance with display off");
      check(!app.handler.hasCallbacks(app.ticker),"Dashboard polling remains scheduled while display off");
    });
    shell("input keyevent KEYCODE_WAKEUP");shell("wm dismiss-keyguard");SystemClock.sleep(350);
    test("screen wakes and local playback remains controllable",() -> {
      android.os.PowerManager power=(android.os.PowerManager)app.getSystemService(android.content.Context.POWER_SERVICE);
      check(power.isInteractive(),"Display did not wake");
      check(app.localAudio.playing(),"Playback stopped on wake");app.live.updateMedia();
    });
    if (options != null && "true".equals(options.getString("liveCapture"))) {
      runOnMainSync(() -> app.remote = false);
      screenshot("live-01-home", () -> app.navigate("Home"));
      screenshot("live-02-apps", () -> app.navigate("Apps"));
      screenshot("live-03-new", () -> app.navigate("New"));
      screenshot("live-04-dashboard", () -> app.navigate("Dashboard"));
      screenshot("live-05-audio", () -> {app.prefs.edit().putString("audioSource","youtube").apply();app.navigate("Audio");});
      screenshot("live-06-search", () -> { app.navigate("Audio"); app.live.search(true); });
      screenshot("live-07-media-access", () -> { app.navigate("Audio"); app.live.mediaAccess(); });
      screenshot("live-08-my-list", () -> app.navigate("My list"));
      screenshot("live-11-local-audio", () -> {app.prefs.edit().putString("audioSource","local").apply();app.navigate("Audio");});
      screenshot("live-12-local-search", () -> {app.navigate("Audio");app.live.localSearch();});
      screenshot("live-13-black-screen", () -> {app.navigate("Audio");app.live.blackScreen();});
      runOnMainSync(() -> {app.onBackPressed();app.prefs.edit().putString("audioSource","youtube").apply();});
      runOnMainSync(() -> { app.preview = false; app.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT); });
      long deadline = SystemClock.uptimeMillis() + 15000;
      while (app.landscape() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100);
      check(!app.landscape(), "Portrait transition failed");
      screenshot("live-09-portrait-home", () -> app.navigate("Home"));
      screenshot("live-10-portrait-audio", () -> app.navigate("Audio"));
      screenshot("v4-08-portrait-dashboard", () -> app.navigate("Dashboard"));
      runOnMainSync(() -> { app.preview = true; app.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE); });
    }
  }

  void awaitLocal(int track) {
    long deadline=SystemClock.uptimeMillis()+15000;
    final boolean[] ready=new boolean[1];
    while(SystemClock.uptimeMillis()<deadline) {
      runOnMainSync(() -> ready[0]=app.localAudio!=null && app.localAudio.track==track && app.localAudio.playing());
      if(ready[0])return;SystemClock.sleep(75);
    }
    throw new AssertionError("Local audio did not start track "+track);
  }
  void shell(String command) throws Exception {
    try(android.os.ParcelFileDescriptor fd=getUiAutomation().executeShellCommand(command);
        java.io.InputStream input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {
      byte[] bytes=new byte[1024];while(input.read(bytes)!=-1){}
    }
  }

  void collectFields(View view, ArrayList<EditText> fields) {
    if (view instanceof EditText) fields.add((EditText)view);
    if (view instanceof ViewGroup) { ViewGroup group = (ViewGroup)view; for (int n=0;n<group.getChildCount();n++) collectFields(group.getChildAt(n), fields); }
  }

  @Override
  public void onStart() {
    super.onStart();
    Bundle out = new Bundle();
    try {
      if(options!=null && "true".equals(options.getString("screensOnly"))){
        getTargetContext().getSharedPreferences("flow",0).edit().putBoolean("catalogueEnabled",true).putBoolean("designDemo",false).putInt("realTabsVersion",4).putBoolean("remote",false).putBoolean("preview",true).commit();
        app=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        FullReleaseChecks.capture(this);
        out.putString("stream","17 full-app screen captures completed; visual review only, no successful catalogue fixtures.");finish(Activity.RESULT_OK,out);return;
      }
      Intent i =
          new Intent(getTargetContext(), MainActivity.class)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getTargetContext()
          .getSharedPreferences("flow", 0)
          .edit()
          .clear()
          .putInt("visualVersion", 2)
          .putInt("realTabsVersion", 4)
          .putBoolean("designDemo", true)
          .putBoolean("catalogueEnabled", false)
          .putBoolean("preview", true)
          .putBoolean("remote", true)
          .commit();
      app = (MainActivity) startActivitySync(i);
      waitForIdleSync();
      test(
          "home sample labels and navigation",
          () -> {
            check(contains(app.content, "SAMPLE"), "Sample label missing");
            for (String p : app.sections) {
              app.navigate(p);
              check(app.page.equals(p), "Navigation failed " + p);
            }
            app.navigate("Home");
          });
      test(
          "catalogue content, language and provider filters",
          () -> {
            app.navigate("New");
            click("Shows");
            check(
                contains(app.content, "The Black Dagger")
                    && !contains(app.content, "Echoes of Tomorrow"),
                "Shows content filter failed");
            click("All");
            check(contains(app.content, "Echoes of Tomorrow"), "All content filter failed");
            app.navigate("Movies");
            app.genre = "Romance";
            app.build();
            check(
                contains(app.content, "Monsoon Letters") && !contains(app.content, "Raitra"),
                "Genre filtering failed");
            app.genre = "All";
            app.language = "Kannada";
            app.build();
            check(
                contains(app.content, "Monsoon Letters")
                    && !contains(app.content, "The Last Harbor"),
                "Language filtering failed");
            app.language = "All";
            app.provider = "Netflix";
            app.build();
            check(
                contains(app.content, "The Last Harbor")
                    && !contains(app.content, "Monsoon Letters"),
                "Provider filtering failed");
            app.provider = "All";
            app.navigate("Home");
          });
      test(
          "Kids age selection updates sample recommendations",
          () -> {
            app.navigate("Kids");
            click("6 – 8");
            check(
                firstDescription(app.content, ", sample Kids").startsWith("Space Rangers"),
                "Age selection did not update recommendations");
            click("3 – 5");
            check(
                firstDescription(app.content, ", sample Kids").startsWith("Jungle Friends"),
                "Age selection reset failed");
            app.navigate("Home");
          });
      test(
          "watchlist persisted across activity recreation",
          () -> {
            app.titleDetails(app.catalogue[0]);
            click("My list");
            check(
                app.prefs
                    .getStringSet("watchlist", new HashSet<String>())
                    .contains(app.catalogue[0].name),
                "Watchlist not persisted");
            app.closeDrawer();
            app.navigate("My list");
            check(contains(app.content, app.catalogue[0].name), "Watchlist missing UI");
            app.finish();
          });
      waitForIdleSync();
      app = (MainActivity) startActivitySync(i);
      waitForIdleSync();
      test(
          "watchlist restored on launch",
          () -> check(app.watchlist.contains(app.catalogue[0].name), "Watchlist restore failed"));
      test(
          "actual dashboard and unsupported phone controls",
          () -> {
            app.navigate("Dashboard");
            check(app.metricViews.size() >= 5, "Supported metric cards missing");
            check(app.metric("RAM", true).contains("Total"), "RAM unavailable");
            check(app.metric("Storage", true).contains("Data volume"), "Storage unavailable");
            check(!app.tv(), "Test image unexpectedly TV");
            check(
                app.metric("Ports & Inputs", true).contains("Unavailable on this device"),
                "Phone TV controls not labelled");
            check(app.metric("System", true).contains(android.os.Build.MODEL), "Model fabricated");
          });
      test(
          "drawer focus containment and Back",
          () -> {
            app.detail("RAM");
            check(app.drawer != null, "Drawer absent");
            check(
                app.content.getDescendantFocusability() == ViewGroup.FOCUS_BLOCK_DESCENDANTS,
                "Content focus not blocked");
            app.onBackPressed();
            check(app.drawer == null, "Back did not close drawer");
          });
      test(
          "real sample playback and uninterrupted search",
          () -> {
            app.navigate("Audio");
            app.startTrack(0);
            check(app.player != null && app.player.isPlaying(), "MediaPlayer did not start");
            check(app.player.getDuration() >= 29900, "Unexpected sample length");
            app.searchSongs();
            check(app.player.isPlaying(), "Search interrupted playback");
            check(contains(app.drawer, "Flow Originals"), "Search channel missing");
            EditText query = editText(app.drawer);
            check(query != null, "Search field missing");
            query.setText("slate");
            check(
                contains(app.drawer, "Slate Rain") && !contains(app.drawer, "Mint Horizon"),
                "Song filtering failed");
            query.setText("evening geometry");
            check(
                contains(app.drawer, "Lavender Night") && !contains(app.drawer, "Slate Rain"),
                "Album filtering failed");
            query.setText("flow studio");
            check(
                contains(app.drawer, "Mint Horizon") && contains(app.drawer, "Slate Rain"),
                "Artist filtering failed");
            query.setText("no-matching-song");
            check(
                !find(app.drawer, "Play now").isEnabled(), "Empty result should disable Play now");
            query.setText("");
            check(app.player.isPlaying(), "Filtering interrupted audio");
            click("Add to queue");
            check(app.queue.size() == 1, "Queue action failed");
            app.closeDrawer();
            app.nextTrack();
            check(app.queue.isEmpty(), "Next did not consume queue");
            app.seek(10000);
          });
      SystemClock.sleep(350);
      test(
          "sample seeking and pause/resume",
          () -> {
            check(app.player.getCurrentPosition() > 9000, "Seek failed");
            app.toggleAudio();
            check(!app.player.isPlaying(), "Pause failed");
            app.toggleAudio();
            check(app.player.isPlaying(), "Resume failed");
          });
      test(
          "favourites order persistence and cancel",
          () -> {
            app.scanApps();
            check(app.apps.size() >= 2, "Need two installed apps");
            app.favorites.clear();
            app.favorites.add(app.apps.get(0).pkg);
            app.favorites.add(app.apps.get(1).pkg);
            String first = app.favorites.get(0);
            app.movingPackage = first;
            app.movingOriginal = 0;
            app.moveFavorite(1);
            check(app.favorites.indexOf(first) == 1, "Remote move failed");
            app.cancelMove();
            check(app.favorites.indexOf(first) == 0, "Cancel failed");
            app.movingPackage = first;
            app.movingOriginal = 0;
            app.moveFavorite(1);
            app.movingPackage = null;
            app.saveFavorites();
            check(
                app.prefs.getString("favorites", "").indexOf(first)
                    > app.prefs.getString("favorites", "").indexOf(app.apps.get(1).pkg),
                "Order not saved");
          });
      runOnMainSync(() -> app.navigate("Home"));
      waitForIdleSync();
      final int[] tap = new int[2];
      test(
          "remote target focus setup",
          () -> {
            Button target = find(app.content, "Watch now");
            check(target.requestFocusFromTouch(), "Cannot focus sample details button");
            Button ok = find(app.getWindow().getDecorView(), "OK");
            check(ok != null, "Remote OK missing");
            ok.getLocationOnScreen(tap);
            tap[0] += ok.getWidth() / 2;
            tap[1] += ok.getHeight() / 2;
          });
      long now = SystemClock.uptimeMillis();
      sendPointerSync(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, tap[0], tap[1], 0));
      sendPointerSync(MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, tap[0], tap[1], 0));
      waitForIdleSync();
      test(
          "on-screen remote touch restores focus and activates",
          () -> {
            check(app.drawer != null, "Remote OK touch did not open title details");
            app.sendRemote(KeyEvent.KEYCODE_BACK);
            check(app.drawer == null, "Remote Back failed");
          });
      if (options != null && "true".equals(options.getString("capture"))) captureScreens();
      runOnMainSync(
          () -> {
            app.navigate("Audio");
            app.startTrack(1);
          });
      waitForIdleSync();
      check(
          getUiAutomation()
              .performGlobalAction(
                  android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME),
          "System Home action unavailable");
      final boolean[] background = new boolean[1];
      long deadline = SystemClock.uptimeMillis() + 30000;
      do {
        runOnMainSync(() -> background[0] = !app.resumed);
        if (background[0]) break;
        SystemClock.sleep(200);
      } while (SystemClock.uptimeMillis() < deadline);
      test(
          "background stops audio and dashboard polling",
          () -> {
            check(
                background[0],
                "Home did not transition the activity to background within 30 seconds");
            check(!app.player.isPlaying(), "Audio continued in background");
            check(!app.resumed, "Ticker still enabled");
          });
      runLive(i);
      CatalogueChecks.run(this);
      FullReleaseChecks.run(this);
      runOnMainSync(() -> {if(app.localAudio!=null)app.localAudio.stopPlayback();});
      out.putString("stream", report + "\n" + passed + " checks passed\n");
      out.putInt("passed", passed);
      finish(Activity.RESULT_OK, out);
    } catch (Throwable e) {
      out.putString(
          "stream",
          report + "\nFAIL " + e.toString() + "\n" + android.util.Log.getStackTraceString(e));
      finish(Activity.RESULT_CANCELED, out);
    }
  }
}
