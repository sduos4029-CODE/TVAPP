package tv.flow.mobile;

import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Only OS-published TV records. Does not read private app data, logins or cookies. */
final class SharedTvContent {
  static final class Item {
    long id; int type,watchType=-1; boolean watchNext,kids;
    String name,description,poster,link,pkg,source;
  }
  final MainActivity a;
  final ExecutorService worker=Executors.newSingleThreadExecutor();
  final ArrayList<Item> items=new ArrayList<>();
  String status="Not checked",detail="";
  boolean loading,checked;
  long lastCheck;
  boolean observing;
  final android.database.ContentObserver observer=new android.database.ContentObserver(new android.os.Handler(android.os.Looper.getMainLooper())){
    @Override public void onChange(boolean self){if(a.resumed && isContentPage(a.page))refresh(true);}
  };
  void startObserving(){
    stopObserving();if(!a.tv() || !isContentPage(a.page))return;
    try{a.getContentResolver().registerContentObserver(TvContract.WatchNextPrograms.CONTENT_URI,true,observer);
      a.getContentResolver().registerContentObserver(TvContract.PreviewPrograms.CONTENT_URI,true,observer);observing=true;
    }catch(RuntimeException e){a.getContentResolver().unregisterContentObserver(observer);}
  }
  void stopObserving(){if(observing){a.getContentResolver().unregisterContentObserver(observer);observing=false;}}

  SharedTvContent(MainActivity activity){a=activity;}

