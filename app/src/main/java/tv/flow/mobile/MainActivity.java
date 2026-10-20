package tv.flow.mobile;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.hardware.usb.*;
import android.media.*;
import android.media.tv.*;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.*;

/** Local-first mobile/TV prototype. No catalogue service or privileged system access. */
public class MainActivity extends Activity {
  static final int BG = 0xff03111f,
      CARD = 0xff15263b,
      TEXT = 0xffeef2f8,
      MUTED = 0xffa5b2c6,
      MINT = 0xff7df3d3,
      LAV = 0xffb9a6fa,
      LINE = 0xff344258;
  float designScale = 1;
  FlowDesign design;
  LiveFlow live;
  YouTubeMedia youtube;
  LocalAudioService localAudio;
  boolean localBound;
  View blackOverlay;
  final ServiceConnection localConnection = new ServiceConnection() {
    @Override public void onServiceConnected(ComponentName name, IBinder binder) {
      localAudio = ((LocalAudioService.LocalBinder)binder).service();
      localAudio.changed = () -> { if(live!=null && resumed) live.updateMedia(); };
      if(live!=null) live.updateMedia();
    }
    @Override public void onServiceDisconnected(ComponentName name) { localAudio=null; if(live!=null)live.updateMedia(); }
  };
  boolean watchingPackages;
  final BroadcastReceiver packageChanges = new BroadcastReceiver() {
    @Override public void onReceive(Context context, Intent intent) {
      scanApps();
      if (resumed && !demo() && !page.equals("Audio") && !page.equals("Dashboard")) build();
    }
  };
  LinearLayout root, body, content, top, audioInfo;
  FrameLayout stage;
  LinearLayout drawer;
  TextView clock, trackStatus, progressLabel;
  SeekBar progress;
  Button playButton, queueButton;
  View lastRemoteFocus;
  SharedPreferences prefs;
  String page = "Home", language = "All", provider = "All", type = "All", genre = "All";
  boolean preview = false,
      remote = true,
      resumed = false,
      dragging = false,
      audioVisible = false,
      updatingSeek = false;
  String movingPackage = null;
  int movingOriginal = -1, track = 0;
  MediaPlayer player;
  ArrayList<Integer> queue = new ArrayList<>();
  ArrayList<AppEntry> apps = new ArrayList<>();
  ArrayList<String> favorites = new ArrayList<>();
  HashSet<String> watchlist = new HashSet<>();
  final Handler handler = new Handler();
  final ArrayList<TextView> metricViews = new ArrayList<>();
  final ArrayList<String> metricKeys = new ArrayList<>();
  final String[] sections = {"Home", "Movies", "Shows", "New", "Kids", "My list", "Dashboard"};
  final String[] songTitles = {"Mint Horizon", "Slate Rain", "Lavender Night"};
  final String[] songArtists = {"Flow Studio", "Flow Studio", "Flow Studio"};
  final String[] songAlbums = {"Quiet Signals", "Quiet Signals", "Evening Geometry"};
  final String[] audioFiles = {"mint.wav", "slate.wav", "lavender.wav"};

  static class AppEntry {
    String pkg, label;
    Drawable icon;
    Intent intent;
  }

  static class Title {
    String name, kind, lang, service, badge, synopsis;
    int minutes;

    Title(String n, String k, String l, String s, String b, int m, String desc) {
      name = n;
      kind = k;
      lang = l;
      service = s;
      badge = b;
      minutes = m;
      synopsis = desc;
    }
  }

