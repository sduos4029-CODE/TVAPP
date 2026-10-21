package tv.flow.mobile;

import android.content.*;
import android.net.Uri;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import android.text.TextUtils;
import java.util.*;

/** Separates actual shared programme types; never substitutes app tiles for movies. */
final class SharedContentScreen {
  final MainActivity a;final FlowDesign d;
  SharedContentScreen(MainActivity activity,FlowDesign design){a=activity;d=design;}
  void render(){
    a.sharedContent.refresh(false);
    if(a.page.equals("Home")){
      d.section(a.content,"Favourite apps","Installed on this device");d.favoriteRow();d.gap(a.content,14);
      if(!a.live.recentApps().isEmpty()){
        d.section(a.content,"Recently opened","Apps opened through Flow");a.live.appRow(a.live.recentApps(),true);d.gap(a.content,14);
      }
    }
    LinearLayout heading=d.row();
    String title=a.page.equals("Home")?"From your TV apps":a.page.equals("New")?"New shared titles":a.page;
    heading.addView(d.bold(title,26,FlowDesign.TEXT),new LinearLayout.LayoutParams(0,-2,1));
    heading.addView(d.button("↻ Refresh",()->{a.sharedContent.refresh(true);a.build();}));
    heading.addView(d.button("Sources",this::sources));a.content.addView(heading);
    d.note(a.content,a.sharedContent.status);
    ArrayList<SharedTvContent.Item> list=a.sharedContent.forPage(a.page);
    if(list.isEmpty()){
      LinearLayout empty=d.col();empty.setPadding(d.u(25),d.u(28),d.u(25),d.u(25));
      empty.setBackground(d.border(0xff15263b,0xff304457,28,1));
      LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(d.u(5),d.u(13),d.u(5),d.u(10));a.content.addView(empty,lp);
      empty.addView(d.bold(a.sharedContent.loading?"Checking your TV's shared cards…":a.tv()?"No readable "+title.toLowerCase(Locale.ROOT)+" yet":"Your apps do not share this catalogue",22,FlowDesign.TEXT));
      empty.addView(d.label(a.sharedContent.detail,14,FlowDesign.MUTED));
      empty.addView(d.button("How app connections work",this::sources));
      if(a.page.equals("New"))d.note(empty,"Only records explicitly published as new by an app appear here. Newly installed apps are not movie releases.");
      if(a.page.equals("Kids"))d.note(empty,"Only published family/kids genre records qualify. No invented age ratings or parental enforcement.");
    }else{
      grid(list);
      d.note(a.content,"Only programme metadata shared by source apps · no full-catalogue or subscription guarantee");
    }
  }
  void grid(ArrayList<SharedTvContent.Item> list){
      int columns=a.landscape()?6:3;
      a.sharedPage=Math.max(0,Math.min(a.sharedPage,Math.max(0,(list.size()-1)/24)));
      int offset=a.sharedPage*24,end=Math.min(offset+24,list.size());
      for(int start=offset;start<end;start+=columns){
        LinearLayout row=d.row();
        for(int n=start;n<Math.min(start+columns,end);n++){
          SharedTvContent.Item item=list.get(n);
          LinearLayout card=d.col();card.setPadding(d.u(9),d.u(11),d.u(9),d.u(9));
          card.setBackground(d.border(0xff15263b,0xff304457,20,1));
          FrameLayout picture=new FrameLayout(a);picture.setBackground(d.solid(0xff0c1c2d,14));picture.setClipToOutline(true);
          ImageView image=new ImageView(a);image.setScaleType(ImageView.ScaleType.CENTER_CROP);picture.addView(image,new FrameLayout.LayoutParams(-1,-1));
          TextView art=d.label("Loading artwork…",12,FlowDesign.MUTED);art.setGravity(Gravity.CENTER);picture.addView(art,new FrameLayout.LayoutParams(-1,-1));
          card.addView(picture,new LinearLayout.LayoutParams(-1,d.u(115)));a.sharedArtwork.load(item.poster,image,art);
          TextView name=d.bold(item.name,14,FlowDesign.TEXT);name.setMaxLines(2);name.setEllipsize(TextUtils.TruncateAt.END);card.addView(name);
          TextView source=d.label(item.source,10,FlowDesign.LAV);source.setMaxLines(1);source.setEllipsize(TextUtils.TruncateAt.END);card.addView(source);
          d.focus(card,()->details(item));card.setContentDescription(item.name+", shared by "+item.source);
          LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,d.u(200),1);lp.setMargins(d.u(4),d.u(5),d.u(4),d.u(5));row.addView(card,lp);
        }
        while(row.getChildCount()<columns)row.addView(new View(a),new LinearLayout.LayoutParams(0,1,1));a.content.addView(row);
      }
    if(list.size()>24){
      LinearLayout pages=d.row();
      Button previous=d.button("‹ Previous",()->{a.sharedPage--;a.build();});previous.setEnabled(offset>0);pages.addView(previous);
      pages.addView(d.label((offset+1)+"–"+end+" of "+list.size()+" shared titles",12,FlowDesign.MUTED),new LinearLayout.LayoutParams(0,-2,1));
      Button next=d.button("Next ›",()->{a.sharedPage++;a.build();});next.setEnabled(end<list.size());pages.addView(next);a.content.addView(pages);
    }
  }
  void saved(){
    LinearLayout heading=d.row();heading.addView(d.bold("My list",26,FlowDesign.TEXT),new LinearLayout.LayoutParams(0,-2,1));
    heading.addView(d.button("YouTube links",this::youtubeLinks));a.content.addView(heading);
    ArrayList<SharedTvContent.Item> list=a.sharedContent.saved();
    d.note(a.content,"Saved programme cards · "+list.size()+" titles · availability is checked by the source app");
    if(list.isEmpty())d.note(a.content,"Save a real shared title from its details panel. Your earlier YouTube links remain available through YouTube links.");else grid(list);
  }
  void youtubeLinks(){
    LinearLayout p=d.panel("Saved YouTube links");org.json.JSONArray links=a.live.bookmarks();
    p.addView(d.button("+ Save a YouTube link",()->a.live.addBookmark()));
    if(links.length()==0)d.note(p,"No YouTube links saved yet.");
    for(int n=0;n<links.length();n++){
      org.json.JSONObject object=links.optJSONObject(n);if(object==null)continue;
      String id=object.optString("id");if(!id.matches("[A-Za-z0-9_-]{11}"))continue;
      String name=object.optString("name",id);p.addView(d.button("▶ "+name,()->a.live.openVideo(id)));
    }
    d.note(p,"User-saved links, not an imported YouTube account queue or watch history.");
  }
  void search(){
    LinearLayout p=d.panel("Search shared titles & apps");android.widget.EditText input=a.live.field(p,"Title or installed app");
    d.note(p,"Searches only programme cards shared with Flow and your local saved titles. Private app catalogues cannot be searched through their login sessions.");
    LinearLayout results=d.col();p.addView(results);
    input.addTextChangedListener(a.watcher(()->{
      results.removeAllViews();String query=input.getText().toString().trim().toLowerCase(Locale.ROOT);
      if(query.isEmpty())return;
      java.util.HashSet<String> seen=new java.util.HashSet<>();
      ArrayList<SharedTvContent.Item> entries=new ArrayList<>(a.sharedContent.items);entries.addAll(a.sharedContent.saved());
      for(SharedTvContent.Item item:entries)if(item.name.toLowerCase(Locale.ROOT).contains(query) && seen.add(a.sharedContent.key(item)))results.addView(d.button(item.name+" · "+item.source,()->details(item)));
      for(MainActivity.AppEntry app:a.apps)if(app.label.toLowerCase(Locale.ROOT).contains(query))results.addView(d.button("Open "+app.label,()->a.launchApp(app)));
      if(results.getChildCount()==0)d.note(results,"No matching shared titles or installed apps");
    }));
    a.live.keyboard(p,input);
  }
  void details(SharedTvContent.Item item){
    LinearLayout p=d.panel(item.name);
    p.addView(d.label("Published by "+item.source,12,FlowDesign.LAV));
    p.addView(d.label(item.description.isEmpty()?"The source did not publish a description.":item.description,14,FlowDesign.TEXT));
    p.addView(d.mint("Open in source app",()->a.sharedContent.open(item)));
    p.addView(d.button(a.sharedContent.isSaved(item)?"Remove from My list":"+ My list",()->{a.sharedContent.toggleSaved(item);a.build();new SharedContentScreen(a,a.design).details(item);}));
    d.note(p,"The provider controls login, subscription checks and playback. A shared card is not a promise that this account can watch it.");
  }
  void sources(){
    LinearLayout p=d.panel("Connected app content");
    p.addView(d.bold(a.sharedContent.status,20,FlowDesign.TEXT));d.note(p,a.sharedContent.detail);
    d.note(p,"Flow reads only programme cards that Android TV makes accessible through TvContract. It cannot read another app's private catalogue, cookies, login session or watch history.");
    d.note(p,"On a phone, being signed in to Netflix, Prime Video or another app does not expose its catalogue to Flow. Provider-specific account integration needs an official supported API/partnership.");
    p.addView(d.button("Check shared TV feed",()->{a.sharedContent.refresh(true);a.closeDrawer();a.build();}));
    p.addView(d.button("Android TV API documentation",()->a.safeStart(new Intent(Intent.ACTION_VIEW,Uri.parse("https://developer.android.com/reference/android/media/tv/TvContract.WatchNextPrograms")))));
    d.note(p,"Shared poster URLs may load from the publisher’s HTTPS server. No cookies or account credentials are read or forwarded. No private-app scraping or replacement sample titles are used.");
  }
}
