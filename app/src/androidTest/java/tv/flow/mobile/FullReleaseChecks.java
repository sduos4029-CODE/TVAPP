package tv.flow.mobile;

import android.content.pm.ActivityInfo;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Full-product regression checks; provider identities are classification fixtures, not installs. */
final class FullReleaseChecks {
  static View description(View view,String prefix){
    if(view.getContentDescription()!=null && view.getContentDescription().toString().startsWith(prefix))return view;
    if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=description(group.getChildAt(i),prefix);if(found!=null)return found;}}
    return null;
  }
  static void run(FlowSmoke t)throws Exception{
    MainActivity a=t.app;
    t.test("video-app recognition includes the user's providers without matching lookalike packages",()->{
      for(String pkg:new String[]{"in.airtel.tv","in.startv.hotstar","com.amazon.avod.thirdpartyclient","com.amazon.amazonvideo.livingroom"})t.check(LiveFlow.isVideoProvider(pkg),"Provider identity missing");
      t.check(!LiveFlow.isVideoProvider("in.airtel.tv.evil") && !LiveFlow.isVideoProvider("com.google.android.apps.youtube.music"),"Unrelated package treated as a video catalogue");
    });
    t.test("provider cards only include actual installed launchable apps and their supplied icons",()->{
      for(MainActivity.AppEntry entry:a.live.providers()){
        t.check(a.findApp(entry.pkg)==entry && entry.icon!=null,"Invented provider card");
        try{ActivityInfo info=a.getPackageManager().getActivityInfo(entry.intent.getComponent(),0);t.check(info.exported && info.enabled,"Unavailable activity exposed");}catch(Exception e){throw new AssertionError(e);}
      }
    });
    t.test("every browsing tab offers explicitly app-only navigation while public sources are offline",()->{
      for(String page:new String[]{"Home","Movies","Shows","New","Kids"}){
        a.navigate(page);t.check(t.contains(a.content,"Browse in your apps"),"Missing app navigation in "+page);
        t.check(t.contains(a.content,page.equals("Kids")?"not a verified kids section":"not import catalogues"),"App shortcut scope missing");
        Button all=t.find(a.content,"All installed apps");t.check(all!=null,"All Apps action missing");all.performClick();t.check(a.page.equals("Apps") && t.contains(a.content,"Actual installed apps"),"Native app navigation failed");
      }
    });
    t.test("first-use audio is local and an explicit YouTube selection remains respected",()->{
      a.prefs.edit().remove("audioSource").apply();a.navigate("Audio");
      t.check(a.localMode() && t.contains(a.content,"LOCAL AUDIO"),"Working local source is not the default");
      a.prefs.edit().putString("audioSource","youtube").apply();a.navigate("Audio");
      t.check(!a.localMode() && !a.live.play.isEnabled(),"YouTube choice overridden or unsupported control enabled");
      a.prefs.edit().putString("audioSource","local").apply();
    });
    final int[] position=new int[2];
    t.runOnMainSync(()->{a.remote=true;a.navigate("Dashboard");});
    t.waitForIdleSync();
    t.test("production Dashboard exposes actual System details as a remote focus target",()->{
      View system=description(a.content,"System, actual device information");t.check(system!=null && system.requestFocusFromTouch(),"System widget cannot take focus");
      Button ok=t.find(a.getWindow().getDecorView(),"OK");t.check(ok!=null,"On-screen remote missing");
      ok.getLocationOnScreen(position);position[0]+=ok.getWidth()/2;position[1]+=ok.getHeight()/2;
    });
    long now=SystemClock.uptimeMillis();
    MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,position[0],position[1],0);
    MotionEvent up=MotionEvent.obtain(now,now+60,MotionEvent.ACTION_UP,position[0],position[1],0);
    down.setSource(InputDevice.SOURCE_TOUCHSCREEN);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
    try{t.sendPointerSync(down);t.sendPointerSync(up);}finally{down.recycle();up.recycle();}
    t.waitForIdleSync();
    t.test("touching production remote OK opens actual System drawer and Back closes it",()->{
      t.check(a.drawer!=null && t.contains(a.drawer,android.os.Build.MODEL) && t.contains(a.drawer,a.appVersion()),"System drawer is not device-backed");
      t.check(a.drawer.getWidth()<=a.root.getWidth()*.36f,"Landscape settings panel too wide");
      a.sendRemote(KeyEvent.KEYCODE_BACK);t.check(a.drawer==null,"Remote Back did not close System drawer");
      a.remote=false;a.navigate("Dashboard");
    });
  }
  static void capture(FlowSmoke t)throws Exception{
    MainActivity a=t.app;
    t.runOnMainSync(()->{a.prefs.edit().putString("audioSource","local").apply();a.remote=false;});
    t.screenshot("v5-01-dashboard",()->a.navigate("Dashboard"));
    t.screenshot("v5-02-home",()->a.navigate("Home"));
    t.screenshot("v5-03-movies",()->a.navigate("Movies"));
    t.screenshot("v5-04-shows",()->a.navigate("Shows"));
    t.screenshot("v5-05-new",()->a.navigate("New"));
    t.screenshot("v5-06-kids",()->a.navigate("Kids"));
    t.screenshot("v5-07-my-list",()->a.navigate("My list"));
    t.screenshot("v5-08-apps",()->a.navigate("Apps"));
    t.screenshot("v5-09-settings",()->{a.navigate("Home");a.settings();});
    t.screenshot("v5-10-device-details",()->{a.navigate("Dashboard");a.detail("RAM");});
    t.screenshot("v5-11-local-audio",()->a.navigate("Audio"));
    t.screenshot("v5-12-song-search",()->{a.navigate("Audio");a.live.localSearch();});
    t.screenshot("v5-13-youtube",()->{a.prefs.edit().putString("audioSource","youtube").apply();a.navigate("Audio");a.live.playbackLimits();});
    t.screenshot("v5-14-tv-preview-remote",()->{a.remote=true;a.navigate("Dashboard");});
    t.runOnMainSync(()->{a.remote=false;a.preview=false;a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);});
    long end=SystemClock.uptimeMillis()+15000;while(a.landscape() && SystemClock.uptimeMillis()<end)SystemClock.sleep(100);
    t.check(!a.landscape(),"Portrait capture did not rotate");
    t.screenshot("v5-15-portrait-dashboard",()->a.navigate("Dashboard"));
    t.screenshot("v5-16-portrait-home",()->a.navigate("Home"));
    t.screenshot("v5-17-portrait-audio",()->{a.prefs.edit().putString("audioSource","local").apply();a.navigate("Audio");});
  }
}