  final Title[] catalogue = {
    new Title(
        "The Lost Horizon",
        "Movie",
        "English",
        "Prime Video",
        "NEW RELEASE",
        124,
        "A group of explorers discover a hidden valley where nature still holds its secrets."
            + " Fictional sample; no live availability is claimed."),
    new Title(
        "Echoes of Tomorrow",
        "Movie",
        "Hindi",
        "JioHotstar",
        "NEW RELEASE",
        118,
        "An astronaut follows a mysterious signal beyond the edge of known space. Fictional sample;"
            + " no live availability is claimed."),
    new Title(
        "The Black Dagger",
        "Show",
        "Hindi",
        "Netflix",
        "NEW EPISODE",
        44,
        "A detective follows a trail through a rain-soaked city. Fictional sample; no live"
            + " availability is claimed."),
    new Title(
        "Beyond the Ice",
        "Movie",
        "English",
        "Prime Video",
        "NEWLY ADDED",
        107,
        "A mountaineer finds an unexpected journey beyond the frozen peaks. Fictional sample; no"
            + " live availability is claimed."),
    new Title(
        "A Fly in the Sky",
        "Movie",
        "Hindi",
        "JioHotstar",
        "NEW RELEASE",
        102,
        "A young writer follows a distant dream through the mountains. Fictional sample; no live"
            + " availability is claimed."),
    new Title(
        "The Last Bloom",
        "Movie",
        "Hindi",
        "JioHotstar",
        "NEWLY ADDED",
        116,
        "Two strangers meet again as the seasons change. Fictional sample; no live availability is"
            + " claimed."),
    new Title(
        "City of Shadows",
        "Movie",
        "Hindi",
        "Netflix",
        "NEWLY ADDED",
        128,
        "A lone investigator navigates the neon streets after dark. Fictional sample; no live"
            + " availability is claimed."),
    new Title(
        "The River Within",
        "Movie",
        "English",
        "Prime Video",
        "NEW RELEASE",
        110,
        "An old river brings a family back together. Fictional sample; no live availability is"
            + " claimed."),
    new Title(
        "Desert Kings",
        "Movie",
        "Hindi",
        "ZEE5",
        "NEW RELEASE",
        132,
        "A desert kingdom faces a choice between power and peace. Fictional sample; no live"
            + " availability is claimed."),
    new Title(
        "Monsoon Letters",
        "Movie",
        "Kannada",
        "Prime Video",
        "NEW RELEASE",
        128,
        "In a rainy hill town, a young photographer discovers a bundle of unsent letters that lead"
            + " her on a journey of love, loss and new beginnings. Fictional sample; no live"
            + " availability is claimed."),
    new Title(
        "The Last Harbor",
        "Movie",
        "English",
        "Netflix",
        "NEWLY ADDED",
        114,
        "A lighthouse keeper returns to the coast that shaped his childhood. Fictional sample; no"
            + " live availability is claimed."),
    new Title(
        "Raitra",
        "Movie",
        "Telugu",
        "JioHotstar",
        "NEW RELEASE",
        132,
        "One night changes a detective and the city he protects. Fictional sample; no live"
            + " availability is claimed."),
    new Title(
        "The Quiet Forest",
        "Movie",
        "Malayalam",
        "Prime Video",
        "NEWLY ADDED",
        106,
        "A traveller follows a quiet path through a forest full of stories. Fictional sample; no"
            + " live availability is claimed."),
    new Title(
        "City of Echoes",
        "Movie",
        "Hindi",
        "ZEE5",
        "COMING SOON",
        123,
        "Two lives cross as memories echo across the city. Fictional sample; no live availability"
            + " is claimed."),
    new Title(
        "A Different Sky",
        "Movie",
        "Tamil",
        "Sony LIV",
        "COMING SOON",
        121,
        "An artist imagines a brighter horizon. Fictional sample; no live availability is"
            + " claimed."),
    new Title(
        "Nine Summers",
        "Movie",
        "Tamil",
        "ZEE5",
        "NEWLY ADDED",
        131,
        "Two old friends reunite after nine summers apart. Fictional sample; no live availability"
            + " is claimed."),
    new Title(
        "Shadow Code",
        "Show",
        "Hindi",
        "Prime Video",
        "NEW EPISODE",
        43,
        "A team uncovers a network of secrets. Fictional sample; no live availability is claimed."),
    new Title(
        "Room 306",
        "Show",
        "Hindi",
        "JioHotstar",
        "NEW EPISODE",
        39,
        "A hotel room holds a different mystery each night. Fictional sample; no live availability"
            + " is claimed."),
    new Title(
        "The Green Line",
        "Show",
        "Tamil",
        "Netflix",
        "NEW EPISODE",
        42,
        "An unexpected journey brings commuters together. Fictional sample; no live availability is"
            + " claimed."),
    new Title(
        "Half Light",
        "Show",
        "English",
        "ZEE5",
        "NEWLY ADDED",
        46,
        "At dusk, a town begins to tell its secrets. Fictional sample; no live availability is"
            + " claimed."),
    new Title(
        "River Town",
        "Show",
        "Hindi",
        "Prime Video",
        "NEW EPISODE",
        41,
        "Friends navigate life beside a river. Fictional sample; no live availability is claimed."),
    new Title(
        "Call Me Home",
        "Show",
        "Kannada",
        "Sony LIV",
        "NEW EPISODE",
        38,
        "A young man returns to his family home. Fictional sample; no live availability is"
            + " claimed."),
    new Title(
        "Paper Planes",
        "Show",
        "Hindi",
        "Prime Video",
        "NEWLY ADDED",
        40,
        "Every letter carries a new possibility. Fictional sample; no live availability is"
            + " claimed."),
    new Title(
        "The Last Page",
        "Show",
        "Tamil",
        "Netflix",
        "NEWLY ADDED",
        44,
        "A bookshop connects strangers across the city. Fictional sample; no live availability is"
            + " claimed."),
    new Title(
        "Metro People",
        "Show",
        "Telugu",
        "JioHotstar",
        "NEWLY ADDED",
        37,
        "Stories from the people who make a city. Fictional sample; no live availability is"
            + " claimed."),
    new Title(
        "Wildflower",
        "Show",
        "Kannada",
        "JioHotstar",
        "COMING SOON",
        42,
        "A young botanist begins a new life. Fictional sample; no live availability is claimed."),
    new Title(
        "Tide & Town",
        "Show",
        "Malayalam",
        "Prime Video",
        "NEWLY ADDED",
        45,
        "A coastal community prepares for change. Fictional sample; no live availability is"
            + " claimed."),
    new Title(
        "The Repair Shop",
        "Show",
        "English",
        "Sony LIV",
        "NEWLY ADDED",
        35,
        "Small repairs reveal larger stories. Fictional sample; no live availability is claimed."),
    new Title(
        "Leo and the Magic Forest",
        "Kids",
        "English",
        "Sample Kids",
        "NEWLY ADDED",
        24,
        "Leo, a brave little lion, and his friends explore a magical forest. Fictional sample; no"
            + " live availability is claimed."),
    new Title(
        "Jungle Friends",
        "Kids",
        "English",
        "Sample Kids",
        "NEWLY ADDED",
        18,
        "Friendly animals learn to look after their forest. Fictional sample; no live availability"
            + " is claimed."),
    new Title(
        "Space Rangers",
        "Kids",
        "Hindi",
        "Sample Kids",
        "NEWLY ADDED",
        21,
        "Young explorers learn about the stars. Fictional sample; no live availability is"
            + " claimed."),
    new Title(
        "Dino World",
        "Kids",
        "Tamil",
        "Sample Kids",
        "NEWLY ADDED",
        20,
        "A playful journey into a colourful dinosaur world. Fictional sample; no live availability"
            + " is claimed."),
    new Title(
        "Mia's World",
        "Kids",
        "English",
        "Sample Kids",
        "NEWLY ADDED",
        17,
        "Mia and her friends solve small everyday mysteries. Fictional sample; no live availability"
            + " is claimed."),
    new Title(
        "The Little Explorer",
        "Kids",
        "Hindi",
        "Sample Kids",
        "NEWLY ADDED",
        19,
        "An explorer discovers a new friendship. Fictional sample; no live availability is"
            + " claimed."),
    new Title(
        "Underwater Buddies",
        "Kids",
        "English",
        "Sample Kids",
        "NEWLY ADDED",
        16,
        "Sea creatures learn to help each other. Fictional sample; no live availability is"
            + " claimed."),
    new Title(
        "The Quiet Coast",
        "Movie",
        "English",
        "Sample Cinema",
        "NEW RELEASE",
        92,
        "A photographer follows the coastline in search of a slower way of living. Fictional sample"
            + " title; no stream is integrated."),
    new Title(
        "Cities After Dark",
        "Show",
        "Hindi",
        "Sample Studio",
        "NEW EPISODE",
        42,
        "An illustrated journey through the people who keep a city awake. Fictional sample"
            + " series."),
    new Title(
        "A Small Universe",
        "Movie",
        "Tamil",
        "Sample Cinema",
        "NEWLY ADDED",
        104,
        "Two neighbours discover a world of possibility in their shared courtyard. Fictional sample"
            + " title."),
    new Title(
        "Wild Paths",
        "Show",
        "English",
        "Sample Studio",
        "NEW EPISODE",
        28,
        "Explore imaginary trails and quiet landscapes. Fictional sample series."),
    new Title(
        "Tomorrow, Together",
        "Movie",
        "Hindi",
        "Sample Cinema",
        "COMING SOON",
        110,
        "An optimistic story about friendship and a community garden. Fictional sample title."),
    new Title(
        "Little Cloud Club",
        "Kids",
        "English",
        "Sample Kids",
        "NEWLY ADDED",
        18,
        "Friendly clouds learn to share the sky. Fictional sample children's programme.")
  };
  final Runnable ticker =
      new Runnable() {
        public void run() {
          if (!resumed) return;
          if (clock != null)
            clock.setText(
                new java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date()));
          if (page.equals("Dashboard")) {
            if (design != null) design.refreshMetrics();
          }
          updateAudio();
          handler.postDelayed(this, 1000);
        }
      };

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
    getWindow().setStatusBarColor(BG);
    getWindow().setNavigationBarColor(BG);
    prefs = getSharedPreferences("flow", MODE_PRIVATE);
    youtube = new YouTubeMedia(this);
    localBound=bindService(new Intent(this,LocalAudioService.class),localConnection,Context.BIND_AUTO_CREATE);
    IntentFilter packages = new IntentFilter();
    packages.addAction(Intent.ACTION_PACKAGE_ADDED);
    packages.addAction(Intent.ACTION_PACKAGE_REMOVED);
    packages.addAction(Intent.ACTION_PACKAGE_CHANGED);
    packages.addDataScheme("package");
    if (Build.VERSION.SDK_INT >= 33) registerReceiver(packageChanges, packages, Context.RECEIVER_EXPORTED);
    else registerReceiver(packageChanges, packages);
    watchingPackages = true;
    remote = prefs.getBoolean("remote", false);
    preview = prefs.getBoolean("preview", true);
    if (prefs.getInt("visualVersion", 0) < 2) {
      preview = true;
      prefs.edit().putInt("visualVersion", 2).putBoolean("preview", true).apply();
    }
    if (preview && !tv()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    watchlist.addAll(prefs.getStringSet("watchlist", new HashSet<String>()));
    try {
      JSONArray a = new JSONArray(prefs.getString("favorites", "[]"));
      for (int i = 0; i < a.length(); i++) favorites.add(a.getString(i));
    } catch (Exception ignored) {
    }
    scanApps();
    if (!prefs.contains("favorites")) {
      for (String pkg :
          new String[] {
            "com.google.android.youtube",
            "com.google.android.youtube.tv",
            "com.netflix.mediaclient",
            "com.netflix.ninja",
            "com.amazon.avod.thirdpartyclient",
            "com.amazon.amazonvideo.livingroom",
            "in.startv.hotstar",
            "org.videolan.vlc",
            "com.android.vending"
          }) {
        if (favorites.size() < 6 && findApp(pkg) != null && !favorites.contains(pkg))
          favorites.add(pkg);
      }
      for (AppEntry app : apps)
        if (favorites.size() < 6 && !favorites.contains(app.pkg)) favorites.add(app.pkg);
      saveFavorites();
    }
    if (state != null) {
      page = state.getString("page", "Home");
      track = state.getInt("track", 0);
    }
    build();
  }

  @Override
  public void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putString("page", page);
    b.putInt("track", track);
  }

  @Override
  public void onConfigurationChanged(Configuration c) {
    super.onConfigurationChanged(c);
    build();
  }

  @Override
  protected void onResume() {
    super.onResume();
    resumed = true;
    scanApps();
    youtube.start();
    if (!demo()) build();
    handler.removeCallbacks(ticker);
    handler.post(ticker);
  }

  @Override
  protected void onPause() {
    resumed = false;
    handler.removeCallbacks(ticker);
    if (player != null && player.isPlaying()) player.pause();
    youtube.stop();
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    if (player != null) player.release();
    if (watchingPackages) unregisterReceiver(packageChanges);
    youtube.stop();
    if(localAudio!=null) localAudio.changed=null;
    if(localBound) unbindService(localConnection);
    super.onDestroy();
  }

  int dp(float n) {
    return Math.max(1, (int) (n * designScale + .5f));
  }

  boolean landscape() {
    return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
  }

  boolean tv() {
    return ((UiModeManager) getSystemService(UI_MODE_SERVICE)).getCurrentModeType()
        == Configuration.UI_MODE_TYPE_TELEVISION;
  }

  GradientDrawable shape(int color) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(9));
    return d;
  }

  android.graphics.drawable.StateListDrawable buttonBackground() {
    android.graphics.drawable.StateListDrawable s =
        new android.graphics.drawable.StateListDrawable();
    GradientDrawable f = shape(0xff263b36);
    f.setStroke(dp(3), MINT);
    s.addState(new int[] {android.R.attr.state_focused}, f);
    GradientDrawable p = shape(0xff334b3c);
    p.setStroke(dp(2), MINT);
    s.addState(new int[] {android.R.attr.state_pressed}, p);
    s.addState(new int[] {}, shape(CARD));
    return s;
  }

  TextView text(String s, int size, int color) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextColor(color);
    t.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(size));
    t.setPadding(dp(4), dp(3), dp(4), dp(3));
    return t;
  }

  TextView heading(String s) {
    TextView t = text(s, 22, TEXT);
    t.setTypeface(null, Typeface.BOLD);
    return t;
  }

  LinearLayout col() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  LinearLayout row() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.HORIZONTAL);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  Button button(String label, Runnable action) {
    Button b = new Button(this);
    b.setText(label);
    b.setTextColor(TEXT);
    b.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(13));
    b.setAllCaps(false);
    b.setMinWidth(dp(48));
    b.setMinimumWidth(dp(48));
    b.setMinHeight(dp(32));
    b.setMinimumHeight(dp(32));
    b.setPadding(dp(13), dp(7), dp(13), dp(7));
    b.setBackground(buttonBackground());
    StateListDrawable ring = new StateListDrawable();
    GradientDrawable focusRing = shape(Color.TRANSPARENT);
    focusRing.setStroke(dp(2), MINT);
    ring.addState(new int[] {android.R.attr.state_focused}, focusRing);
    ring.addState(new int[] {}, new ColorDrawable(Color.TRANSPARENT));
    b.setForeground(ring);
    b.setOnFocusChangeListener(
        (v, focused) -> {
          if (focused) lastRemoteFocus = v;
        });
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
    p.setMargins(dp(3), dp(3), dp(3), dp(3));
    b.setLayoutParams(p);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  void addButton(LinearLayout l, String s, Runnable r) {
    l.addView(button(s, r));
  }

  void hint(LinearLayout l, String s) {
    l.addView(text(s, 12, MUTED));
  }

  void space(LinearLayout l, int n) {
    View v = new View(this);
    l.addView(v, new LinearLayout.LayoutParams(1, dp(n)));
  }

  void horizontal(LinearLayout parent, LinearLayout row) {
    HorizontalScrollView h = new HorizontalScrollView(this);
    h.setHorizontalScrollBarEnabled(false);
    h.setFillViewport(false);
    h.addView(row);
    parent.addView(h, new LinearLayout.LayoutParams(-1, -2));
  }

  void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }

  void build() {
    blackOverlay=null;
    design = new FlowDesign(this);
    live = new LiveFlow(this, design);
    design.build();
  }

  boolean demo() { return prefs.getBoolean("designDemo", false); }
  boolean localMode() { return prefs.getString("audioSource", "youtube").equals("local"); }

  @Override protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request,result,data);
    if(request==70 && result==RESULT_OK && data!=null) {
      ArrayList<Uri> uris=new ArrayList<>();
      if(data.getClipData()!=null) for(int n=0;n<data.getClipData().getItemCount();n++) uris.add(data.getClipData().getItemAt(n).getUri());
      else if(data.getData()!=null) uris.add(data.getData());
      if(live!=null) live.importAudio(uris, data.getFlags());
    }
  }

  void launchApp(AppEntry app) {
    if (app == null) { toast("App unavailable on this device"); return; }
    try {
      startActivity(app.intent);
      JSONObject recent = new JSONObject(prefs.getString("recentApps", "{}"));
      recent.put(app.pkg, System.currentTimeMillis());
      prefs.edit().putString("recentApps", recent.toString()).apply();
    } catch (Exception e) { toast("Unable to open this app"); scanApps(); }
  }

  View firstFocusable(ViewGroup group) {
    for (View view : group.getFocusables(View.FOCUS_DOWN))
      if (view != group && view.isShown()) return view;
    return null;
  }

  void navigate(String s) {
    cancelMove();
    if ((s.equals("Movies") && type.equals("Show")) || (s.equals("Shows") && type.equals("Movie")))
      type = "All";
    page = s;
    build();
  }

  void sendRemote(int key) {
    if (key == KeyEvent.KEYCODE_BACK) {
      onBackPressed();
      return;
    }
    if (movingPackage != null && key >= 19 && key <= 22) {
      moveFavorite(key == 19 || key == 21 ? -1 : 1);
      return;
    }
    View selected = restoreRemoteFocus();
    if (selected == null) return;
    if (key == KeyEvent.KEYCODE_DPAD_CENTER && selected.isClickable()) {
      selected.performClick();
      return;
    }
    dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
    dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, key));
  }

  View restoreRemoteFocus() {
    View focused = getCurrentFocus();
    if (focused == null || focused == drawer || !focused.isFocusable()) {
      View target = lastRemoteFocus;
      if (target == null
          || !target.isAttachedToWindow()
          || !target.isShown()
          || (drawer != null && !isWithin(target, drawer)))
        target = firstFocusable(drawer != null ? drawer : content);
      if (target == null) return null;
      target.requestFocusFromTouch();
      focused = target;
    }
    return focused;
  }

  void hideBars() {
    if (Build.VERSION.SDK_INT >= 30) {
      android.view.WindowInsetsController c = getWindow().getInsetsController();
      if (c != null) {
        c.hide(android.view.WindowInsets.Type.systemBars());
        c.setSystemBarsBehavior(
            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
      }
    } else
      getWindow()
          .getDecorView()
          .setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                  | View.SYSTEM_UI_FLAG_FULLSCREEN
                  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                  | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                  | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                  | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  }

  @Override
  public void onWindowFocusChanged(boolean focused) {
    super.onWindowFocusChanged(focused);
    if (focused) hideBars();
  }

  boolean isWithin(View view, ViewGroup parent) {
    View current = view;
    while (current != null) {
      if (current == parent) return true;
      android.view.ViewParent ancestor = current.getParent();
      current = ancestor instanceof View ? (View) ancestor : null;
    }
    return false;
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent e) {
    if (movingPackage != null && e.getAction() == KeyEvent.ACTION_DOWN) {
      int k = e.getKeyCode();
      if (k >= 19 && k <= 22) {
        moveFavorite(k == 19 || k == 21 ? -1 : 1);
        return true;
      }
      if (k == 23 || k == 66) {
        movingPackage = null;
        saveFavorites();
        build();
        toast("Favourite order saved");
        return true;
      }
      if (k == 4) {
        cancelMove();
        build();
        return true;
      }
    }
    return super.dispatchKeyEvent(e);
  }

  @Override
  public void onBackPressed() {
    if (blackOverlay!=null) { if(blackOverlay.getParent()!=null)((ViewGroup)blackOverlay.getParent()).removeView(blackOverlay);blackOverlay=null;return; }
    if (movingPackage != null) {
      cancelMove();
      build();
      return;
    }
    if (drawer != null) {
      closeDrawer();
      return;
    }
    if (!page.equals("Home")) {
      navigate("Home");
      return;
    }
    new AlertDialog.Builder(this)
        .setTitle("Exit Flow TV?")
        .setMessage("Your local preferences are saved.")
        .setNegativeButton("Stay", null)
        .setPositiveButton("Exit", (d, w) -> finish())
        .show();
  }

  void closeDrawer() {
    if (drawer != null) {
      stage.removeView(drawer);
      drawer = null;
      content.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
      content.setDescendantFocusability(android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS);
      content.setAlpha(1f);
      content.requestFocus();
    }
  }

  LinearLayout panel(String title) {
    return design.panel(title);
  }

  void home() {
    design.home();
  }

  void scanApps() {
    apps.clear();
    PackageManager pm = getPackageManager();
    HashSet<String> seen = new HashSet<>();
    for (String category :
        new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
      Intent q = new Intent(Intent.ACTION_MAIN).addCategory(category);
      for (ResolveInfo info : pm.queryIntentActivities(q, 0)) {
        String pkg = info.activityInfo.packageName;
        if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
        AppEntry a = new AppEntry();
        a.pkg = pkg;
        a.label = info.loadLabel(pm).toString();
        a.icon = info.loadIcon(pm);
        a.intent =
            new Intent(Intent.ACTION_MAIN)
                .addCategory(category)
                .setComponent(new ComponentName(pkg, info.activityInfo.name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        apps.add(a);
      }
    }
    apps.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
  }

  AppEntry findApp(String pkg) {
    for (AppEntry a : apps) if (a.pkg.equals(pkg)) return a;
    return null;
  }

  void saveFavorites() {
    prefs.edit().putString("favorites", new JSONArray(favorites).toString()).apply();
  }

  void favoriteRow() {
    design.favoriteRow();
  }

  Button appTile(AppEntry a, String pkg) {
    Button b =
        button(
            (a == null ? pkg + "\nUnavailable" : a.label),
            () -> {
              if (movingPackage != null) {
                movingPackage = null;
                saveFavorites();
                build();
                return;
              }
              if (a == null) {
                toast("App unavailable; remove it from favourites in All Apps");
                return;
              }
              try {
                startActivity(a.intent);
              } catch (Exception ex) {
                toast("This app is no longer available");
                scanApps();
              }
            });
    b.setTag(pkg);
    if (pkg.equals(movingPackage)) b.setTextColor(MINT);
    b.setMaxLines(3);
    b.setWidth(dp(150));
    if (a != null) {
      Drawable d = a.icon;
      d.setBounds(0, 0, dp(36), dp(36));
      b.setCompoundDrawables(null, d, null, null);
      b.setCompoundDrawablePadding(dp(8));
    }
    return b;
  }

  void moveFavorite(int delta) {
    if (movingPackage == null) return;
    int i = favorites.indexOf(movingPackage),
        j = Math.max(0, Math.min(favorites.size() - 1, i + delta));
    Collections.swap(favorites, i, j);
    if (page.equals("Home") || page.equals("Apps")) {
      build();
      content.post(
          () -> {
            View moved = content.findViewWithTag(movingPackage);
            if (moved != null) moved.requestFocusFromTouch();
          });
    }
    toast("Position " + (j + 1) + " · OK saves · Back cancels");
  }

  void cancelMove() {
    if (movingPackage != null) {
      favorites.remove(movingPackage);
      favorites.add(Math.max(0, Math.min(movingOriginal, favorites.size())), movingPackage);
      movingPackage = null;
    }
  }

  void allApps() {
    design.allApps();
  }

  void titleRow(LinearLayout target, List<Title> list) {
    LinearLayout r = row();
    for (Title t : list) {
      Button b =
          button(
              t.badge + "\n\n" + t.name + "\n" + t.lang + " · " + t.minutes + " min\nSAMPLE",
              () -> titleDetails(t));
      b.setWidth(dp(205));
      b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
      b.setMinHeight(dp(142));
      r.addView(b);
    }
    horizontal(target, r);
  }

  void cataloguePage() {
    if (page.equals("Kids")) design.kids();
    else design.catalogue();
  }

  interface Selection {
    void selected(String s);
  }

  void choose(String title, String[] values, Selection callback) {
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setItems(values, (d, w) -> callback.selected(values[w]))
        .setNegativeButton("Cancel", null)
        .show();
  }

  void titleDetails(Title t) {
    design.titleDetails(t);
  }

  void openGlobalSearch() {
    if (!demo()) { live.search(false); return; }
    LinearLayout p = panel("Search");
    EditText q = new EditText(this);
    q.setSingleLine(true);
    q.setTextColor(TEXT);
    q.setHintTextColor(MUTED);
    q.setHint("Search sample titles");
    p.addView(q);
    LinearLayout results = col();
    p.addView(results);
    Runnable refresh =
        () -> {
          results.removeAllViews();
          for (Title t : catalogue)
            if (t.name
                .toLowerCase(Locale.ROOT)
                .contains(q.getText().toString().toLowerCase(Locale.ROOT)))
              addButton(results, t.name + " · Sample", () -> titleDetails(t));
        };
    q.addTextChangedListener(watcher(refresh));
    hint(p, "Local samples only. No online search.");
    refresh.run();
  }

  TextWatcher watcher(Runnable r) {
    return new TextWatcher() {
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      public void onTextChanged(CharSequence s, int start, int before, int count) {
        r.run();
      }

      public void afterTextChanged(Editable e) {}
    };
  }

  void dashboard() {
    design.dashboard();
  }

  String bytes(long n) {
    if (n < 0) return "Unavailable";
    return android.text.format.Formatter.formatFileSize(this, n);
  }

  String metric(String key, boolean detailed) {
    try {
      switch (key) {
        case "RAM":
          ActivityManager.MemoryInfo m = new ActivityManager.MemoryInfo();
          ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(m);
          return "Total "
              + bytes(m.totalMem)
              + "\nUsed "
              + bytes(m.totalMem - m.availMem)
              + " · Available "
              + bytes(m.availMem)
              + (detailed
                  ? "\nAndroid available memory includes reclaimable memory. Low memory: "
                      + m.lowMemory
                  : "");
        case "Storage":
          StatFs fs = new StatFs(getFilesDir().getAbsolutePath());
          return "Data volume "
              + bytes(fs.getTotalBytes())
              + "\nUsed "
              + bytes(fs.getTotalBytes() - fs.getAvailableBytes())
              + " · Free "
              + bytes(fs.getAvailableBytes())
              + (detailed
                  ? "\n"
                      + "This is the app's writable data volume, not advertised device capacity."
                      + " System/reserved space differs."
                  : "");
        case "CPU":
          return Build.SUPPORTED_ABIS[0]
              + " · "
              + Runtime.getRuntime().availableProcessors()
              + " available cores"
              + (detailed
                  ? "\nHardware: "
                      + Build.HARDWARE
                      + "\nSupported ABIs: "
                      + Arrays.toString(Build.SUPPORTED_ABIS)
                      + "\nCPU utilization and temperature unavailable through supported APIs."
                  : "");
        case "Wi-Fi":
          ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
          NetworkCapabilities c = cm.getNetworkCapabilities(cm.getActiveNetwork());
          return c == null
              ? "No active network"
              : (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                      ? "Active transport: Wi-Fi"
                      : "Active transport: other network")
                  + (detailed
                      ? "\nInternet capability: "
                          + c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                          + "\nValidated: "
                          + c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                          + "\nSSID, password and location are not requested."
                      : "");
        case "Bluetooth":
          return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
              ? "Bluetooth hardware present\nStatus/devices: permission not requested"
                  + (detailed
                      ? "\n"
                          + "Use Android Bluetooth settings for pairing. This app does not"
                          + " request nearby-device or location access."
                      : "")
              : "Unavailable on this device";
        case "Network":
          return "Since device boot\nReceived "
              + bytes(TrafficStats.getTotalRxBytes())
              + " · Sent "
              + bytes(TrafficStats.getTotalTxBytes())
              + (detailed
                  ? "\n"
                      + "OS traffic counters across supported interfaces, not instantaneous speed"
                      + " or Wi-Fi-only usage. No packet inspection."
                  : "");
        case "Ports & Inputs":
          UsbManager usb = (UsbManager) getSystemService(USB_SERVICE);
          int usbs = usb == null ? 0 : usb.getDeviceList().size();
          String str = "Visible USB devices: " + usbs + "\nTV input controls: ";
          if (!tv()) return str + "Unavailable on this device";
          TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
          if (tim == null) return str + "Unavailable on this device";
          List<TvInputInfo> ins = tim.getTvInputList();
          str += ins.size() + " inputs exposed by Android";
          if (detailed) for (TvInputInfo in : ins) str += "\n" + in.loadLabel(this);
          return str
              + (detailed
                  ? "\nHDMI switching is not implemented; no privileged access is claimed."
                  : "");
        case "System":
          return Build.MANUFACTURER
              + " "
              + Build.MODEL
              + "\nAndroid "
              + Build.VERSION.RELEASE
              + " · API "
              + Build.VERSION.SDK_INT
              + (detailed
                  ? "\nFlow TV 0.3.0 · "
                      + (tv() ? "TV" : "Phone/tablet")
                      + "\nBrand: "
                      + Build.BRAND
                      + "\nApp is a normal launchable activity, not your default home launcher."
                  : "");
      }
    } catch (SecurityException e) {
      return "Unavailable: Android restricts access";
    } catch (Exception e) {
      return "Unavailable on this device";
    }
    return "Unavailable";
  }

  void detail(String key) {
    design.detail(key);
  }

  void settings() {
    design.settings();
  }

  void safeStart(Intent i) {
    try {
      startActivity(i);
    } catch (Exception e) {
      toast("Unavailable on this device");
    }
  }

  void openSettings(String action) {
    safeStart(new Intent(action));
  }

  void settingsSection(String s) {
    if (s.equals("Privacy")) {
      design.privacy();
      return;
    }
    if (s.equals("Network")) {
      detail("Wi-Fi");
      return;
    }
    if (s.equals("Sound") || s.equals("Display")) {
      design.soundDisplay(s.equals("Display"));
      return;
    }
    if (s.equals("Inputs")) {
      detail("Ports & Inputs");
      return;
    }
    LinearLayout p = panel(s);
    switch (s) {
      case "Network":
        addButton(p, "Android Wi-Fi", () -> openSettings(Settings.ACTION_WIFI_SETTINGS));
        addButton(p, "Android Bluetooth", () -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS));
        break;
      case "Sound":
        hint(
            p,
            "Controls the phone/TV's supported Android sound settings. Flow's audio volume slider"
                + " controls sample playback only.");
        addButton(p, "Android sound settings", () -> openSettings(Settings.ACTION_SOUND_SETTINGS));
        break;
      case "Display":
        hint(
            p,
            "App appearance: Minimal Glass, navy/slate, mint focus and lavender accents. Solid"
                + " surfaces; no blur or decorative animation.\n"
                + "System brightness/picture settings are separate; TV picture controls may be"
                + " unavailable.");
        addButton(
            p, "Android display settings", () -> openSettings(Settings.ACTION_DISPLAY_SETTINGS));
        addButton(
            p,
            "TV Preview: " + (preview ? "On" : "Off"),
            () -> {
              preview = !preview;
              prefs.edit().putBoolean("preview", preview).apply();
              setRequestedOrientation(
                  preview
                      ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                      : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
              build();
            });
        break;
      case "Apps":
        addButton(p, "Installed apps", () -> navigate("Apps"));
        addButton(
            p,
            "Android app settings",
            () -> openSettings(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        break;
      case "Inputs":
        p.addView(text(metric("Ports & Inputs", true), 16, TEXT));
        hint(
            p,
            "TV switching: Unavailable on this device / not implemented. This demo never pretends"
                + " to switch HDMI.");
        break;
      case "Privacy":
        hint(
            p,
            "No ads, analytics, tracking SDKs or network requests in Flow TV. Favourites and"
                + " watchlist stay locally. No microphone permission. No background"
                + " playback/service. Flow cannot control tracking in other apps or TV firmware.");
        addButton(
            p,
            "Clear local history…",
            () ->
                confirm(
                    "Clear local history?",
                    "There is no viewing history stored. The audio queue will be cleared;"
                        + " favourites and watchlist stay.",
                    () -> {
                      queue.clear();
                      toast("Local history and queue cleared");
                    }));
        addButton(
            p,
            "Delete Flow app data…",
            () ->
                confirm(
                    "Delete all Flow data?",
                    "Deletes favourites, ordering, watchlist and preferences. Stops audio. Cannot"
                        + " be undone.",
                    () -> {
                      if (player != null) {
                        player.release();
                        player = null;
                      }
                      prefs.edit().clear().apply();
                      queue.clear();
                      favorites.clear();
                      watchlist.clear();
                      preview = false;
                      remote = true;
                      track = 0;
                      page = "Home";
                      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                      build();
                      toast("Flow data deleted");
                    }));
        break;
      case "Accessibility":
        hint(
            p,
            "System text scaling, high-contrast mint focus outlines, TalkBack labels,"
                + " keyboard/D-pad and touch. TV Preview scales the reference layout. The optional"
                + " remote has 48dp touch buttons. No motion effects.");
        addButton(
            p, "Android accessibility", () -> openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        break;
      case "System":
        p.addView(text(metric("System", true), 15, TEXT));
        addButton(
            p,
            "Android device information",
            () -> openSettings(Settings.ACTION_DEVICE_INFO_SETTINGS));
        break;
    }
  }

  void confirm(String title, String message, Runnable r) {
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Confirm", (d, w) -> r.run())
        .show();
  }

  void audio() {
    design.audio();
  }

  void prepareTrack(int n) throws Exception {
    if (player != null) {
      player.release();
      player = null;
    }
    track = n;
    android.content.res.AssetFileDescriptor fd = getAssets().openFd(audioFiles[n]);
    MediaPlayer m = new MediaPlayer();
    m.setAudioAttributes(
        new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build());
    m.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
    fd.close();
    m.prepare();
    float v = prefs.getInt("volume", 70) / 100f;
    m.setVolume(v, v);
    m.setOnCompletionListener(
        x -> {
          if (!queue.isEmpty()) startTrack(queue.remove(0));
          else updateAudio();
        });
    player = m;
  }

  void startTrack(int n) {
    try {
      prepareTrack(n);
      player.start();
      updateAudio();
      toast("Playing " + songTitles[n]);
    } catch (Exception e) {
      toast("Audio could not be opened");
    }
  }

  void toggleAudio() {
    try {
      if (player == null) startTrack(track);
      else if (player.isPlaying()) player.pause();
      else {
        if (player.getCurrentPosition() >= player.getDuration() - 100) player.seekTo(0);
        player.start();
      }
      updateAudio();
    } catch (Exception e) {
      toast("Audio playback unavailable");
    }
  }

  void nextTrack() {
    startTrack(queue.isEmpty() ? (track + 1) % 3 : queue.remove(0));
  }

  void seek(int delta) {
    if (player != null) {
      player.seekTo(
          Math.max(0, Math.min(player.getDuration(), player.getCurrentPosition() + delta)));
      updateAudio();
    } else toast("Play a sample first");
  }

  String time(int ms) {
    return String.format(Locale.ROOT, "%02d:%02d", ms / 60000, (ms / 1000) % 60);
  }

  void updateAudio() {
    if (!demo()) { if (live != null) live.updateMedia(); return; }
    if (!audioVisible || !page.equals("Audio") || trackStatus == null) return;
    if (queueButton != null) queueButton.setText("Queue (" + queue.size() + ")");
    trackStatus.setText(songTitles[track]);
    if (design != null && design.audioMeta != null)
      design.audioMeta.setText(songArtists[track] + " • " + songAlbums[track]);
    try {
      int dur = player == null ? 30000 : player.getDuration(),
          pos = player == null ? 0 : player.getCurrentPosition();
      progress.setMax(dur);
      progress.setProgress(pos);
      progressLabel.setText(time(pos) + " / " + time(dur));
      playButton.setText(player != null && player.isPlaying() ? "Ⅱ\nPause" : "▶\nPlay");
    } catch (IllegalStateException ignored) {
    }
  }

  void queuePanel() {
    LinearLayout p = panel("Audio queue");
    if (queue.isEmpty()) hint(p, "Queue is empty. Use Search songs → Add to queue.");
    for (int k = 0; k < queue.size(); k++) {
      final int at = k;
      int n = queue.get(k);
      addButton(
          p,
          (k + 1) + ". " + songTitles[n] + " · Remove",
          () -> {
            queue.remove(at);
            queuePanel();
          });
    }
    hint(
        p,
        "Queue stays in memory for this session. Current song continues while you browse or"
            + " search.");
  }

  void searchSongs() {
    design.searchSongs();
  }
}
