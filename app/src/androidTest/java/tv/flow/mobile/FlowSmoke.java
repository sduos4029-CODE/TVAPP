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

  @Override
  public void onStart() {
    super.onStart();
    Bundle out = new Bundle();
    try {
      Intent i =
          new Intent(getTargetContext(), MainActivity.class)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getTargetContext()
          .getSharedPreferences("flow", 0)
          .edit()
          .clear()
          .putInt("visualVersion", 2)
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
