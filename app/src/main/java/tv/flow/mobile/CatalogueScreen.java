package tv.flow.mobile;

import android.content.*;
import android.net.Uri;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Native, source-backed discovery. Source pages and app launches are distinct from Watch. */
final class CatalogueScreen {
  final MainActivity a;final FlowDesign d;
  CatalogueScreen(MainActivity activity,FlowDesign design){a=activity;d=design;}
  String kind(){return a.page.equals("Shows")?"show":a.page.equals("Movies")?"movie":a.catalogueKind;}
  String language(String kind){return a.prefs.getString(kind.equals("movie")?"movieLanguage":"showLanguage",kind.equals("movie")?"Hindi":"All languages");}
  OpenCatalogue.Request request(String kind){return new OpenCatalogue.Request(kind,a.catalogueQuery,language(kind),a.page.equals("Kids"),a.page.equals("New"),a.catalogueIndex);}
  void render(){
    if(a.page.equals("My list")){saved();return;}
    if(a.page.equals("Home")){home();return;}
    LinearLayout header=d.row();header.addView(d.bold(a.page.equals("New")?"Release discovery":a.page,26,FlowDesign.TEXT),new LinearLayout.LayoutParams(0,-2,1));
    header.addView(d.button("⌕ Search",this::search));header.addView(d.button("Sources",this::sources));a.content.addView(header);
    LinearLayout filters=d.row();
    if(a.page.equals("New") || a.page.equals("Kids"))filters.addView(d.button(kind().equals("movie")?"Films":"Series / episodes",()->{a.catalogueKind=kind().equals("movie")?"show":"movie";a.catalogueIndex=0;a.sharedPage=0;a.build();}));
    filters.addView(d.button(language(kind())+" ▾",this::languages));
    filters.addView(d.button("↻ Refresh",()->{a.openCatalogue.fetch(request(kind()),true);a.build();}));
    if(!a.catalogueQuery.isEmpty())filters.addView(d.button("Clear search",()->{a.catalogueQuery="";a.catalogueIndex=0;a.sharedPage=0;a.build();}));a.content.addView(filters);
    if(!a.catalogueQuery.isEmpty())paragraph(a.content,"Search: "+a.catalogueQuery);
    String scope=kind().equals("movie")?"Wikidata film metadata · selected original language · global release dates":"TVmaze series metadata · global coverage";
    if(a.page.equals("New"))scope+=kind().equals("movie")?" · source release within 90 days":" · episodes scheduled today (device date), not verified provider additions";
    else if(kind().equals("show") && a.catalogueQuery.isEmpty())scope+=" · source directory page "+(a.catalogueIndex+1)+", not ranked recommendations";
    paragraph(a.content,scope+". Streaming availability in India and your subscription are not supplied by these sources.");
    if(a.page.equals("Kids")){paragraph(a.content,"Children/family genre collection, without certified age ratings. Anime is not automatically included. This does not enforce parental controls.");a.content.addView(d.button("Parental settings",()->{LinearLayout p=d.panel("Parental settings");paragraph(p,"Flow does not enforce a device-wide child lock. Use supported parental controls in Android and each provider app.");p.addView(d.button("Open device settings",()->a.openSettings(android.provider.Settings.ACTION_SETTINGS)));}));}
    result(a.openCatalogue.fetch(request(kind()),false),a.content,true);
    if(kind().equals("show") && !a.page.equals("New") && a.catalogueQuery.isEmpty()){
      LinearLayout pages=d.row();Button previous=d.button("‹ Directory page",()->{a.catalogueIndex--;a.sharedPage=0;a.build();});previous.setEnabled(a.catalogueIndex>0);pages.addView(previous);
      Button next=d.button("Next directory page ›",()->{a.catalogueIndex++;a.sharedPage=0;a.build();});next.setEnabled(a.catalogueIndex<20);pages.addView(next);a.content.addView(pages);
      paragraph(a.content,"Up to 40 matching records are displayed from each source response. Search by name for titles outside this directory slice.");
    }
    attribution(a.content);
  }
  void home(){
    LinearLayout header=d.row();header.addView(d.bold("Discover something real.",26,FlowDesign.TEXT),new LinearLayout.LayoutParams(0,-2,1));header.addView(d.button("Sources",this::sources));a.content.addView(header);
    paragraph(a.content,"Public catalogue discovery · not your provider account, watch history or entitlement");
    OpenCatalogue.Result movies=a.openCatalogue.fetch(new OpenCatalogue.Request("movie","",language("movie"),false,false,0),false);
    if(!movies.cards.isEmpty())featured(movies.cards.get(0));
    d.section(a.content,"Favourite apps","Actual installed apps");d.favoriteRow();d.gap(a.content,8);
    if(!a.live.recentApps().isEmpty()){d.section(a.content,"Recently opened","Apps opened through Flow");a.live.appRow(a.live.recentApps(),true);}
    d.section(a.content,"Film discovery","Wikidata · "+language("movie"));result(movies,a.content,false);
    OpenCatalogue.Result shows=a.openCatalogue.fetch(new OpenCatalogue.Request("show","",language("show"),false,true,0),false);
    d.section(a.content,"Episodes scheduled today","TVmaze · global, not subscription availability");result(shows,a.content,false);
    if(a.tv()){
      a.sharedContent.refresh(false);ArrayList<SharedTvContent.Item> shared=a.sharedContent.forPage("Home");
      if(!shared.isEmpty()){d.section(a.content,"From your TV apps","Only OS-permitted published cards");new SharedContentScreen(a,d).grid(shared);}
    }
    attribution(a.content);
  }
  void featured(OpenCatalogue.Card c){
    LinearLayout box=d.col();box.setPadding(d.u(20),d.u(15),d.u(20),d.u(15));box.setBackground(d.border(FlowDesign.CARD,FlowDesign.LINE,24,1));
    box.addView(d.label("FEATURED FROM PUBLIC METADATA · "+c.source,10,FlowDesign.LAV));box.addView(d.bold(c.title,25,FlowDesign.TEXT));
    TextView summary=d.label(c.summary.isEmpty()?"Source description unavailable":c.summary,13,FlowDesign.MUTED);summary.setMaxLines(2);box.addView(summary);box.addView(d.mint("More information",()->details(c)));a.content.addView(box);d.gap(a.content,8);
  }
  void result(OpenCatalogue.Result r,LinearLayout parent,boolean full){
    String state=r.loading?"Loading public catalogue…":!r.error.isEmpty()?(r.cards.isEmpty()?"Catalogue unavailable":"Cached catalogue · refresh failed"):r.cards.isEmpty()?"No matching source records":r.cached?"Cached metadata":"Fetched metadata";
    paragraph(parent,state+(r.time>0 && !r.cards.isEmpty()?" · "+new java.text.SimpleDateFormat("dd MMM, HH:mm",Locale.getDefault()).format(new Date(r.time)):""));
    if(!r.error.isEmpty())paragraph(parent,r.error);
    if(r.cards.isEmpty()){
      if(!r.loading)paragraph(parent,"No sample titles are substituted. Check Sources, try another language/search, or retry when the public source is reachable. Opening a signed-in streaming app does not import its private catalogue.");return;
    }
    ArrayList<OpenCatalogue.Card> records=new ArrayList<>(r.cards);
    if(!full && records.size()>6)records=new ArrayList<>(records.subList(0,6));
    grid(records,parent,full);
  }
  void grid(ArrayList<OpenCatalogue.Card> list,LinearLayout parent,boolean paginate){
    int columns=a.landscape()?6:2,offset=0;
    if(paginate){a.sharedPage=Math.max(0,Math.min(a.sharedPage,Math.max(0,(list.size()-1)/24)));offset=a.sharedPage*24;}
    int end=Math.min(list.size(),offset+24);
    for(int start=offset;start<end;start+=columns){
      LinearLayout row=d.row();
      for(int n=start;n<Math.min(start+columns,end);n++){
        OpenCatalogue.Card c=list.get(n);LinearLayout card=d.col();card.setPadding(d.u(8),d.u(8),d.u(8),d.u(8));card.setBackground(d.border(FlowDesign.CARD,FlowDesign.LINE,18,1));
        card.addView(picture(c,a.landscape()?128:153),new LinearLayout.LayoutParams(-1,d.u(a.landscape()?128:153)));
        TextView title=d.bold(c.title,14,FlowDesign.TEXT);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);card.addView(title);
        TextView metadata=d.label(c.language+(c.date.isEmpty()?"":" · "+c.date),10,FlowDesign.MUTED);metadata.setMaxLines(1);metadata.setEllipsize(TextUtils.TruncateAt.END);card.addView(metadata);
        card.addView(d.label(c.source+" · "+(c.kind.equals("movie")?"film":"series"),10,FlowDesign.LAV));
        d.focus(card,()->details(c));card.setContentDescription(c.title+", "+c.kind+", metadata from "+c.source);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,d.u(a.landscape()?222:252),1);lp.setMargins(d.u(4),d.u(5),d.u(4),d.u(5));row.addView(card,lp);
      }
      while(row.getChildCount()<columns)row.addView(new View(a),new LinearLayout.LayoutParams(0,1,1));parent.addView(row);
    }
    if(paginate && list.size()>24){LinearLayout pages=d.row();Button back=d.button("‹ Previous",()->{a.sharedPage--;a.build();});back.setEnabled(offset>0);pages.addView(back);pages.addView(d.label((offset+1)+"–"+end+" of "+list.size(),12,FlowDesign.MUTED),new LinearLayout.LayoutParams(0,-2,1));Button next=d.button("Next ›",()->{a.sharedPage++;a.build();});next.setEnabled(end<list.size());pages.addView(next);parent.addView(pages);}
  }
  FrameLayout picture(OpenCatalogue.Card c,int height){
    FrameLayout frame=new FrameLayout(a);frame.setBackground(d.solid(0xff0c1321,12));frame.setClipToOutline(true);
    ImageView image=new ImageView(a);image.setScaleType(ImageView.ScaleType.CENTER_CROP);frame.addView(image,new FrameLayout.LayoutParams(-1,-1));
    TextView text=d.label("Source image unavailable",11,FlowDesign.MUTED);text.setGravity(Gravity.CENTER);frame.addView(text,new FrameLayout.LayoutParams(-1,-1));a.catalogueImages.load(c,image,text);return frame;
  }
  void details(OpenCatalogue.Card c){
    LinearLayout p=d.panel(c.title);p.addView(picture(c,160),new LinearLayout.LayoutParams(-1,d.u(160)));
    paragraph(p,c.source+" · "+(c.kind.equals("movie")?"Film":"TV series")+" · "+c.language);
    paragraph(p,(c.date.isEmpty()?"Date not supplied":c.date)+(c.runtime.isEmpty()?"":" · "+c.runtime));
    paragraph(p,c.summary.isEmpty()?"No description supplied by the source.":c.summary);
    if(!c.genres.isEmpty())paragraph(p,"Genres: "+android.text.TextUtils.join(", ",c.genres));
    if(!c.network.isEmpty())paragraph(p,"Reported original network/channel: "+c.network+". This does not establish current streaming availability in India.");
    p.addView(d.mint(a.openCatalogue.isSaved(c)?"Remove from My list":"+ My list",()->{a.openCatalogue.toggle(c);a.build();new CatalogueScreen(a,a.design).details(c);}));
    p.addView(d.button("Open metadata source",()->openSource(c)));
    p.addView(d.button("Open a streaming app",()->providers(c)));
    paragraph(p,"No verified provider title link, subscription check or trailer was supplied. Source pages contain information, not a playable film. General YouTube video IDs are not assumed to be trailers.");
    if(c.source.equals("TVmaze"))paragraph(p,"Metadata/image source: TVmaze. Catalogue attribution: CC BY-SA 4.0; see source and API terms.");
    else{
      paragraph(p,"Wikidata structured metadata: CC0. A source image is not necessarily a poster, and its copyright licence is separate.");
      CatalogueImages.Credit credit=a.catalogueImages.credits.get(c.image);
      if(credit!=null && !credit.image.isEmpty()){paragraph(p,"Image: "+credit.artist+" · "+credit.license);p.addView(d.button("Image licence & attribution",()->safeWeb(credit.page)));}
      else paragraph(p,"A Commons image is displayed only after a permitted licence and attribution can be resolved. Reopen details once image loading completes to view any resolved credit.");
    }
  }
  void providers(OpenCatalogue.Card c){
    LinearLayout p=d.panel("Open a streaming app");paragraph(p,"This opens the app, not a verified page for “"+c.title+"”. Search there using your existing login. Flow has no regional availability or entitlement data for this title.");
    boolean any=false;for(String pkg:new String[]{"com.amazon.avod.thirdpartyclient","com.amazon.amazonvideo.livingroom","in.startv.hotstar","in.airtel.tv"}){MainActivity.AppEntry app=a.findApp(pkg);if(app!=null){any=true;p.addView(d.button("Open "+app.label,()->a.launchApp(app)));}}
    if(!any)paragraph(p,"Prime Video, JioHotstar or Airtel Xstream was not found among launchable apps on this device.");p.addView(d.button("All installed apps",()->a.navigate("Apps")));
  }
  void openSource(OpenCatalogue.Card c){
    String expected=c.source.equals("TVmaze")?"https://www.tvmaze.com/shows/"+c.id:c.source.equals("Wikidata")?"https://www.wikidata.org/wiki/"+c.id:"";
    if(expected.isEmpty() || !c.url.equals(expected)){a.toast("Supported source link unavailable");return;}safeWeb(expected);
  }
  void safeWeb(String address){Uri uri=Uri.parse(address);if("https".equals(uri.getScheme()) && uri.getUserInfo()==null)a.safeStart(new Intent(Intent.ACTION_VIEW,uri));else a.toast("Unsupported web link");}
  void languages(){
    String current=kind();LinearLayout p=d.panel("Catalogue language");paragraph(p,current.equals("movie")?"Filter by source original-language statements.":"Filters the loaded TVmaze source response. A directory slice may contain no matching records; use search for a specific series.");
    for(String name:new String[]{"All languages","Hindi","Tamil","Telugu","Malayalam","Kannada","English"})p.addView(d.button(name,()->{a.prefs.edit().putString(current.equals("movie")?"movieLanguage":"showLanguage",name).apply();a.sharedPage=0;a.catalogueIndex=0;a.closeDrawer();a.build();}));
  }
  void search(){
    LinearLayout p=d.panel("Search films & series");EditText field=a.live.field(p,"Title in the public catalogue");field.setText(a.catalogueQuery);
    LinearLayout actions=d.row();actions.addView(d.button("Search films",()->submit(field,"movie")));actions.addView(d.button("Search series",()->submit(field,"show")));p.addView(actions);
    paragraph(p,"Queries go to Wikidata or TVmaze, not to your private streaming accounts. Press Search to submit; typing does not send requests.");a.live.keyboard(p,field);
  }
  void submit(EditText field,String type){String query=OpenCatalogue.cleanQuery(field.getText().toString());if(query.isEmpty()){a.toast("Enter a title");return;}a.catalogueQuery=query;a.catalogueKind=type;a.catalogueIndex=0;a.sharedPage=0;a.navigate(type.equals("movie")?"Movies":"Shows");}
  void saved(){
    LinearLayout header=d.row();header.addView(d.bold("My list",26,FlowDesign.TEXT),new LinearLayout.LayoutParams(0,-2,1));header.addView(d.button("YouTube links",()->new SharedContentScreen(a,d).youtubeLinks()));a.content.addView(header);
    ArrayList<OpenCatalogue.Card> list=new ArrayList<>();for(OpenCatalogue.Card c:a.openCatalogue.saved())if(a.savedKind.equals("all") || c.kind.equals(a.savedKind))list.add(c);
    LinearLayout filters=d.row();for(String kind:new String[]{"all","movie","show"})filters.addView(d.button(kind.equals("all")?"All":kind.equals("movie")?"Films":"Series",()->{a.savedKind=kind;a.sharedPage=0;a.build();}));a.content.addView(filters);
    paragraph(a.content,"Saved public metadata · no guarantee of streaming availability or subscription access");if(list.isEmpty())paragraph(a.content,"No matching saved catalogue titles. Open a real title’s details and select My list.");else grid(list,a.content,true);
    ArrayList<SharedTvContent.Item> shared=a.sharedContent.saved();if(!shared.isEmpty()){d.section(a.content,"Saved TV app cards","Earlier locally saved shared metadata");new SharedContentScreen(a,d).grid(shared);}
  }
  void sources(){
    LinearLayout p=d.panel("Catalogue sources");paragraph(p,"Films: Wikidata public structured metadata. Series: TVmaze public directory, title search and today’s global streaming schedule. No API key is used by these integrations.");
    paragraph(p,"These are separate from Prime Video, JioHotstar and Airtel Xstream accounts. They do not import your login, history, subscription or provider availability. India availability/provider filters are unavailable with these sources.");
    paragraph(p,"New films means source release dates within 90 days. Episode listings are scheduled airings, not verified newly added titles in your apps. Missing descriptions, images and ratings stay unavailable.");
    p.addView(d.button(a.openCatalogue.enabled()?"Turn catalogue networking off":"Turn catalogue networking on",()->{a.prefs.edit().putBoolean("catalogueEnabled",!a.openCatalogue.enabled()).apply();a.openCatalogue.clear();a.closeDrawer();a.build();}));
    p.addView(d.button("Clear catalogue cache…",()->a.confirm("Clear catalogue cache?","Removes cached metadata, decoded artwork and image credits. Saved titles remain.",()->{a.openCatalogue.clear();a.sharedArtwork.cache.evictAll();a.catalogueImages.credits.clear();a.toast("Catalogue cache cleared");})));
    p.addView(d.button("TVmaze API & attribution",()->safeWeb("https://www.tvmaze.com/api")));p.addView(d.button("Wikidata query service",()->safeWeb("https://www.wikidata.org/wiki/Wikidata:SPARQL_query_service")));
    p.addView(d.button("Optional shared TV app feed",()->new SharedContentScreen(a,d).sources()));
    paragraph(p,"HTTPS requests send searches and normal connection information to source/image servers, which have their own privacy policies. Flow sends no streaming-app credentials or account cookies. Metadata cache: up to 12 responses, 30-minute freshness; older retained data is labelled when refresh fails.");
  }
  void attribution(LinearLayout p){paragraph(p,"Sources: Wikidata structured metadata (CC0), TVmaze catalogue (CC BY-SA 4.0). Image licences are separate; source attribution is available in title details. No streaming availability or playback entitlement is inferred.");}
  void paragraph(LinearLayout p,String text){TextView v=d.label(text,12,FlowDesign.MUTED);v.setPadding(d.u(2),d.u(5),d.u(2),d.u(7));p.addView(v);}
}
