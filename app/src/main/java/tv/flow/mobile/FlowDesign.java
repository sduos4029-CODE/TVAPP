package tv.flow.mobile;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.util.*;

/**
 * Native view implementation of the supplied visual references. Artwork is separate from controls.
 */
final class FlowDesign {
  final MainActivity a;
  TextView audioMeta;
  GlassDashboard glassDashboard;
  static Bitmap atlas, heroes, kidsArt;
  static Typeface dots;
  static final int BG = 0xff03111f,
      CARD = 0xff15263b,
      LINE = 0xff30465b,
      TEXT = 0xfff4f7fc,
      MUTED = 0xffa8bad0,
      MINT = 0xff7df3d3,
      LAV = 0xffb9a6fa,
      RED = 0xffff3347;
  final HashMap<String, ProgressBar> bars = new HashMap<>();
  final HashMap<String, TextView> readings = new HashMap<>();
  long lastRx = -1, lastTx = -1, lastTime = 0;
  LinearLayout shell;

  FlowDesign(MainActivity activity) {
    a = activity;
    loadArt();
  }

  void loadArt() {
    try {
      if (a.demo() && kidsArt == null) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inPreferredConfig = Bitmap.Config.RGB_565;
        kidsArt = BitmapFactory.decodeStream(a.getAssets().open("kids-artwork.png"), null, o);
      }
    } catch (Exception ignored) {
    }
    try {
      if (a.demo() && atlas == null) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inPreferredConfig = Bitmap.Config.RGB_565;
        atlas = BitmapFactory.decodeStream(a.getAssets().open("artwork-atlas.png"), null, o);
      }
      if (a.demo() && heroes == null) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inPreferredConfig = Bitmap.Config.RGB_565;
        heroes = BitmapFactory.decodeStream(a.getAssets().open("hero-artwork.png"), null, o);
      }
    } catch (Exception ignored) {
    }
    try {
      if (dots == null) dots = Typeface.createFromAsset(a.getAssets(), "fonts/Doto.ttf");
    } catch (Exception ignored) {
      dots = Typeface.MONOSPACE;
    }
  }

  int u(float n) {
    return a.dp(n);
  }

  LinearLayout row() {
    return a.row();
  }

  LinearLayout col() {
    return a.col();
  }

  TextView label(String s, int size, int color) {
    return a.text(s, size, color);
  }

  TextView bold(String s, int size, int color) {
    TextView t = label(s, size, color);
    t.setTypeface(null, Typeface.BOLD);
    return t;
  }

  GradientDrawable solid(int color, int radius) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(color);
    g.setCornerRadius(u(radius));
    return g;
  }

  GradientDrawable border(int color, int strokeColor, int radius, int stroke) {
    GradientDrawable g = solid(color, radius);
    g.setStroke(Math.max(1, u(stroke)), strokeColor);
    return g;
  }

  StateListDrawable outline() {
    StateListDrawable s = new StateListDrawable();
    s.addState(new int[] {android.R.attr.state_focused}, border(Color.TRANSPARENT, MINT, 9, 3));
    s.addState(new int[] {android.R.attr.state_pressed}, border(0x337df3d3, MINT, 9, 2));
    s.addState(new int[] {}, new ColorDrawable(Color.TRANSPARENT));
    return s;
  }

  void focus(View v, Runnable action) {
    v.setFocusable(true);
    v.setDefaultFocusHighlightEnabled(false);
    v.setClickable(true);
    v.setForeground(outline());
    v.setOnFocusChangeListener(
        (w, f) -> {
          if (f) a.lastRemoteFocus = w;
        });
    v.setOnClickListener(w -> action.run());
  }

  Button button(String s, Runnable action) {
    Button b = a.button(s, action);
    b.setMinWidth(u(30));
    b.setMinimumWidth(u(30));
    b.setMinHeight(u(32));
    b.setMinimumHeight(u(32));
    b.setPadding(u(13), u(4), u(13), u(4));
    b.setBackground(a.buttonBackground());
    if(a.page.equals("Audio") && !a.demo()) {
      StateListDrawable background=new StateListDrawable();
      background.addState(new int[]{android.R.attr.state_focused},border(0xff202324,Color.WHITE,9,2));
      background.addState(new int[]{android.R.attr.state_pressed},border(0xff202324,RED,9,2));
      background.addState(new int[]{},solid(0xff191c1d,9));
      b.setBackground(background);b.setForeground(new ColorDrawable(Color.TRANSPARENT));
      b.setTextColor(0xffeeeae0);b.setTypeface(dots);
    }
    return b;
  }

  Button mint(String s, Runnable action) {
    Button b = button(s, action);
    StateListDrawable bg = new StateListDrawable();
    bg.addState(new int[] {android.R.attr.state_focused}, border(MINT, Color.WHITE, 8, 2));
    bg.addState(new int[] {}, solid(MINT, 8));
    b.setBackground(bg);
    b.setTextColor(BG);
    b.setTypeface(null, Typeface.BOLD);
    return b;
  }

  void add(LinearLayout p, View v, int width, int height) {
    p.addView(
        v,
        new LinearLayout.LayoutParams(
            width < 0 ? width : u(width), height < 0 ? height : u(height)));
  }

  void gap(LinearLayout p, int h) {
    a.space(p, h);
  }

  void note(LinearLayout p, String s) {
    TextView t = label(s, 11, MUTED);
    t.setMaxLines(2);
    p.addView(t);
  }

  void section(LinearLayout p, String title, String sub) {
    LinearLayout r = row();
    TextView name = bold(title, 16, TEXT);
    name.setPadding(0, 0, 0, 0);
    r.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
    if (sub != null) {
      TextView caption = label(sub, 10, MUTED);
      caption.setPadding(0, 0, 0, 0);
      r.addView(caption);
    }
    if (!a.landscape() && sub != null) {
      r.removeViewAt(r.getChildCount() - 1);
      p.addView(r, new LinearLayout.LayoutParams(-1, u(21)));
      p.addView(label(sub, 10, MUTED));
    } else p.addView(r, new LinearLayout.LayoutParams(-1, u(21)));
  }

  void pill(LinearLayout p, String title, boolean active, Runnable run) {
    Button b = active ? mint(title, run) : button(title, run);
    b.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(12));
    p.addView(b);
  }

  FrameLayout frame(int color) {
    FrameLayout f = new FrameLayout(a);
    f.setBackground(solid(color, 9));
    f.setClipToOutline(true);
    return f;
  }

  ImageView image(int index) {
    ImageView i = new ImageView(a);
    i.setImageDrawable(new Artwork(index, false));
    i.setScaleType(ImageView.ScaleType.FIT_XY);
    i.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    return i;
  }

  FrameLayout.LayoutParams fp(int w, int h, int gravity) {
    return new FrameLayout.LayoutParams(w < 0 ? w : u(w), h < 0 ? h : u(h), gravity);
  }

  void build() {
    a.audioVisible = false;
    a.metricViews.clear();
    a.metricKeys.clear();
    a.drawer = null;
    a.lastRemoteFocus = null;
    android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
    a.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
    float density = dm.density;
    boolean wide = a.landscape();
    int remoteH = a.remote ? (int) (48 * density) : 0;
    a.designScale =
        wide
            ? Math.min(dm.widthPixels / 960f, (dm.heightPixels - remoteH) / 540f)
            : dm.widthPixels / 420f;
    a.designScale = Math.max(.35f, a.designScale);
    FrameLayout viewport = new FrameLayout(a);
    viewport.setBackgroundColor(Color.BLACK);
    LinearLayout host = col();
    viewport.addView(host, new FrameLayout.LayoutParams(-1, -1));
    FrameLayout canvas = new FrameLayout(a);
    host.addView(canvas, new LinearLayout.LayoutParams(-1, 0, 1));
    a.root = col();
    a.root.setBackgroundColor(BG);
    a.root.setPadding(u(22), u(12), u(22), u(10));
    int width = wide ? u(960) : -1;
    int height = wide ? u(540) : -1;
    canvas.addView(a.root, new FrameLayout.LayoutParams(width, height, Gravity.CENTER));
    a.setContentView(viewport);
    a.hideBars();
    a.root.setAlpha(a.prefs.getFloat("appearance", 1f));
    if (a.page.equals("Audio")) audioHeader();
    else header();
    if (a.demo()) a.root.addView(label("DESIGN PREVIEW • SAMPLE CONTENT • exit in Settings", 10, LAV));
    a.stage = new FrameLayout(a);
    a.root.addView(a.stage, new LinearLayout.LayoutParams(-1, 0, 1));
    ScrollView scroll = new ScrollView(a);
    scroll.setFillViewport(true);
    scroll.setVerticalScrollBarEnabled(false);
    a.content = col();
    a.content.setPadding(u(1), u(6), u(1), u(4));
    scroll.addView(a.content);
    a.stage.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
    switch (a.page) {
      case "Home":
        home();
        break;
      case "Apps":
        allApps();
        break;
      case "Dashboard":
        dashboard();
        break;
      case "Audio":
        audio();
        break;
      case "Kids":
        kids();
        break;
      default:
        catalogue();
    }
    if (a.remote) remote(host, density);
    a.updateAudio();
  }

  void header() {
    LinearLayout h = row();
    LinearLayout brand = row();
    ImageView logo = new ImageView(a);
    logo.setImageResource(R.drawable.ic_flow);
    add(brand, logo, 28, 30);
    TextView name = bold("Flow", 24, TEXT);
    name.setPadding(u(3), 0, 0, 0);
    brand.addView(name);
    brand.addView(label("TV", 23, TEXT));
    add(h, brand, 139, 36);
    LinearLayout actions = a.landscape() ? h : row();
    LinearLayout nav = row();
    Button selected = null;
    for (String s : a.sections) {
      Button b = button(s, () -> a.navigate(s));
      b.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(12));
      b.setPadding(u(10), u(3), u(10), u(3));
      if (s.equals(a.page)) {
        b.setBackground(solid(MINT, 13));
        b.setTextColor(BG);
        b.setTypeface(null, Typeface.BOLD);
        selected = b;
      } else {
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[] {android.R.attr.state_focused}, border(CARD, MINT, 12, 2));
        bg.addState(new int[] {}, solid(Color.TRANSPARENT, 12));
        b.setBackground(bg);
      }
      nav.addView(b);
    }
    HorizontalScrollView n = new HorizontalScrollView(a);
    n.setHorizontalScrollBarEnabled(false);
    n.addView(nav);
    if (a.landscape()) h.addView(n, new LinearLayout.LayoutParams(0, u(40), 1));
    else h.addView(new View(a), new LinearLayout.LayoutParams(0, 1, 1));
    final Button sel = selected;
    if (sel != null)
      n.post(() -> n.smoothScrollTo(Math.max(0, sel.getLeft() - n.getWidth() / 2), 0));
    iconButton(actions, "search", () -> a.openGlobalSearch());
    iconButton(actions, "wifi", () -> a.detail("Wi-Fi"));
    iconButton(actions, "settings", () -> a.settings());
    iconButton(actions, "audio", () -> a.navigate("Audio"));
    iconButton(actions, "remote", () -> previewPanel());
    a.clock =
        label(
            new java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date()),
            12,
            TEXT);
    add(actions, a.clock, 68, 32);
    if (!a.landscape()) h.addView(actions);
    a.root.addView(h, new LinearLayout.LayoutParams(-1, u(44)));
    if (!a.landscape()) a.root.addView(n, new LinearLayout.LayoutParams(-1, u(42)));
  }

  void iconButton(LinearLayout h, String icon, Runnable action) {
    Button b = button("", action);
    b.setContentDescription(icon);
    b.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
    Drawable d = new LineIcon(icon, TEXT);
    d.setBounds(0, 0, u(19), u(19));
    b.setCompoundDrawables(d, null, null, null);
    b.setPadding(u(7), 0, u(4), 0);
    b.setBackground(solid(Color.TRANSPARENT, 8));
    b.setForeground(outline());
    h.addView(b, new LinearLayout.LayoutParams(u(32), u(32)));
  }

  void remote(LinearLayout host, float density) {
    LinearLayout r = row();
    r.setBackgroundColor(0xff0a1421);
    r.setPadding((int) (8 * density), 0, (int) (8 * density), 0);
    String[] names = {"↑", "↓", "←", "→", "OK", "Back", "Hide"};
    int[] codes = {19, 20, 21, 22, 23, 4, 0};
    for (int j = 0; j < names.length; j++) {
      final int k = codes[j];
      Button b = new Button(a);
      b.setAllCaps(false);
      b.setText(names[j]);
      b.setTextSize(13);
      b.setTextColor(TEXT);
      b.setBackground(solid(CARD, 8));
      b.setFocusable(false);
      b.setFocusableInTouchMode(false);
      b.setOnClickListener(
          v -> {
            if (k == 0) {
              a.remote = false;
              a.prefs.edit().putBoolean("remote", false).apply();
              a.build();
            } else a.sendRemote(k);
          });
      if (k == 23)
        b.setOnLongClickListener(
            v -> {
              View target = a.restoreRemoteFocus();
              if (target != null) target.performLongClick();
              return true;
            });
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
      lp.setMargins(
          (int) (2 * density), (int) (3 * density), (int) (2 * density), (int) (3 * density));
      r.addView(b, lp);
    }
    host.addView(r, new LinearLayout.LayoutParams(-1, (int) (48 * density)));
  }

  void previewPanel() {
    LinearLayout p = panel("TV Preview");
    note(
        p,
        "Native controls • "
            + (a.tv() ? "TV device" : "Phone preview")
            + " • No remote TV connected");
    p.addView(
        button(
            a.remote ? "Hide on-screen remote" : "Show on-screen remote",
            () -> {
              a.remote = !a.remote;
              a.prefs.edit().putBoolean("remote", a.remote).apply();
              a.build();
            }));
    p.addView(
        button(
            a.preview ? "Use portrait / auto rotation" : "Landscape TV Preview",
            () -> {
              a.preview = !a.preview;
              a.prefs.edit().putBoolean("preview", a.preview).apply();
              a.setRequestedOrientation(
                  a.preview
                      ? android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                      : android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
              a.build();
            }));
    p.addView(button("All installed apps", () -> a.navigate("Apps")));
  }

  void home() {
    if (!a.demo()) { a.live.home(); return; }
    hero(a.content, a.catalogue[0], false, 168);
    section(a.content, "Favourite Apps", "Installed on this device");
    favoriteRow();
    gap(a.content, 5);
    section(a.content, "Continue Watching", "Sample layout • no viewing history connected");
    LinearLayout r = row();
    int[] idx = {3, 5, 4, 8};
    for (int i : idx) {
      MainActivity.Title t = a.catalogue[i];
      FrameLayout c = landscapeCard(t, 83, true);
      LinearLayout.LayoutParams lp =
          new LinearLayout.LayoutParams(a.landscape() ? 0 : u(205), u(83), a.landscape() ? 1 : 0);
      lp.setMargins(u(3), u(2), u(3), u(2));
      r.addView(c, lp);
    }
    if (a.landscape()) a.content.addView(r);
    else a.horizontal(a.content, r);
    gap(a.content, 3);
    section(a.content, "New on Your Apps", "Illustrative catalogue • providers not connected");
    LinearLayout fresh = row();
    for (int i : new int[] {12, 13, 9, 11, 7, 2}) {
      FrameLayout c = landscapeCard(a.catalogue[i], 75, false);
      LinearLayout.LayoutParams lp =
          new LinearLayout.LayoutParams(a.landscape() ? 0 : u(170), u(75), a.landscape() ? 1 : 0);
      lp.setMargins(u(3), u(2), u(3), u(2));
      fresh.addView(c, lp);
    }
    if (a.landscape()) a.content.addView(fresh);
    else a.horizontal(a.content, fresh);
  }

  void hero(LinearLayout parent, MainActivity.Title title, boolean kids, int height) {
    FrameLayout f = frame(CARD);
    ImageView img = new ImageView(a);
    img.setImageDrawable(new Artwork(kids ? 1 : 0, true));
    f.addView(img, new FrameLayout.LayoutParams(-1, -1));
    View shade = new View(a);
    GradientDrawable gradient =
        new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {0xf503101b, 0xc003101b, 0x0003101b, 0x0003101b});
    shade.setBackground(gradient);
    f.addView(shade, new FrameLayout.LayoutParams(-1, -1));
    LinearLayout copy = col();
    copy.setGravity(Gravity.CENTER_VERTICAL);
    copy.setPadding(u(24), u(8), 0, u(8));
    TextView heading = bold(title.name, kids ? 29 : 31, TEXT);
    if (!kids) heading.setTypeface(Typeface.create("serif", Typeface.BOLD));
    copy.addView(heading);
    copy.addView(
        label(
            (kids ? "Animation • Adventure" : "Adventure • Drama")
                + "  •  2024  •  "
                + (kids ? "U/A 7+" : "U/A 13+"),
            12,
            TEXT));
    TextView synopsis =
        label(
            kids
                ? "Join Leo, a brave little lion, on a magical journey\n"
                    + "to save his friends and the forest."
                : "A group of explorers discover a hidden valley\n"
                    + "where nature still holds its secrets.",
            13,
            TEXT);
    copy.addView(synopsis);
    LinearLayout actions = row();
    Button play = button("▶  Watch now", () -> a.titleDetails(title));
    play.setBackground(solid(kids ? MINT : Color.WHITE, 7));
    play.setTextColor(BG);
    actions.addView(play);
    actions.addView(button(watch(title) ? "✓  My List" : "＋  My List", () -> toggleWatch(title)));
    copy.addView(actions);
    f.addView(copy, fp(a.landscape() ? 450 : 335, -1, Gravity.LEFT));
    TextView sample = label("SAMPLE CATALOGUE", 10, LAV);
    sample.setBackground(solid(0xb003111f, 6));
    FrameLayout.LayoutParams sp = fp(-2, -2, Gravity.TOP | Gravity.RIGHT);
    sp.setMargins(0, u(8), u(10), 0);
    f.addView(sample, sp);
    parent.addView(f, new LinearLayout.LayoutParams(-1, u(height)));
    gap(parent, 5);
  }

  boolean watch(MainActivity.Title t) {
    return a.watchlist.contains(t.name);
  }

  void toggleWatch(MainActivity.Title t) {
    if (watch(t)) a.watchlist.remove(t.name);
    else a.watchlist.add(t.name);
    a.prefs.edit().putStringSet("watchlist", new HashSet<>(a.watchlist)).apply();
    a.toast(watch(t) ? "Added to My list" : "Removed from My list");
    a.build();
  }

  int art(MainActivity.Title t) {
    String n = t.name.toLowerCase(Locale.ROOT);
    if (n.contains("lost horizon")) return 0;
    if (t.kind.equals("Kids")) {
      if (n.contains("space")) return 13;
      if (n.contains("dino")) return 14;
      if (n.contains("mia")) return 15;
      if (n.contains("explorer")) return 16;
      if (n.contains("underwater")) return 17;
      return 12;
    }
    if (n.contains("tomorrow") || n.contains("signal")) return 2;
    if (n.contains("dagger") || n.contains("raitra") || n.contains("shadow code")) return 3;
    if (n.contains("ice") || n.contains("skybound") || n.contains("mountain")) return 4;
    if (n.contains("monsoon") || n.contains("bloom") || n.contains("letters")) return 5;
    if (n.contains("echoes") || n.contains("summer")) return 6;
    if (n.contains("city") || n.contains("forest") || n.contains("night")) return 7;
    if (n.contains("harbor") || n.contains("harbour") || n.contains("good man")) return 8;
    if (n.contains("wild") || n.contains("jungle")) return 9;
    if (n.contains("neon") || n.contains("sky")) return 10;
    if (n.contains("king") || n.contains("desert")) return 11;
    return Math.floorMod(t.name.hashCode(), 12);
  }

  FrameLayout landscapeCard(MainActivity.Title t, int h, boolean meta) {
    FrameLayout f = frame(CARD);
    f.addView(image(art(t)), new FrameLayout.LayoutParams(-1, -1));
    View shade = new View(a);
    shade.setBackground(
        new GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP, new int[] {0xf5020b13, 0x00020b13}));
    f.addView(shade, new FrameLayout.LayoutParams(-1, -1));
    LinearLayout copy = col();
    copy.setPadding(u(8), 0, u(5), u(4));
    TextView name = bold(t.name, 13, TEXT);
    name.setMaxLines(1);
    name.setEllipsize(TextUtils.TruncateAt.END);
    copy.addView(name);
    if (meta) {
      TextView caption = label("Sample preview • " + t.kind, 10, MUTED);
      caption.setMaxLines(1);
      caption.setEllipsize(TextUtils.TruncateAt.END);
      copy.addView(caption);
    }
    f.addView(copy, fp(-1, -2, Gravity.BOTTOM));
    focus(f, () -> a.titleDetails(t));
    f.setContentDescription(t.name + ", illustrative sample");
    return f;
  }

  void favoriteRow() {
    LinearLayout r = row();
    int limit = a.landscape() ? 6 : 4;
    int count = 0;
    for (String pkg : new ArrayList<>(a.favorites)) {
      if (count++ >= limit) break;
      View tile = appTile(a.findApp(pkg), pkg, true);
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(u(125), u(60));
      p.setMargins(u(3), u(1), u(3), u(1));
      r.addView(tile, p);
    }
    Button all = button("▦\nAll Apps", () -> a.navigate("Apps"));
    all.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(13));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(u(125), u(60));
    p.setMargins(u(3), u(1), u(3), u(1));
    r.addView(all, p);
    a.horizontal(a.content, r);
  }

  View appTile(MainActivity.AppEntry entry, String pkg, boolean favorite) {
    LinearLayout tile = col();
    tile.setGravity(Gravity.CENTER);
    tile.setPadding(u(8), u(favorite ? 3 : 8), u(8), u(favorite ? 2 : 5));
    tile.setBackground(solid(CARD, 8));
    tile.setTag(pkg);
    if (entry != null) {
      ImageView icon = new ImageView(a);
      icon.setImageDrawable(entry.icon);
      add(tile, icon, favorite ? 32 : 42, favorite ? 32 : 42);
    } else add(tile, label("?", 28, MUTED), 40, 40);
    TextView name = bold(entry == null ? "Unavailable" : entry.label, favorite ? 11 : 13, TEXT);
    name.setGravity(Gravity.CENTER);
    name.setMaxLines(1);
    name.setEllipsize(TextUtils.TruncateAt.END);
    tile.addView(name, new LinearLayout.LayoutParams(-1, -2));
    focus(
        tile,
        () -> {
          if (a.movingPackage != null) {
            a.movingPackage = null;
            a.saveFavorites();
            a.build();
            a.toast("Favourite order saved");
          } else if (entry != null) a.launchApp(entry);
          else a.toast("App unavailable. Remove it from favourites in All Apps.");
        });
    tile.setOnLongClickListener(
        v -> {
          if (!favorite || entry == null) {
            appOptions(entry, pkg);
            return true;
          }
          a.movingPackage = pkg;
          a.movingOriginal = a.favorites.indexOf(pkg);
          if (v.isInTouchMode())
            v.startDragAndDrop(
                ClipData.newPlainText("Flow favourite", pkg),
                new View.DragShadowBuilder(v),
                pkg,
                0);
          a.toast("Move with arrows, OK saves; Back cancels. Or drag to a tile.");
          return true;
        });
    tile.setOnDragListener(
        (v, e) -> {
          if (e.getLocalState() == null || !e.getLocalState().equals(a.movingPackage)) return false;
          if (e.getAction() == DragEvent.ACTION_DROP) {
            int index = a.favorites.indexOf(pkg);
            String moving = a.movingPackage;
            a.favorites.remove(moving);
            a.favorites.add(Math.max(0, Math.min(index, a.favorites.size())), moving);
            a.movingPackage = null;
            a.saveFavorites();
            a.build();
          }
          return true;
        });
    return tile;
  }

  void appOptions(MainActivity.AppEntry app, String pkg) {
    LinearLayout p = panel(app == null ? "App unavailable" : app.label);
    note(p, "Installed app • its own icon and launch activity");
    p.addView(
        button(
            a.favorites.contains(pkg) ? "★ Remove favourite" : "☆ Add favourite",
            () -> {
              if (a.favorites.contains(pkg)) a.favorites.remove(pkg);
              else a.favorites.add(pkg);
              a.saveFavorites();
              a.build();
            }));
    if (app != null) p.addView(button("Open app", () -> a.launchApp(app)));
    p.addView(
        button(
            "Android app information",
            () ->
                a.safeStart(
                    new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + pkg)))));
  }

  void allApps() {
    a.scanApps();
    ArrayList<MainActivity.AppEntry> ordered = new ArrayList<>();
    for (String pkg : a.favorites) {
      MainActivity.AppEntry entry = a.findApp(pkg);
      if (entry != null) ordered.add(entry);
    }
    for (MainActivity.AppEntry entry : a.apps) if (!ordered.contains(entry)) ordered.add(entry);
    LinearLayout tools = row();
    tools.addView(
        label("Hold OK → Move → OK to save  |  Long press an app for options", 14, MUTED),
        new LinearLayout.LayoutParams(0, -2, 1));
    tools.addView(button("★ Favourites", () -> a.navigate("Home")));
    tools.addView(
        button(
            "↔ Reorder",
            () -> {
              a.navigate("Home");
              a.toast("Hold a favourite tile, then use arrows and OK");
            }));
    a.content.addView(tools);
    note(
        a.content, "Actual installed apps • icons supplied by each app • no illustrated app logos");
    gap(a.content, 8);
    int columns = a.landscape() ? 6 : 3;
    for (int i = 0; i < ordered.size(); i += columns) {
      LinearLayout r = row();
      for (int j = i; j < Math.min(i + columns, ordered.size()); j++) {
        MainActivity.AppEntry app = ordered.get(j);
        View tile = appTile(app, app.pkg, a.favorites.contains(app.pkg));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, u(100), 1);
        lp.setMargins(u(5), u(5), u(5), u(5));
        r.addView(tile, lp);
      }
      for (int j = r.getChildCount(); j < columns; j++)
        r.addView(new View(a), new LinearLayout.LayoutParams(0, 1, 1));
      a.content.addView(r);
    }
    if (a.apps.isEmpty()) note(a.content, "No other launchable apps were found on this device.");
    Button store =
        button(
            "Open Play Store",
            () -> {
              Intent i = a.getPackageManager().getLaunchIntentForPackage("com.android.vending");
              if (i == null) a.toast("Play Store is unavailable on this device");
              else a.safeStart(i);
            });
    a.content.addView(store);
  }

  void filters() {
    LinearLayout r = row();
    if (a.page.equals("New")) {
      pill(
          r,
          "All",
          a.type.equals("All"),
          () -> {
            a.type = "All";
            a.build();
          });
      pill(
          r,
          "Movies",
          a.type.equals("Movie"),
          () -> {
            a.type = "Movie";
            a.build();
          });
      pill(
          r,
          "Shows",
          a.type.equals("Show"),
          () -> {
            a.type = "Show";
            a.build();
          });
    } else
      pill(
          r,
          "Genre  ⌄ " + a.genre,
          false,
          () ->
              a.choose(
                  "Sample genre",
                  new String[] {"All", "Adventure", "Drama", "Romance", "Thriller", "Animation"},
                  v -> {
                    a.genre = v;
                    a.build();
                  }));
    pill(
        r,
        "Language  ⌄ " + a.language,
        false,
        () ->
            a.choose(
                "Language",
                new String[] {"All", "English", "Hindi", "Tamil", "Kannada", "Malayalam", "Telugu"},
                v -> {
                  a.language = v;
                  a.build();
                }));
    pill(
        r,
        "My apps  ⌄ " + a.provider,
        false,
        () ->
            a.choose(
                "Illustrative provider filter",
                new String[] {"All", "Prime Video", "Netflix", "JioHotstar", "ZEE5", "Sony LIV"},
                v -> {
                  a.provider = v;
                  a.build();
                }));
    a.horizontal(a.content, r);
    note(
        a.content,
        "Illustrative sample catalogue • artwork and provider labels do not establish live"
            + " availability");
    gap(a.content, 5);
  }

  String genre(MainActivity.Title t) {
    String name = t.name.toLowerCase(Locale.ROOT);
    if (t.kind.equals("Kids")) return "Animation";
    if (name.contains("lost")
        || name.contains("ice")
        || name.contains("desert")
        || name.contains("wild")) return "Adventure";
    if (name.contains("dagger")
        || name.contains("shadow")
        || name.contains("raitra")
        || name.contains("room")) return "Thriller";
    if (name.contains("bloom")
        || name.contains("monsoon")
        || name.contains("summer")
        || name.contains("echoes")) return "Romance";
    return "Drama";
  }

  ArrayList<MainActivity.Title> filtered() {
    ArrayList<MainActivity.Title> list = new ArrayList<>();
    for (MainActivity.Title t : a.catalogue) {
      if (a.page.equals("New") && t.name.equals("The Lost Horizon")) continue;
      if (a.page.equals("Movies") && !t.kind.equals("Movie")) continue;
      if (a.page.equals("Shows") && !t.kind.equals("Show")) continue;
      if (a.page.equals("My list") && !watch(t)) continue;
      if (!a.page.equals("My list") && t.kind.equals("Kids")) continue;
      if (!a.type.equals("All") && !t.kind.equals(a.type)) continue;
      if (!a.genre.equals("All") && !genre(t).equals(a.genre)) continue;
      if (!a.language.equals("All") && !t.lang.equals(a.language)) continue;
      if (!a.provider.equals("All") && !t.service.equals(a.provider)) continue;
      list.add(t);
    }
    if (a.page.equals("Movies")) {
      java.util.List<String> order =
          Arrays.asList(
              "Monsoon Letters",
              "The Last Harbor",
              "Raitra",
              "The Quiet Forest",
              "City of Echoes",
              "A Different Sky",
              "Nine Summers",
              "The Lost Horizon",
              "The Last Bloom",
              "The River Within",
              "Beyond the Ice",
              "Desert Kings");
      list.sort(
          (left, right) -> {
            int l = order.indexOf(left.name), r = order.indexOf(right.name);
            return Integer.compare(l < 0 ? 99 : l, r < 0 ? 99 : r);
          });
    }
    return list;
  }

  void catalogue() {
    if (!a.demo()) { a.live.catalogue(); return; }
    if (a.page.equals("My list")) {
      LinearLayout filters = row();
      int movies = 0, shows = 0;
      for (MainActivity.Title t : a.catalogue)
        if (watch(t)) {
          if (t.kind.equals("Movie")) movies++;
          else shows++;
        }
      pill(
          filters,
          "All  " + a.watchlist.size(),
          a.type.equals("All"),
          () -> {
            a.type = "All";
            a.build();
          });
      pill(
          filters,
          "Movies  " + movies,
          a.type.equals("Movie"),
          () -> {
            a.type = "Movie";
            a.build();
          });
      pill(
          filters,
          "Shows  " + shows,
          a.type.equals("Show"),
          () -> {
            a.type = "Show";
            a.build();
          });
      a.content.addView(filters);
      note(a.content, "Saved on this device • illustrative catalogue, not a viewing history");
    } else filters();
    ArrayList<MainActivity.Title> list = filtered();
    if (list.isEmpty()) {
      gap(a.content, 60);
      TextView t =
          bold(
              a.page.equals("My list") ? "Your list starts here" : "No matching sample titles",
              26,
              TEXT);
      t.setGravity(Gravity.CENTER);
      a.content.addView(t);
      note(a.content, "Open a title and choose My list, or change your filters.");
      return;
    }
    if (a.page.equals("New")) {
      grid(a.content, list.subList(0, Math.min(8, list.size())), 4, 199, false);
      return;
    }
    if (a.page.equals("My list")) {
      grid(a.content, list, a.landscape() ? 7 : 3, 236, true);
      return;
    }
    int split = Math.min(6, list.size());
    section(a.content, a.page.equals("Shows") ? "New episodes" : "New movies", "›");
    grid(a.content, list.subList(0, split), 6, 217, true);
    if (list.size() > split) {
      gap(a.content, 7);
      section(a.content, a.page.equals("Shows") ? "Discover a series" : "Picked for you", "›");
      grid(a.content, list.subList(split, Math.min(split + 6, list.size())), 6, 160, true);
    }
  }

  void grid(
      LinearLayout parent,
      java.util.List<MainActivity.Title> list,
      int columns,
      int height,
      boolean portrait) {
    if (!a.landscape()) columns = Math.min(columns, 3);
    for (int start = 0; start < list.size(); start += columns) {
      LinearLayout r = row();
      for (int j = start; j < Math.min(start + columns, list.size()); j++) {
        MainActivity.Title t = list.get(j);
        View card = poster(t, height, portrait);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, u(height), 1);
        lp.setMargins(u(4), u(3), u(4), u(3));
        r.addView(card, lp);
      }
      for (int j = r.getChildCount(); j < columns; j++)
        r.addView(new View(a), new LinearLayout.LayoutParams(0, 1, 1));
      parent.addView(r);
    }
  }

  View poster(MainActivity.Title t, int height, boolean portrait) {
    FrameLayout f = frame(0xff071624);
    int imageHeight = portrait ? height - 62 : height - 64;
    ImageView img = image(art(t));
    f.addView(img, fp(-1, imageHeight, Gravity.TOP));
    LinearLayout labels = col();
    labels.setPadding(u(7), u(2), u(4), u(4));
    TextView name = bold(t.name, portrait ? 13 : 15, TEXT);
    name.setMaxLines(1);
    name.setEllipsize(TextUtils.TruncateAt.END);
    labels.addView(name);
    labels.addView(
        label(
            t.lang + "  •  " + (t.kind.equals("Show") ? "Sample episode" : t.minutes + " min"),
            11,
            MUTED));
    TextView provider =
        label(t.service + "  · sample", 11, t.service.equals("Netflix") ? 0xffff3f50 : 0xff61d5ff);
    labels.addView(provider);
    for (int child = 0; child < labels.getChildCount(); child++)
      labels.getChildAt(child).setPadding(0, 0, 0, 0);
    f.addView(labels, fp(-1, 64, Gravity.BOTTOM));
    if (!portrait) {
      TextView badge = bold(t.badge, 10, 0xff14213a);
      badge.setBackground(solid(LAV, 4));
      FrameLayout.LayoutParams lp = fp(-2, -2, Gravity.TOP | Gravity.LEFT);
      lp.setMargins(u(6), u(imageHeight - 25), 0, 0);
      f.addView(badge, lp);
    }
    if (a.page.equals("My list")) {
      Button remove =
          button(
              "×",
              () -> {
                a.watchlist.remove(t.name);
                a.prefs.edit().putStringSet("watchlist", new HashSet<>(a.watchlist)).apply();
                a.build();
              });
      remove.setContentDescription("Remove " + t.name + " from My list");
      remove.setPadding(0, 0, 0, 0);
      f.addView(remove, fp(28, 28, Gravity.TOP | Gravity.RIGHT));
    }
    focus(f, () -> a.titleDetails(t));
    f.setContentDescription(t.name + ", sample " + t.kind);
    return f;
  }

  void kids() {
    if (!a.demo()) { a.live.catalogue(); return; }
    MainActivity.Title lead = null;
    for (MainActivity.Title t : a.catalogue)
      if (t.kind.equals("Kids")) {
        lead = t;
        break;
      }
    if (lead == null) lead = a.catalogue[0];
    hero(a.content, lead, true, 190);
    LinearLayout ages = row();
    ages.addView(bold("Age Group", 15, TEXT));
    for (String age : new String[] {"3 – 5", "6 – 8", "9 – 12"})
      pill(
          ages,
          age,
          age.equals(a.prefs.getString("kidsAge", "3 – 5")),
          () -> {
            a.prefs.edit().putString("kidsAge", age).apply();
            a.build();
          });
    ages.addView(new View(a), new LinearLayout.LayoutParams(0, 1, 1));
    ages.addView(
        button(
            "♙ Parent Controls",
            () -> {
              LinearLayout p = panel("Parental controls");
              note(
                  p,
                  "This is a sample Kids view. Flow TV does not enforce device-wide parental"
                      + " restrictions or a PIN.");
              p.addView(
                  button("Open Android settings", () -> a.openSettings(Settings.ACTION_SETTINGS)));
            }));
    a.content.addView(ages);
    section(a.content, "Popular for Kids", "Illustrative sample programmes");
    ArrayList<MainActivity.Title> kids = new ArrayList<>();
    for (MainActivity.Title t : a.catalogue)
      if (t.kind.equals("Kids")
          && !t.name.equals("Leo and the Magic Forest")
          && !t.name.equals("Little Cloud Club")) kids.add(t);
    String age = a.prefs.getString("kidsAge", "3 – 5");
    if (!age.equals("3 – 5")) {
      java.util.List<String> order =
          age.equals("6 – 8")
              ? Arrays.asList(
                  "Space Rangers",
                  "Dino World",
                  "Mia's World",
                  "The Little Explorer",
                  "Underwater Buddies",
                  "Jungle Friends")
              : Arrays.asList(
                  "The Little Explorer",
                  "Space Rangers",
                  "Dino World",
                  "Underwater Buddies",
                  "Mia's World",
                  "Jungle Friends");
      kids.sort(
          (left, right) ->
              Integer.compare(
                  order.indexOf(left.name) < 0 ? 99 : order.indexOf(left.name),
                  order.indexOf(right.name) < 0 ? 99 : order.indexOf(right.name)));
    }
    grid(a.content, kids, 6, 130, false);
    note(
        a.content,
        "Age groups reorder sample recommendations only; they are not certified ratings or parental"
            + " enforcement.");
  }

  LinearLayout panel(String title) {
    a.closeDrawer();
    a.content.setAlpha(.38f);
    a.content.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    a.content.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    a.drawer = col();
    a.drawer.setPadding(u(15), u(12), u(15), u(10));
    a.drawer.setBackground(border(0xff0d1b2b, LINE, 10, 1));
    int width = u(a.landscape() ? 320 : 410);
    FrameLayout.LayoutParams lp =
        new FrameLayout.LayoutParams(
            Math.min(width, a.stage.getWidth() > 0 ? a.stage.getWidth() : width),
            -1,
            Gravity.RIGHT);
    a.stage.addView(a.drawer, lp);
    LinearLayout header = row();
    header.addView(bold(title, 21, TEXT), new LinearLayout.LayoutParams(0, -2, 1));
    Button close = button("×", () -> a.closeDrawer());
    close.setContentDescription("Close " + title);
    add(header, close, 32, 32);
    a.drawer.addView(header);
    ScrollView scroll = new ScrollView(a);
    scroll.setVerticalScrollBarEnabled(false);
    LinearLayout inside = col();
    scroll.addView(inside);
    a.drawer.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    a.drawer.setFocusableInTouchMode(true);
    a.drawer.requestFocus();
    return inside;
  }

  void titleDetails(MainActivity.Title t) {
    LinearLayout p = panel(t.name);
    FrameLayout art = landscapeCard(t, 130, false);
    p.addView(art, new LinearLayout.LayoutParams(-1, u(130)));
    gap(p, 6);
    p.addView(label(t.lang + " • " + t.kind + " • " + t.minutes + " min", 12, MUTED));
    p.addView(label(t.synopsis, 14, TEXT));
    note(
        p,
        "ILLUSTRATIVE SAMPLE • "
            + t.service
            + " label is illustrative. No live availability or stream connected.");
    LinearLayout actions = row();
    actions.addView(
        mint("▶ Watch", () -> a.toast("Unavailable: no streaming catalogue is connected")),
        new LinearLayout.LayoutParams(0, u(36), 1));
    actions.addView(
        button("▶ Trailer", () -> a.toast("Trailer integration unavailable")),
        new LinearLayout.LayoutParams(0, u(36), 1));
    p.addView(actions);
    p.addView(
        button(
            watch(t) ? "✓ Remove from My list" : "＋ My list",
            () -> {
              if (watch(t)) a.watchlist.remove(t.name);
              else a.watchlist.add(t.name);
              a.prefs.edit().putStringSet("watchlist", new HashSet<>(a.watchlist)).apply();
              titleDetails(t);
            }));
    p.addView(
        button(
            "▦ More like this",
            () -> {
              a.closeDrawer();
              a.type = "All";
              a.language = "All";
              a.provider = "All";
              a.navigate(t.kind.equals("Show") ? "Shows" : "Movies");
            }));
  }

  void dashboard() {
    glassDashboard=new GlassDashboard(a,this);
    glassDashboard.render();
  }

  LinearLayout metricCard(String key) {
    LinearLayout c = col();
    c.setPadding(u(16), u(10), u(14), u(10));
    c.setBackground(border(0xff102131, 0xff293e50, 18, 1));
    LinearLayout title = row();
    ImageView icon = new ImageView(a);
    icon.setImageDrawable(new LineIcon(key.toLowerCase(Locale.ROOT), key.equals("Storage") ? LAV : MINT));
    add(title, icon, 24, 24);
    TextView label = label(key.equals("Network") ? "Network" : key, 13, MUTED);
    label.setPadding(u(10), 0, 0, 0);
    title.addView(label);
    c.addView(title);
    TextView value = label("", key.equals("CPU") || key.equals("Ports & Inputs") ? 13 : 15, TEXT);
    value.setPadding(u(4), u(6), 0, 0);
    c.addView(value);
    a.metricKeys.add(key);
    a.metricViews.add(value);
    readings.put(key, value);
    if (key.equals("RAM") || key.equals("Storage")) {
      ProgressBar b = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
      b.setMax(1000);
      b.setProgressTintList(ColorStateList.valueOf(key.equals("RAM") ? MINT : LAV));
      b.setProgressBackgroundTintList(ColorStateList.valueOf(0xff364d67));
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, u(9));
      lp.setMargins(u(6), u(9), u(6), 0);
      c.addView(b, lp);
      bars.put(key, b);
    }
    focus(c, () -> a.detail(key));
    c.setContentDescription(key + ", actual device reading, open details");
    return c;
  }

  void refreshMetrics() {
    if(glassDashboard!=null){glassDashboard.refresh();return;}
    for (Map.Entry<String, TextView> entry : readings.entrySet()) {
      String key = entry.getKey();
      TextView view = entry.getValue();
      try {
        if (key.equals("RAM")) {
          ActivityManager.MemoryInfo m = new ActivityManager.MemoryInfo();
          ((ActivityManager) a.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(m);
          long used = m.totalMem - m.availMem;
          view.setText(
              a.bytes(used)
                  + " / "
                  + a.bytes(m.totalMem)
                  + "\n"
                  + a.bytes(m.availMem)
                  + " available");
          bars.get(key).setProgress((int) (used * 1000d / m.totalMem));
        } else if (key.equals("Storage")) {
          StatFs fs = new StatFs(a.getFilesDir().getAbsolutePath());
          long used = fs.getTotalBytes() - fs.getAvailableBytes();
          view.setText(
              a.bytes(used)
                  + " / "
                  + a.bytes(fs.getTotalBytes())
                  + "\nData volume • "
                  + a.bytes(fs.getAvailableBytes())
                  + " free");
          bars.get(key).setProgress((int) (used * 1000d / fs.getTotalBytes()));
        } else if (key.equals("CPU")) {
          String cpu =
              Build.VERSION.SDK_INT >= 31 && !Build.SOC_MODEL.equalsIgnoreCase("unknown")
                  ? Build.SOC_MODEL
                  : Build.HARDWARE;
          view.setText(
              cpu
                  + "\n"
                  + Build.SUPPORTED_ABIS[0]
                  + " • "
                  + Runtime.getRuntime().availableProcessors()
                  + " available cores");
        } else if (key.equals("Network")) {
          view.setText(
              "↓ "
                  + a.bytes(TrafficStats.getTotalRxBytes())
                  + " received\n↑ "
                  + a.bytes(TrafficStats.getTotalTxBytes())
                  + " sent\nActual totals since boot");
        } else view.setText(a.metric(key, false));
        if (key.equals("RAM") || key.equals("Storage")) {
          String message = view.getText().toString();
          int end = message.indexOf('\n');
          if (end > 0) {
            android.text.SpannableString styled = new android.text.SpannableString(message);
            styled.setSpan(
                new android.text.style.RelativeSizeSpan(1.28f),
                0,
                end,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            view.setText(styled);
          }
        }
      } catch (Exception e) {
        view.setText("Unavailable on this device");
      }
    }
  }

  void settings() {
    LinearLayout p = panel("Quick settings");
    p.addView(button("Connected app content",()->new SharedContentScreen(a,this).sources()));
    for (String[] item :
        new String[][] {
          {"wifi", "Network"},
          {"audio", "Sound"},
          {"display", "Display"},
          {"apps", "Apps"},
          {"ports", "Inputs"},
          {"privacy", "Privacy"},
          {"accessibility", "Accessibility"},
          {"settings", "System"}
        }) {
      Button b = button(item[1] + "                      ›", () -> a.settingsSection(item[1]));
      Drawable icon = new LineIcon(item[0], LAV);
      icon.setBounds(0, 0, u(20), u(20));
      b.setCompoundDrawables(icon, null, null, null);
      b.setCompoundDrawablePadding(u(11));
      b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
      p.addView(b, new LinearLayout.LayoutParams(-1, u(41)));
      gap(p, 4);
    }
    note(
        p,
        "Real Android settings open only through supported system intents. No privileged TV"
            + " controls.");
  }

  void detail(String key) {
    LinearLayout p = panel(key.equals("Ports & Inputs") ? "Ports & Inputs" : key);
    TextView chip = label(a.tv() ? "Device data" : "Phone data", 10, MINT);
    p.addView(chip);
    p.addView(label(a.metric(key, true), 15, TEXT));
    gap(p, 10);
    if (key.equals("Wi-Fi")) {
      note(
          p,
          "Nearby networks and SSIDs are not read without the relevant permissions. No sample Wi-Fi"
              + " networks are presented as real.");
      p.addView(
          mint(
              "↗ Open device Wi-Fi settings", () -> a.openSettings(Settings.ACTION_WIFI_SETTINGS)));
    }
    if (key.equals("Bluetooth"))
      p.addView(
          mint(
              "↗ Open Bluetooth settings",
              () -> a.openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)));
    if (key.equals("Ports & Inputs")) {
      p.addView(
          button(
              a.tv() ? "↗ Open TV inputs" : "TV inputs — unavailable on this device",
              () -> {
                if (a.tv()) a.openSettings("android.settings.TV_INPUT_SETTINGS");
                else a.toast("TV input controls are unavailable on this phone");
              }));
      p.addView(
          button(
              "▱ Browse files",
              () ->
                  a.safeStart(
                      new Intent(Intent.ACTION_OPEN_DOCUMENT)
                          .addCategory(Intent.CATEGORY_OPENABLE)
                          .setType("*/*"))));
      note(
          p,
          "No HDMI switching or tuner state is invented. Only accessible Android input and USB"
              + " information is shown.");
    }
  }

  void privacy() {
    LinearLayout p = panel("Privacy");
    note(p, "Flow controls only • other apps and TV firmware have separate privacy settings");
    for (String[] row :
        new String[][] {
          {"Ads & trackers", "None in Flow TV"},
          {"Microphone", "Not requested • voice unavailable"},
          {"Recommendations", a.demo() ? "Test-only samples" : "OS-shared TV programme cards"},
          {"Full app catalogues", "Private · not exposed by login"},
          {"Notification access", "Not requested · no cross-app media access"},
          {"Background work", "Flow polling stops when inactive"}
        }) {
      Button b = button(row[0] + "\n" + row[1], () -> a.toast(row[0] + ": " + row[1]));
      b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
      p.addView(b, new LinearLayout.LayoutParams(-1, u(49)));
      gap(p, 4);
    }
    p.addView(
        button(
            "◷ Clear local history…",
            () ->
                a.confirm(
                    "Clear local history?",
                    "Clears apps opened through Flow and the local audio queue; favourites and"
                        + " watchlist stay.",
                    () -> {
                      a.queue.clear();
                      if(a.localAudio!=null)a.localAudio.queue.clear();
                      a.prefs.edit().remove("recentApps").apply();
                      a.toast("Local queue cleared");
                    })));
    p.addView(
        button(
            "▥ Delete Flow data…",
            () ->
                a.confirm(
                    "Delete Flow data?",
                    "Deletes local favourites, lists, audio imports and preferences. Stops local playback."
                        + " This cannot be undone.",
                    () -> {
                      if(a.localAudio!=null)a.localAudio.stopPlayback();
                      for(android.content.UriPermission permission:a.getContentResolver().getPersistedUriPermissions()) {
                        try{a.getContentResolver().releasePersistableUriPermission(permission.getUri(),Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}
                      }
                      if (a.player != null) {
                        a.player.release();
                        a.player = null;
                      }
                      a.prefs.edit().clear().apply();
                      a.queue.clear();
                      a.favorites.clear();
                      a.watchlist.clear();
                      a.remote = false;
                      a.preview = true;
                      a.track = 0;
                      a.page = "Home";
                      a.build();
                      a.toast("Flow data deleted");
                    })));
  }

  void soundDisplay(boolean display) {
    LinearLayout p = panel("Sound & display");
    LinearLayout tabs = row();
    pill(tabs, "♬ Sound", !display, () -> soundDisplay(false));
    pill(tabs, "▣ Display", display, () -> soundDisplay(true));
    p.addView(tabs);
    if (!display) {
      p.addView(bold(a.demo() ? "Sample player volume" : "Device media volume", 14, TEXT));
      AudioManager audioManager = (AudioManager) a.getSystemService(Context.AUDIO_SERVICE);
      SeekBar v = new SeekBar(a);
      v.setMax(a.demo() ? 100 : audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
      v.setProgress(a.demo() ? a.prefs.getInt("volume", 70) : audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
      v.setProgressTintList(ColorStateList.valueOf(MINT));
      v.setOnSeekBarChangeListener(
          new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar s) {}

            public void onStopTrackingTouch(SeekBar s) {}

            public void onProgressChanged(SeekBar s, int n, boolean user) {
              if (user) {
                if (!a.demo()) {
                  try { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, n, 0); }
                  catch (SecurityException e) { a.toast("Use device sound settings"); }
                  return;
                }
                a.prefs.edit().putInt("volume", n).apply();
                if (a.player != null) a.player.setVolume(n / 100f, n / 100f);
              }
            }
          });
      p.addView(v);
      note(
          p,
          a.demo() ? "Changes sample playback only." : "Changes Android media volume where supported. Output routing is controlled in Android settings.");
      p.addView(
          button(
              "↗ Open device sound settings",
              () -> a.openSettings(Settings.ACTION_SOUND_SETTINGS)));
    }
    gap(p, 10);
    p.addView(bold("App appearance", 15, TEXT));
    note(p, "Brightness of Flow's interface only — not system brightness or TV picture settings.");
    SeekBar appearance = new SeekBar(a);
    appearance.setMax(50);
    appearance.setProgress((int) (a.prefs.getFloat("appearance", 1f) * 100) - 50);
    appearance.setProgressTintList(ColorStateList.valueOf(LAV));
    appearance.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onStartTrackingTouch(SeekBar s) {}

          public void onStopTrackingTouch(SeekBar s) {}

          public void onProgressChanged(SeekBar s, int n, boolean user) {
            if (user) {
              float value = (n + 50) / 100f;
              a.root.setAlpha(value);
              a.prefs.edit().putFloat("appearance", value).apply();
            }
          }
        });
    p.addView(appearance);
    p.addView(
        button(
            "↗ Open device display settings",
            () -> a.openSettings(Settings.ACTION_DISPLAY_SETTINGS)));
    note(p, "TV picture, screen-off and output controls depend on supported device capabilities.");
  }

  void audioHeader() {
    LinearLayout header = row();
    TextView brand = label("Flow TV", 26, 0xffeee8d0);
    brand.setTypeface(dots);
    add(header, brand, 150, 38);
    LinearLayout actions = a.landscape() ? header : row();
    if (a.landscape())
      header.addView(
          label("│  ♬ AUDIO PLAYER", 16, 0xffeee8d0), new LinearLayout.LayoutParams(0, -2, 1));
    else {
      header.addView(new View(a), new LinearLayout.LayoutParams(0, 1, 1));
      actions.addView(
          label("♬ AUDIO PLAYER", 14, 0xffeee8d0), new LinearLayout.LayoutParams(0, -2, 1));
    }
    actions.addView(button("⌕ Search songs", () -> { if (a.demo()) a.searchSongs(); else a.live.search(true); }));
    actions.addView(button("⚙ Settings", () -> a.settings()));
    Button mode = button("▦", () -> previewPanel());
    mode.setContentDescription("TV Preview and remote controls");
    header.addView(mode);
    header.addView(button("⌂", () -> a.navigate("Home")));
    a.clock =
        label(
            new java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date()),
            12,
            0xffeee8d0);
    add(header, a.clock, 70, 32);
    a.root.setBackgroundColor(0xff080a0b);
    a.root.addView(header, new LinearLayout.LayoutParams(-1, u(48)));
    if (!a.landscape()) a.root.addView(actions, new LinearLayout.LayoutParams(-1, u(40)));
  }

  void audio() {
    if (!a.demo()) { a.live.audio(); return; }
    a.audioVisible = true;
    int cream = 0xffefe8cd;
    LinearLayout deck = col();
    deck.setPadding(u(6), u(15), u(6), u(6));
    LinearLayout track = a.landscape() ? row() : col();
    FrameLayout art = frame(Color.BLACK);
    art.addView(image(10), new FrameLayout.LayoutParams(-1, -1));
    art.setForeground(new DotBorder(0xffd8d0aa));
    track.addView(
        art,
        new LinearLayout.LayoutParams(a.landscape() ? u(218) : -1, u(a.landscape() ? 218 : 160)));
    LinearLayout info = col();
    info.setPadding(u(a.landscape() ? 27 : 4), u(5), u(8), 0);
    TextView sample = bold("BUNDLED SAMPLE MEDIA", 11, RED);
    info.addView(sample);
    a.trackStatus = label("", 32, cream);
    a.trackStatus.setTypeface(dots);
    info.addView(a.trackStatus);
    audioMeta = label("", 15, cream);
    audioMeta.setTypeface(Typeface.MONOSPACE);
    info.addView(audioMeta);
    info.addView(label("Original instrumental samples • offline playback", 13, cream));
    gap(info, 20);
    a.progress = new SeekBar(a);
    a.progress.setProgressTintList(ColorStateList.valueOf(RED));
    a.progress.setThumbTintList(ColorStateList.valueOf(cream));
    a.progress.setProgressBackgroundTintList(ColorStateList.valueOf(cream));
    a.progress.setContentDescription("Audio playback position");
    a.progress.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onStartTrackingTouch(SeekBar s) {}

          public void onStopTrackingTouch(SeekBar s) {}

          public void onProgressChanged(SeekBar s, int value, boolean user) {
            if (user && a.player != null) a.player.seekTo(value);
          }
        });
    info.addView(a.progress, new LinearLayout.LayoutParams(-1, u(24)));
    a.progressLabel = label("00:00 / 00:30", 14, cream);
    a.progressLabel.setTypeface(Typeface.MONOSPACE);
    info.addView(a.progressLabel);
    a.queueButton = button("Queue (" + a.queue.size() + ")", () -> a.queuePanel());
    info.addView(a.queueButton);
    track.addView(
        info,
        a.landscape()
            ? new LinearLayout.LayoutParams(0, -2, 1)
            : new LinearLayout.LayoutParams(-1, -2));
    deck.addView(track);
    gap(deck, 15);
    LinearLayout controls = row();
    audioControl(controls, "|◀\nPrevious", () -> a.startTrack((a.track + 2) % 3), false);
    audioControl(controls, "↶ 10\nRewind 10s", () -> a.seek(-10000), false);
    a.playButton = audioControl(controls, "▶ Play", () -> a.toggleAudio(), true);
    audioControl(controls, "10 ↷\nForward 10s", () -> a.seek(10000), false);
    audioControl(controls, "▶|\nNext", () -> a.nextTrack(), false);
    deck.addView(controls);
    gap(deck, 16);
    LinearLayout footer = row();
    footer.addView(label("♬  Volume", 14, cream));
    SeekBar v = new SeekBar(a);
    v.setMax(100);
    v.setProgress(a.prefs.getInt("volume", 70));
    v.setProgressTintList(ColorStateList.valueOf(RED));
    v.setThumbTintList(ColorStateList.valueOf(cream));
    v.setContentDescription("Sample player volume");
    final TextView volumeNumber = label(a.prefs.getInt("volume", 70) + "%", 13, cream);
    v.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onStartTrackingTouch(SeekBar s) {}

          public void onStopTrackingTouch(SeekBar s) {}

          public void onProgressChanged(SeekBar s, int n, boolean user) {
            if (user) {
              volumeNumber.setText(n + "%");
              a.prefs.edit().putInt("volume", n).apply();
              if (a.player != null) a.player.setVolume(n / 100f, n / 100f);
            }
          }
        });
    footer.addView(v, new LinearLayout.LayoutParams(a.landscape() ? u(240) : u(210), u(32)));
    footer.addView(volumeNumber);
    if (a.landscape())
      footer.addView(
          button(
              "Hide video · unavailable",
              () -> a.toast("These are audio-only samples; no video integration exists")));
    if (a.landscape())
      footer.addView(
          button(
              "Screen off · unavailable",
              () ->
                  a.toast(
                      "Screen-off/background playback is not implemented. A black UI is not a"
                          + " powered-off display.")));
    deck.addView(footer);
    note(
        deck,
        "Audio pauses when Flow leaves the foreground. YouTube, video hiding and screen-off"
            + " playback are not integrated.");
    a.content.addView(deck);
    a.updateAudio();
  }

  Button audioControl(LinearLayout row, String label, Runnable action, boolean primary) {
    String display =
        a.landscape()
            ? label
            : label
                .replace("Previous", "Prev")
                .replace("Rewind 10s", "−10s")
                .replace("Forward 10s", "+10s");
    Button b = button(display, action);
    b.setContentDescription(label.replace('\n', ' '));
    if (!a.landscape()) b.setPadding(u(3), u(2), u(3), u(2));
    b.setTypeface(Typeface.MONOSPACE);
    b.setTextColor(primary ? RED : 0xffefe8cd);
    b.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(a.landscape() ? 16 : 14));
    b.setBackground(solid(0xff080a0b, 3));
    StateListDrawable focusStates = new StateListDrawable();
    focusStates.addState(
        new int[] {android.R.attr.state_focused}, border(Color.TRANSPARENT, MINT, 3, 3));
    focusStates.addState(
        new int[] {android.R.attr.state_pressed}, border(Color.TRANSPARENT, RED, 3, 2));
    focusStates.addState(new int[] {}, new DotBorder(primary ? RED : 0xffc8be98));
    b.setForeground(focusStates);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, u(a.landscape() ? 86 : 70), 1);
    lp.setMargins(u(5), 0, u(5), 0);
    row.addView(b, lp);
    return b;
  }

  void searchSongs() {
    LinearLayout p = panel("Search songs");
    a.drawer.setBackground(border(0xff080b10, LINE, 9, 1));
    EditText q = new EditText(a);
    q.setSingleLine(true);
    q.setHint("Song, artist or album");
    q.setTextColor(TEXT);
    q.setHintTextColor(MUTED);
    q.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(14));
    q.setPadding(u(9), 0, u(9), 0);
    q.setBackground(border(0xff0d1828, LINE, 5, 1));
    q.setShowSoftInputOnFocus(false);
    p.addView(q, new LinearLayout.LayoutParams(-1, u(32)));
    note(p, "Local bundled audio • YouTube not connected");
    LinearLayout results = col();
    p.addView(results);
    final int[] selected = {0};
    LinearLayout actions = row();
    Button play = mint("▶ Play now", () -> a.startTrack(selected[0]));
    Button add =
        button(
            "＋ Add to queue",
            () -> {
              a.queue.add(selected[0]);
              a.toast(a.songTitles[selected[0]] + " added to queue");
              a.updateAudio();
            });
    actions.addView(play, new LinearLayout.LayoutParams(0, u(34), 1));
    actions.addView(add, new LinearLayout.LayoutParams(0, u(34), 1));
    p.addView(actions);
    Runnable render =
        () -> {
          results.removeAllViews();
          String query = q.getText().toString().toLowerCase(Locale.ROOT).trim();
          int count = 0;
          for (int i = 0; i < 3; i++) {
            final int n = i;
            if (!(a.songTitles[i] + " " + a.songArtists[i] + " " + a.songAlbums[i])
                .toLowerCase(Locale.ROOT)
                .contains(query)) continue;
            if (count++ == 0) selected[0] = n;
            LinearLayout item = row();
            item.setPadding(u(5), u(4), u(5), u(4));
            item.setBackground(solid(0xff111b28, 5));
            ImageView thumb = image(i == 0 ? 10 : i == 1 ? 7 : 5);
            add(item, thumb, 47, 43);
            LinearLayout copy = col();
            copy.addView(bold(a.songTitles[n], 13, TEXT));
            copy.addView(label(a.songArtists[n] + " • 00:30", 10, MUTED));
            copy.addView(label("Flow Originals • bundled", 9, MUTED));
            for (int child = 0; child < copy.getChildCount(); child++)
              copy.getChildAt(child).setPadding(0, 0, 0, 0);
            item.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
            focus(
                item,
                () -> {
                  selected[0] = n;
                  for (int child = 0; child < results.getChildCount(); child++)
                    results.getChildAt(child).setForeground(outline());
                  item.setForeground(border(Color.TRANSPARENT, MINT, 5, 2));
                  a.toast("Selected " + a.songTitles[n]);
                });
            if (count == 1) item.setForeground(border(Color.TRANSPARENT, MINT, 5, 2));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, u(56));
            lp.setMargins(0, u(3), 0, u(3));
            results.addView(item, lp);
          }
          if (count == 0) note(results, "No local results");
          play.setEnabled(count > 0);
          add.setEnabled(count > 0);
        };
    q.addTextChangedListener(a.watcher(render));
    render.run();
    gap(p, 9);
    for (String line : new String[] {"qwertyuiop", "asdfghjkl", "zxcvbnm"}) {
      LinearLayout keys = row();
      for (char c : line.toCharArray()) {
        String letter = String.valueOf(c);
        Button b = button(letter, () -> q.append(letter));
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(12));
        b.setBackground(border(Color.TRANSPARENT, LINE, 3, 1));
        b.setForeground(outline());
        LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0, u(28), 1);
        keyParams.setMargins(u(1), u(1), u(1), u(1));
        keys.addView(b, keyParams);
      }
      p.addView(keys);
    }
    LinearLayout edit = row();
    pill(edit, "Space", false, () -> q.append(" "));
    pill(
        edit,
        "⌫",
        false,
        () -> {
          if (q.length() > 0) q.getText().delete(q.length() - 1, q.length());
        });
    pill(edit, "Clear", false, () -> q.setText(""));
    p.addView(edit);
    note(
        p,
        "Search keeps the current song playing. Voice is unavailable; no microphone permission"
            + " requested.");
  }

  final class Artwork extends Drawable {
    final int index;
    final boolean hero;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    Artwork(int n, boolean h) {
      index = n;
      hero = h;
    }

    @Override
    public void draw(Canvas c) {
      Rect b = getBounds();
      boolean kid = !hero && index >= 12 && kidsArt != null;
      Bitmap bitmap = hero && heroes != null ? heroes : kid ? kidsArt : atlas;
      if (bitmap == null) {
        c.drawColor(CARD);
        return;
      }
      int columns = hero && heroes != null ? 1 : kid ? 3 : 4,
          rows = hero && heroes != null ? 2 : kid ? 2 : 3;
      int cell =
          hero && heroes == null
              ? (index == 0 ? 0 : 1)
              : kid ? index - 12 : Math.floorMod(index, hero ? 2 : 12);
      float cw = bitmap.getWidth() / (float) columns, ch = bitmap.getHeight() / (float) rows;
      RectF src =
          new RectF(
              (cell % columns) * cw,
              (cell / columns) * ch,
              (cell % columns + 1) * cw,
              (cell / columns + 1) * ch);
      float want = b.width() / (float) Math.max(1, b.height());
      float ratio = src.width() / src.height();
      if (ratio > want) {
        float width = src.height() * want;
        float middle = src.centerX();
        src.left = middle - width / 2;
        src.right = middle + width / 2;
      } else {
        float height = src.width() / want;
        float middle = src.centerY();
        src.top = middle - height / 2;
        src.bottom = middle + height / 2;
      }
      c.drawBitmap(
          bitmap, new Rect((int) src.left, (int) src.top, (int) src.right, (int) src.bottom), b, p);
    }

    @Override
    public void setAlpha(int alpha) {
      p.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter f) {
      p.setColorFilter(f);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.OPAQUE;
    }
  }

  final class DotBorder extends Drawable {
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    DotBorder(int color) {
      p.setColor(color);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(Math.max(1, u(1)));
      p.setPathEffect(new DashPathEffect(new float[] {u(2), u(2)}, 0));
    }

    @Override
    public void draw(Canvas c) {
      Rect b = getBounds();
      c.drawRoundRect(new RectF(b.left + 1, b.top + 1, b.right - 1, b.bottom - 1), u(3), u(3), p);
    }

    @Override
    public void setAlpha(int alpha) {
      p.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter f) {
      p.setColorFilter(f);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }

  final class LineIcon extends Drawable {
    final String type;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    LineIcon(String t, int color) {
      type = t;
      p.setColor(color);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(1.7f);
      p.setStrokeCap(Paint.Cap.ROUND);
      p.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    public int getIntrinsicWidth() {
      return u(22);
    }

    @Override
    public int getIntrinsicHeight() {
      return u(22);
    }

    @Override
    public void draw(Canvas canvas) {
      Rect b = getBounds();
      canvas.save();
      canvas.translate(b.left, b.top);
      canvas.scale(b.width() / 24f, b.height() / 24f);
      switch (type) {
        case "search":
          canvas.drawCircle(10, 10, 6, p);
          canvas.drawLine(14.5f, 14.5f, 21, 21, p);
          break;
        case "wifi":
        case "wi-fi":
          canvas.drawArc(2, 4, 22, 23, 219, 102, false, p);
          canvas.drawArc(6, 9, 18, 21, 220, 100, false, p);
          canvas.drawCircle(12, 19, 1, p);
          break;
        case "bluetooth":
          Path bt = new Path();
          bt.moveTo(12, 2);
          bt.lineTo(19, 8);
          bt.lineTo(6, 18);
          bt.moveTo(12, 22);
          bt.lineTo(19, 16);
          bt.lineTo(6, 6);
          bt.moveTo(12, 2);
          bt.lineTo(12, 22);
          canvas.drawPath(bt, p);
          break;
        case "ram":
        case "cpu":
          canvas.drawRoundRect(6, 6, 18, 18, 2, 2, p);
          canvas.drawRect(9, 9, 15, 15, p);
          for (int n = 8; n <= 16; n += 4) {
            canvas.drawLine(n, 2, n, 5, p);
            canvas.drawLine(n, 19, n, 22, p);
            canvas.drawLine(2, n, 5, n, p);
            canvas.drawLine(19, n, 22, n, p);
          }
          break;
        case "storage":
          canvas.drawOval(4, 3, 20, 10, p);
          canvas.drawArc(4, 10, 20, 17, 0, 180, false, p);
          canvas.drawArc(4, 16, 20, 23, 0, 180, false, p);
          canvas.drawLine(4, 6, 4, 19, p);
          canvas.drawLine(20, 6, 20, 19, p);
          break;
        case "network":
          canvas.drawLine(7, 3, 7, 21, p);
          canvas.drawLine(3, 7, 7, 3, p);
          canvas.drawLine(11, 7, 7, 3, p);
          canvas.drawLine(17, 3, 17, 21, p);
          canvas.drawLine(13, 17, 17, 21, p);
          canvas.drawLine(21, 17, 17, 21, p);
          break;
        case "audio":
          canvas.drawArc(4, 3, 20, 20, 180, 180, false, p);
          canvas.drawRoundRect(3, 10, 7, 20, 1, 1, p);
          canvas.drawRoundRect(17, 10, 21, 20, 1, 1, p);
          break;
        case "ports":
        case "ports & inputs":
          Path hdmi = new Path();
          hdmi.moveTo(3, 6);
          hdmi.lineTo(21, 6);
          hdmi.lineTo(21, 15);
          hdmi.lineTo(17, 19);
          hdmi.lineTo(7, 19);
          hdmi.lineTo(3, 15);
          hdmi.close();
          canvas.drawPath(hdmi, p);
          canvas.drawLine(7, 10, 17, 10, p);
          break;
        case "apps":
        case "remote":
          for (int x = 3; x < 20; x += 11)
            for (int y = 3; y < 20; y += 11) canvas.drawRoundRect(x, y, x + 7, y + 7, 1, 1, p);
          break;
        case "display":
          canvas.drawRoundRect(2, 3, 22, 18, 2, 2, p);
          canvas.drawLine(12, 18, 12, 22, p);
          canvas.drawLine(7, 22, 17, 22, p);
          break;
        case "privacy":
          Path shield = new Path();
          shield.moveTo(12, 2);
          shield.lineTo(21, 6);
          shield.lineTo(20, 15);
          shield.lineTo(12, 22);
          shield.lineTo(4, 15);
          shield.lineTo(3, 6);
          shield.close();
          canvas.drawPath(shield, p);
          break;
        default:
          canvas.drawCircle(12, 12, 6, p);
          for (int j = 0; j < 8; j++) {
            double angle = j * Math.PI / 4;
            canvas.drawLine(
                12 + (float) Math.cos(angle) * 8,
                12 + (float) Math.sin(angle) * 8,
                12 + (float) Math.cos(angle) * 11,
                12 + (float) Math.sin(angle) * 11,
                p);
          }
          break;
      }
      canvas.restore();
    }

    @Override
    public void setAlpha(int n) {
      p.setAlpha(n);
    }

    @Override
    public void setColorFilter(ColorFilter f) {
      p.setColorFilter(f);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }
}
