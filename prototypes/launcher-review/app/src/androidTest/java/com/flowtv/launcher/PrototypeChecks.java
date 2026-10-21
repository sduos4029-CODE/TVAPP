package com.flowtv.launcher;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.database.MatrixCursor;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import com.flowtv.launcher.data.*;
import java.io.*;
import java.util.*;

/** Runs only in a disposable emulator. TV-card fixtures do not establish actual provider access. */
public final class PrototypeChecks extends Instrumentation {
    int passed;
    Bundle options;
    Activity activity;
    final StringBuilder report = new StringBuilder();
    interface Work { void run() throws Exception; }
    void check(boolean value, String why) { if (!value) throw new AssertionError(why); }
    void test(String name, Work work) throws Exception {
        work.run(); passed++; report.append("PASS ").append(name).append('\n');
        Bundle out = new Bundle(); out.putString("stream", "PASS " + name + "\n"); sendStatus(0, out);
    }
    void main(Work work) throws Exception {
        Throwable[] failure = new Throwable[1];
        runOnMainSync(() -> { try { work.run(); } catch(Throwable e) { failure[0] = e; } });
        if (failure[0] != null) throw new AssertionError(failure[0]);
    }
    @Override public void onCreate(Bundle bundle) { super.onCreate(bundle); options = bundle; start(); }
    Intent home() { return new Intent(getTargetContext(), HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); }
    AccessibilityNodeInfo find(AccessibilityNodeInfo root, String text) {
        if (root == null) return null;
        Rect screen=new Rect(), bounds=new Rect(); AccessibilityNodeInfo window=getUiAutomation().getRootInActiveWindow(); if(window!=null)window.getBoundsInScreen(screen);root.getBoundsInScreen(bounds);
        if (root.isVisibleToUser() && bounds.width()>0 && bounds.height()>0 && Rect.intersects(screen,bounds) && (String.valueOf(root.getText()).contains(text) || String.valueOf(root.getContentDescription()).contains(text))) return root;
        for(int i=0;i<root.getChildCount();i++) { AccessibilityNodeInfo found=find(root.getChild(i),text); if(found!=null)return found; }
        return null;
    }
    AccessibilityNodeInfo scrollable(AccessibilityNodeInfo root) {
        if(root==null)return null;if(root.isScrollable() && root.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN))return root;
        for(int i=0;i<root.getChildCount();i++){AccessibilityNodeInfo found=scrollable(root.getChild(i));if(found!=null)return found;}return null;
    }
    AccessibilityNodeInfo await(String text, boolean scroll) throws Exception {
        long end=SystemClock.uptimeMillis()+12000;
        while(SystemClock.uptimeMillis()<end) {
            AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow(),node=find(root,text);
            if(node!=null){if(scroll && node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId())){SystemClock.sleep(250);node=find(getUiAutomation().getRootInActiveWindow(),text);}if(node!=null)return node;}
            if(scroll){AccessibilityNodeInfo list=scrollable(root);if(list!=null)list.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);}
            SystemClock.sleep(180);
        }
        throw new AssertionError("Visible UI missing: "+text);
    }
    void tap(AccessibilityNodeInfo node) {
        Rect bounds=new Rect();node.getBoundsInScreen(bounds);long now=SystemClock.uptimeMillis();
        Bundle debug=new Bundle();debug.putString("stream","TOUCH "+node.getText()+" / "+node.getContentDescription()+" at "+bounds+"\n");sendStatus(0,debug);
        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,bounds.centerX(),bounds.centerY(),0);
        MotionEvent up=MotionEvent.obtain(now,now+75,MotionEvent.ACTION_UP,bounds.centerX(),bounds.centerY(),0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN); up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try{check(getUiAutomation().injectInputEvent(down,true),"Touch down failed");check(getUiAutomation().injectInputEvent(up,true),"Touch up failed");}finally{down.recycle();up.recycle();}
    }
    void screenshot(String name) throws Exception {
        SystemClock.sleep(400);Bitmap image=getUiAutomation().takeScreenshot();check(image!=null,"No screenshot");
        File folder=new File(getTargetContext().getFilesDir(),"prototype-visuals");folder.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();
    }
    MatrixCursor cardCursor(long id,String title,String pkg,int browsable,int searchable,String link) {
        MatrixCursor c=new MatrixCursor(new String[]{"_id","title","package_name","browsable","searchable","poster_art_uri","intent_uri","watch_next_type"});
        c.addRow(new Object[]{id,title,pkg,browsable,searchable,"https://example.invalid/source.jpg",link,0});c.moveToFirst();return c;
    }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            Context context=getTargetContext();PackageManager pm=context.getPackageManager();
            if(options!=null && "true".equals(options.getString("guardRestart"))) {
                activity=startActivitySync(home());waitForIdleSync();
                test("startup crash-loop recovery disables only this app Home alias",()->{
                    check(SafeMode.INSTANCE.isActive(context),"Seeded startup guard did not run");
                    check(!SafeMode.INSTANCE.homeCandidateEnabled(context),"Home candidate left enabled");
                });
                test("recovery screen remains launchable after startup guard",()->{
                    Activity recovery=startActivitySync(new Intent(context,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    await("Recovery mode",false);screenshot("recovery-after-restart");main(recovery::finish);
                });
                main(()->context.getSharedPreferences("guard",0).edit().clear().commit());
            } else if(options!=null && "true".equals(options.getString("screensOnly"))) {
                activity=startActivitySync(home());await("Installed apps",false);screenshot("landscape-home");
                tap(await("All Apps (",true));await("actual installed apps",false);screenshot("landscape-apps");
                result.putString("stream","2 landscape screenshots captured; visual review only");finish(Activity.RESULT_OK,result);return;
            } else {
                main(()->context.getSharedPreferences("guard",0).edit().clear().commit());
                ComponentName before=SafeMode.INSTANCE.resolvedHome(context);
                test("normal phone and recovery launcher entries exist without a Home requirement",()->{
                    check(!SafeMode.INSTANCE.isTv(context),"Phone suite requires a phone emulator");
                    Intent launch=pm.getLaunchIntentForPackage(context.getPackageName());check(launch!=null,"Normal launcher missing");
                    ActivityInfo recovery=pm.getActivityInfo(new ComponentName(context,SettingsActivity.class),0);check(recovery.exported && recovery.enabled,"Recovery unreachable");
                    check(!SafeMode.INSTANCE.homeCandidateEnabled(context),"Home candidate enabled by default");
                });
                test("APK has no privileged or sensitive access permissions",()->{
                    PackageInfo info=pm.getPackageInfo(context.getPackageName(),PackageManager.GET_PERMISSIONS|PackageManager.GET_SERVICES);
                    List<String> permissions=info.requestedPermissions==null?Collections.emptyList():Arrays.asList(info.requestedPermissions);
                    check(permissions.contains("android.permission.INTERNET"),"Artwork Internet permission missing");
                    for(String denied:new String[]{"android.permission.READ_TV_LISTINGS","com.android.providers.tv.permission.ACCESS_ALL_EPG_DATA","android.permission.READ_SMS","android.permission.RECORD_AUDIO","android.permission.QUERY_ALL_PACKAGES"})check(!permissions.contains(denied),"Unexpected access: "+denied);
                    if(info.services!=null)for(ServiceInfo service:info.services)check(!"android.permission.BIND_NOTIFICATION_LISTENER_SERVICE".equals(service.permission),"Notification listener present");
                });
                test("installed app catalogue contains real deduplicated packages and icons",()->{
                    List<AppItem> apps=new AppsRepository(context).load();Set<String> seen=new HashSet<>();check(apps.size()>=2,"No apps");
                    for(AppItem app:apps){check(seen.add(app.getPkg()),"Duplicate package");check(!app.getPkg().equals(context.getPackageName()),"Own app included");pm.getApplicationInfo(app.getPkg(),0);check(app.getIcon()!=null,"Icon missing");check(new AppsRepository(context).launchIntent(app.getPkg())!=null,"App not launchable");}
                });
                test("phone feed accurately reports TV listings unavailable",()->{
                    TitleFeed feed=new TitlesRepository(context).load();check(feed.getWatchNext().isEmpty()&&feed.getPreviews().isEmpty(),"Fabricated phone feed");check(feed.getStatus().contains("Phone testing"),"Capability scope missing");
                });
                test("public TV parser preserves long IDs and rejects hidden or own records",()->{
                    try(MatrixCursor c=cardCursor(5000000000L,"Synthetic fixture","fixture.source",1,1,"https://example.invalid/title")){TitleCard card=TitlesRepository.Companion.parse(c,false,context.getPackageName());check(card!=null&&card.getId()==5000000000L,"ID truncated");}
                    try(MatrixCursor c=cardCursor(1,"Hidden fixture","fixture.source",0,1,"")){check(TitlesRepository.Companion.parse(c,false,context.getPackageName())==null,"Hidden card exposed");}
                    try(MatrixCursor c=cardCursor(1,"Not searchable","fixture.source",1,0,"")){check(TitlesRepository.Companion.parse(c,false,context.getPackageName())==null,"Non-searchable card exposed");}
                    try(MatrixCursor c=cardCursor(1,"Own fixture",context.getPackageName(),1,1,"")){check(TitlesRepository.Companion.parse(c,false,context.getPackageName())==null,"Own card exposed");}
                });
                test("title intents discard components selectors extras and URI grants",()->{
                    TitleCard fixture=new TitleCard(1,"Synthetic fixture",null,"intent://title#Intent;scheme=https;component=evil.pkg/.Attack;S.secret=fixture;launchFlags=3;end","fixture.source",-1,false);
                    Intent clean=TitlesRepository.Companion.safeTitleIntent(fixture);check(clean!=null&&"fixture.source".equals(clean.getPackage()),"Source package not fixed");check(clean.getComponent()==null&&clean.getSelector()==null&&clean.getExtras()==null,"Unsafe intent fields retained");check((clean.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))==0,"URI grants retained");
                });
                test("dangerous title schemes are rejected",()->{for(String uri:new String[]{"javascript:alert(1)","file:///private","content://private/1","tel:123","sms:123","https://user:pass@example.invalid"})check(TitlesRepository.Companion.safeTitleIntent(new TitleCard(1,"Fixture",null,uri,"fixture.source",-1,false))==null,"Unsafe link accepted");});
                test("Home-intent escape window counts only three presses within two seconds",()->{HomePressTracker tracker=new HomePressTracker();check(!tracker.press(1000)&&!tracker.press(1500)&&tracker.press(2500),"Triple press failed");check(!tracker.press(5000)&&!tracker.press(8000)&&!tracker.press(8100),"Old press retained");});
                test("crash timestamps use actual recent times and ignore future or malformed values",()->{List<Long> times=CrashHistory.INSTANCE.recent("1,90000,95000,100001,bad",100000);check(times.size()==2&&times.get(0)==90000L,"Crash history window incorrect");});
                activity=startActivitySync(home());waitForIdleSync();
                test("normal app starts and displays actual phone capability state",()->{await("Phone testing",false);await("Installed apps",false);screenshot("phone-home");});
                test("touch opens All Apps, launches a real installed app and returns with Back",()->{
                    tap(await("All Apps (",true));await("actual installed apps",false);screenshot("phone-apps");
                    AppItem target=new AppsRepository(context).load().get(0);
                    tap(await("Open "+target.getLabel(),true));
                    long end=SystemClock.uptimeMillis()+15000;boolean opened=false;
                    while(SystemClock.uptimeMillis()<end){AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root!=null&&target.getPkg().contentEquals(root.getPackageName())){opened=true;break;}SystemClock.sleep(200);}
                    check(opened,"Touch did not open installed app "+target.getPkg());
                    // UiAutomation can drive Back across apps in this test; Flow requests no input-injection permission.
                    check(getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK),"System Back failed");
                    await("actual installed apps",false);
                });
                main(activity::finish);activity=startActivitySync(home());waitForIdleSync();SystemClock.sleep(700);
                test("D-pad focus and OK open recovery settings",()->{
                    ActivityMonitor monitor=addMonitor(SettingsActivity.class.getName(),null,false);
                    sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
                    Activity settings=waitForMonitorWithTimeout(monitor,8000);removeMonitor(monitor);check(settings!=null,"D-pad did not activate Settings");
                    await("Phone testing",false);screenshot("phone-settings");main(settings::finish);
                });
                test("phone cannot enable TV Home candidate and default Home stays unchanged",()->{
                    check(!SafeMode.INSTANCE.enableTvCandidate(context),"Phone Home candidate enabled");
                    check(Objects.equals(before,SafeMode.INSTANCE.resolvedHome(context)),"Default Home changed");
                });
                test("disabling optional Home alias preserves recovery and normal launchers",()->{
                    SafeMode.INSTANCE.enter(context,false);check(!SafeMode.INSTANCE.homeCandidateEnabled(context),"Alias not disabled");
                    check(pm.getLaunchIntentForPackage(context.getPackageName())!=null,"Normal app launch lost");pm.getActivityInfo(new ComponentName(context,SettingsActivity.class),0);
                    check(Objects.equals(before,SafeMode.INSTANCE.resolvedHome(context)),"Other Home selection changed");
                });
                // The next instrumentation process exercises actual Application startup recovery.
                main(()->{long now=System.currentTimeMillis();context.getSharedPreferences("guard",0).edit().putBoolean("safe_mode",false).putString("crashes",(now-1000)+","+(now-500)+","+now).commit();});
            }
            result.putInt("passed",passed);result.putString("stream",report+"\n"+passed+" checks passed\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream",report+"\nFAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
    }
}