  void refresh(boolean force){
    if(loading)return;
    if(!force && checked && System.currentTimeMillis()-lastCheck<60000)return;
    checked=true;lastCheck=System.currentTimeMillis();
    if(!a.tv()){
      status="Not shared on this phone";
      detail="Signing in to another app does not share its catalogue with Flow. Android TV can expose published recommendation cards; this phone has no supported shared TV feed.";
      return;
    }
    if(a.getPackageManager().resolveContentProvider("android.media.tv",0)==null){
      status="TV feed unavailable";detail="This device has no accessible Android TV recommendation provider.";return;
    }
    loading=true;status="Reading shared TV cards…";
    worker.execute(()->{
      ArrayList<Item> loaded=new ArrayList<>();ArrayList<String> failures=new ArrayList<>();
      read(TvContract.WatchNextPrograms.CONTENT_URI,true,loaded,failures);
      read(TvContract.PreviewPrograms.CONTENT_URI,false,loaded,failures);
      a.handler.post(()->{
        if(a.isDestroyed())return;
        items.clear();items.addAll(loaded);loading=false;
        status=items.isEmpty()?(failures.isEmpty()?"No shared titles":"Feed access restricted"):items.size()+" shared titles";
        detail=items.isEmpty()?"The OS returned no readable programme records. Apps may not publish cards, and firmware may restrict access. Their private catalogues and logins remain inaccessible.":"Titles published through Android TV by installed apps. This is a limited shared feed, not their full catalogues or proof of subscription availability.";
        if(!failures.isEmpty() && !items.isEmpty())detail+=" Some TV tables were not accessible.";
        if(a.resumed && a.drawer==null && isContentPage(a.page))a.build();
      });
    });
  }
  static boolean isContentPage(String page){return Arrays.asList("Home","Movies","Shows","New","Kids").contains(page);}
  void read(Uri table,boolean watchNext,List<Item> out,List<String> failures){
    ArrayList<String> columns=new ArrayList<>(Arrays.asList("_id","package_name","title","type","short_description","poster_art_uri","intent_uri","canonical_genre","browsable","searchable"));
    if(watchNext)columns.add("watch_next_type");
    try(Cursor cursor=a.getContentResolver().query(table,columns.toArray(new String[0]),null,null,null)){
      if(cursor==null)return;
      int count=0;
      while(cursor.moveToNext() && count++<200){
        String pkg=string(cursor,"package_name"),name=string(cursor,"title");
        if(pkg.isEmpty() || name.isEmpty() || pkg.equals(a.getPackageName()))continue;
        int searchable=number(cursor,"searchable",1),browsable=number(cursor,"browsable",1);
        if(searchable==0 || browsable==0)continue;
        Item item=new Item();item.id=number(cursor,"_id",0);item.name=name;item.pkg=pkg;
        item.type=number(cursor,"type",-1);item.watchNext=watchNext;
        item.watchType=watchNext?number(cursor,"watch_next_type",-1):-1;
        item.description=string(cursor,"short_description");item.poster=string(cursor,"poster_art_uri");
        item.link=string(cursor,"intent_uri");
        String genres=string(cursor,"canonical_genre");
        item.kids=Arrays.asList(TvContract.Programs.Genres.decode(genres)).contains(TvContract.Programs.Genres.FAMILY_KIDS);
        try{ApplicationInfo info=a.getPackageManager().getApplicationInfo(pkg,0);item.source=a.getPackageManager().getApplicationLabel(info).toString();}
        catch(PackageManager.NameNotFoundException e){item.source=pkg;}
        boolean duplicate=false;for(Item existing:out)if(existing.pkg.equals(pkg) && existing.name.equals(name)){duplicate=true;break;}
        if(!duplicate)out.add(item);
      }
    }catch(SecurityException e){failures.add("permission");}
    catch(IllegalArgumentException e){failures.add("unsupported");}
    catch(RuntimeException e){failures.add("unavailable");}
  }
  static String string(Cursor cursor,String column){int n=cursor.getColumnIndex(column);return n<0 || cursor.isNull(n)?"":cursor.getString(n);}
  static int number(Cursor cursor,String column,int fallback){int n=cursor.getColumnIndex(column);return n<0 || cursor.isNull(n)?fallback:cursor.getInt(n);}
  ArrayList<Item> forPage(String page){
    ArrayList<Item> result=new ArrayList<>();
    for(Item item:items){
      boolean include=page.equals("Home")
          || page.equals("Movies") && item.type==TvContract.PreviewPrograms.TYPE_MOVIE
          || page.equals("Shows") && (item.type==TvContract.PreviewPrograms.TYPE_TV_EPISODE || item.type==TvContract.PreviewPrograms.TYPE_TV_SERIES || item.type==TvContract.PreviewPrograms.TYPE_TV_SEASON)
          || page.equals("New") && item.watchNext && item.watchType==TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_NEW
          || page.equals("Kids") && item.kids;
      if(include)result.add(item);
    }
    return result;
  }
  String key(Item item){return item.pkg+"|"+item.name;}
  JSONArray savedData(){try{return new JSONArray(a.prefs.getString("sharedTitleList","[]"));}catch(JSONException e){return new JSONArray();}}
  JSONObject toJson(Item item){
    JSONObject object=new JSONObject();
    try{object.put("name",item.name).put("pkg",item.pkg).put("source",item.source).put("description",item.description).put("poster",item.poster).put("link",item.link).put("type",item.type).put("kids",item.kids).put("id",item.id);}catch(JSONException ignored){}
    return object;
  }
  ArrayList<Item> saved(){
    ArrayList<Item> list=new ArrayList<>();JSONArray records=savedData();
    for(int n=0;n<records.length();n++){
      JSONObject object=records.optJSONObject(n);if(object==null)continue;
      Item item=new Item();item.name=object.optString("name");item.pkg=object.optString("pkg");
      if(item.name.isEmpty() || item.pkg.isEmpty())continue;
      item.source=object.optString("source",item.pkg);item.description=object.optString("description");item.poster=object.optString("poster");item.link=object.optString("link");item.type=object.optInt("type",-1);item.id=object.optLong("id");item.kids=object.optBoolean("kids");list.add(item);
    }
    return list;
  }
  boolean isSaved(Item item){for(Item saved:saved())if(key(item).equals(key(saved)))return true;return false;}
  void toggleSaved(Item item){
    JSONArray records=new JSONArray();boolean remove=isSaved(item);
    for(Item saved:saved())if(!key(saved).equals(key(item)))records.put(toJson(saved));
    if(!remove){if(records.length()>=100){a.toast("Your title list is full");return;}records.put(toJson(item));}
    a.prefs.edit().putString("sharedTitleList",records.toString()).apply();a.toast(remove?"Removed from My list":"Saved shared title");
  }
  void open(Item item){
    try{
      Intent parsed=Intent.parseUri(item.link,Intent.URI_INTENT_SCHEME);
      Uri uri=parsed.getData();String scheme=uri==null?null:uri.getScheme();
      if(scheme!=null && !Arrays.asList("file","content","javascript","data","intent","tel","sms","market").contains(scheme.toLowerCase(Locale.ROOT))){
        Intent view=new Intent(Intent.ACTION_VIEW,uri).setPackage(item.pkg);
        if(view.resolveActivity(a.getPackageManager())!=null){a.safeStart(view);return;}
      }
    }catch(Exception ignored){}
    MainActivity.AppEntry app=a.findApp(item.pkg);
    if(app!=null){a.launchApp(app);a.toast("No supported title link; opened the source app");}
    else a.toast("The source app is unavailable");
  }
  void close(){stopObserving();worker.shutdownNow();}
}
