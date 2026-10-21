package tv.flow.mobile;

import android.content.*;
import android.net.Uri;
import android.text.Html;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Public metadata only: no streaming accounts, entitlement inference or private app access. */
final class OpenCatalogue {
  static final long TTL=30*60*1000L;
  static final class Card {
    String id="",kind="",title="",summary="",image="",date="",language="",runtime="",source="",url="",network="";
    final ArrayList<String> genres=new ArrayList<>();
    boolean children;
    String key(){return source+":"+id;}
    JSONObject json() throws JSONException {
      return new JSONObject().put("id",id).put("kind",kind).put("title",title).put("summary",summary).put("image",image).put("date",date).put("language",language).put("runtime",runtime).put("source",source).put("url",url).put("network",network).put("genres",new JSONArray(genres)).put("children",children);
    }
    static Card from(JSONObject o){
      Card c=new Card();c.id=text(o,"id");c.kind=text(o,"kind");c.title=text(o,"title");c.summary=text(o,"summary");c.image=text(o,"image");c.date=text(o,"date");c.language=text(o,"language");c.runtime=text(o,"runtime");c.source=text(o,"source");c.url=text(o,"url");c.network=text(o,"network");c.children=o.optBoolean("children");
      JSONArray g=o.optJSONArray("genres");if(g!=null)for(int i=0;i<g.length();i++)c.genres.add(g.optString(i));return c;
    }
  }
  static final class Request {
    final String kind,query,language;final boolean kids,recent;final int page;
    Request(String type,String q,String lang,boolean children,boolean releases,int index){kind=type;query=cleanQuery(q);language=lang;kids=children;recent=releases;page=Math.max(0,Math.min(20,index));}
    String key(){return kind+"|"+query+"|"+language+"|"+kids+"|"+recent+"|"+page+"|"+LocalDate.now();}
    String source(){return kind.equals("movie")?"Wikidata":"TVmaze";}
  }
  static final class Result {
    final ArrayList<Card> cards=new ArrayList<>();
    boolean loading,cached,stale;String error="";long time,lastAttempt;
  }
  interface Transport {String get(String address) throws Exception;}
  final MainActivity a;
  final ExecutorService worker=Executors.newFixedThreadPool(2);
  final LinkedHashMap<String,Result> memory=new LinkedHashMap<>();
  final File folder;Transport transport=OpenCatalogue::get;
  boolean closed;int generation;
  OpenCatalogue(MainActivity activity){a=activity;folder=new File(a.getCacheDir(),"open-catalogue");folder.mkdirs();}
  boolean enabled(){return a.prefs.getBoolean("catalogueEnabled",true);}
  Result fetch(Request request,boolean force){
    String key=request.key();Result previous=memory.get(key);long now=System.currentTimeMillis();
    if(!enabled()){Result off=new Result();off.error="Catalogue networking is turned off in Sources.";return off;}
    if(previous!=null && (previous.loading || !force && now-(previous.error.isEmpty()?previous.time:previous.lastAttempt)<(previous.error.isEmpty()?TTL:60000)))return previous;
    Result result=previous==null?read(request):previous;
    if(result==null)result=new Result();
    if(!force && result.error.isEmpty() && result.time>0 && now-result.time<TTL){result.cached=true;remember(key,result);return result;}
    result.loading=true;result.lastAttempt=now;result.stale=!result.cards.isEmpty();remember(key,result);
    final Result target=result;final int issued=generation;
    worker.execute(()->{
      ArrayList<Card> cards=null;String failure="";
      try{if(!enabled())throw new IOException("Catalogue networking off");String raw=transport.get(endpoint(request));cards=request.kind.equals("movie")?parseMovies(raw,request):parseShows(raw,request);}
      catch(Exception e){failure=error(e);}
      final ArrayList<Card> loaded=cards;final String problem=failure;
      a.handler.post(()->{
        if(closed || issued!=generation)return;
        target.loading=false;target.error=problem;
        if(loaded!=null){target.cards.clear();target.cards.addAll(loaded);target.time=System.currentTimeMillis();target.cached=false;target.stale=false;write(request,target);}
        else {target.stale=!target.cards.isEmpty();target.cached=target.stale;}
        if(a.resumed && !a.demo() && SharedTvContent.isContentPage(a.page)){if(a.drawer==null)a.build();else a.cataloguePending=true;}
      });
    });return result;
  }
  void remember(String key,Result value){memory.put(key,value);while(memory.size()>12){String oldest=memory.keySet().iterator().next();if(memory.get(oldest).loading)break;memory.remove(oldest);}}
  static String cleanQuery(String text){return text==null?"":text.replaceAll("[\\p{Cntrl}]"," ").trim().substring(0,Math.min(80,text.replaceAll("[\\p{Cntrl}]"," ").trim().length()));}
  static String endpoint(Request r){
    if(r.kind.equals("movie"))return "https://query.wikidata.org/sparql?format=json&query="+Uri.encode(movieQuery(r));
    if(r.recent)return "https://api.tvmaze.com/schedule/web?date="+LocalDate.now();
    if(!r.query.isEmpty())return "https://api.tvmaze.com/search/shows?q="+Uri.encode(r.query);
    return "https://api.tvmaze.com/shows?page="+r.page;
  }
  static String languageQid(String lang){
    switch(lang){case "Hindi":return "Q1568";case "Tamil":return "Q5885";case "Telugu":return "Q8097";case "Malayalam":return "Q36236";case "Kannada":return "Q3365";case "English":return "Q1860";default:return "";}
  }
  static String movieQuery(Request r){
    String lang=languageQid(r.language),search="";
    if(!r.query.isEmpty())search="?movie rdfs:label ?searchLabel . FILTER(LANG(?searchLabel) IN (\"en\",\"hi\",\"ta\",\"te\",\"ml\",\"kn\")) FILTER(CONTAINS(LCASE(STR(?searchLabel)),LCASE("+JSONObject.quote(r.query)+")))";
    String start=r.recent?" HAVING(MIN(?released) >= \""+LocalDate.now().minusDays(90)+"T00:00:00Z\"^^xsd:dateTime) ":"";
    return "SELECT ?movie ?movieLabel ?movieDescription ?pubDate ?image ?duration WHERE { { SELECT ?movie (MIN(?released) AS ?pubDate) WHERE { ?movie wdt:P31 wd:Q11424 ; wdt:P577 ?released . "+
      (lang.isEmpty()?"":"?movie wdt:P364 wd:"+lang+" . ")+
      (r.kids?"?movie wdt:P136/wdt:P279* wd:Q2143665 . ":"")+search+
      " FILTER(?released <= \""+LocalDate.now()+"T23:59:59Z\"^^xsd:dateTime) } GROUP BY ?movie "+start+" ORDER BY DESC(?pubDate) LIMIT 40 } "+
      "OPTIONAL { ?movie wdt:P18 ?image . } OPTIONAL { ?movie p:P2047/psv:P2047 ?durationNode . ?durationNode wikibase:quantityAmount ?duration ; wikibase:quantityUnit wd:Q7727 . } "+
      "SERVICE wikibase:label { bd:serviceParam wikibase:language \"en,hi,ta,te,ml,kn\". } } ORDER BY DESC(?pubDate)";
  }
  static ArrayList<Card> parseShows(String raw,Request request)throws JSONException{
    JSONArray records=new JSONArray(raw);ArrayList<Card> list=new ArrayList<>();Set<String> seen=new HashSet<>();
    for(int i=0;i<records.length() && list.size()<40;i++){
      JSONObject entry=records.optJSONObject(i);if(entry==null)continue;
      JSONObject show=request.recent?(entry.optJSONObject("_embedded")==null?entry.optJSONObject("show"):entry.optJSONObject("_embedded").optJSONObject("show")):entry.optJSONObject("show");
      if(show==null && !request.recent)show=entry;if(show==null)continue;
      String id=text(show,"id"),title=text(show,"name");if(!id.matches("[0-9]+") || title.isEmpty() || !seen.add(id))continue;
      Card c=new Card();c.id=id;c.kind="show";c.title=title;c.source="TVmaze";c.url="https://www.tvmaze.com/shows/"+id;
      c.summary=Html.fromHtml(text(show,"summary"),Html.FROM_HTML_MODE_LEGACY).toString().trim();c.language=text(show,"language");
      JSONArray genres=show.optJSONArray("genres");if(genres!=null)for(int n=0;n<genres.length();n++){String genre=genres.optString(n);if(!genre.isEmpty())c.genres.add(genre);}
      c.children=c.genres.contains("Children") || c.genres.contains("Family");
      if(request.kids && !c.children || !request.language.equals("All languages") && !request.language.equalsIgnoreCase(c.language))continue;
      JSONObject image=show.optJSONObject("image");if(image!=null)c.image=safeImage(text(image,"medium"),"static.tvmaze.com");
      c.date=text(request.recent?entry:show,request.recent?"airdate":"premiered");
      if(request.recent && !c.date.equals(LocalDate.now().toString()))continue;
      int runtime=(request.recent?entry:show).optInt(request.recent?"runtime":"averageRuntime",0);if(runtime>0)c.runtime=runtime+" min"+(request.recent?" · episode":" / episode");
      JSONObject network=show.optJSONObject("webChannel");if(network==null)network=show.optJSONObject("network");if(network!=null)c.network=text(network,"name");
      if(request.recent){int season=entry.optInt("season",0),episode=entry.optInt("number",0);if(season>0)c.runtime="S"+season+(episode>0?" E"+episode:"")+(c.runtime.isEmpty()?"":" · "+c.runtime);}
      list.add(c);
    }return list;
  }
  static ArrayList<Card> parseMovies(String raw,Request request)throws JSONException{
    JSONObject root=new JSONObject(raw);JSONObject results=root.optJSONObject("results");if(results==null || results.optJSONArray("bindings")==null)throw new JSONException("Missing bindings");
    JSONArray records=results.getJSONArray("bindings");ArrayList<Card> list=new ArrayList<>();Set<String> seen=new HashSet<>();
    for(int i=0;i<records.length() && list.size()<40;i++){
      JSONObject row=records.optJSONObject(i);if(row==null)continue;
      String entity=value(row,"movie"),id=entity.substring(entity.lastIndexOf('/')+1),title=value(row,"movieLabel"),date=value(row,"pubDate");
      if(!id.matches("Q[1-9][0-9]*") || title.isEmpty() || title.matches("Q[0-9]+") || !seen.add(id))continue;
      if(date.length()<10)continue;date=date.substring(0,10);
      try{LocalDate day=LocalDate.parse(date);if(day.isAfter(LocalDate.now()) || request.recent && day.isBefore(LocalDate.now().minusDays(90)))continue;}catch(Exception e){continue;}
      Card c=new Card();c.id=id;c.kind="movie";c.title=title;c.source="Wikidata";c.url="https://www.wikidata.org/wiki/"+id;c.summary=value(row,"movieDescription");c.date=date;
      c.image=safeImage(value(row,"image").replace("http://commons.wikimedia.org/","https://commons.wikimedia.org/"),"commons.wikimedia.org");
      c.language=request.language.equals("All languages")?"Language not loaded":request.language;c.children=request.kids;
      if(c.children)c.genres.add("Children’s film collection");
      try{double duration=Double.parseDouble(value(row,"duration"));if(duration>0 && duration<2000)c.runtime=Math.round(duration)+" min";}catch(Exception ignored){}
      list.add(c);
    }return list;
  }
  static String text(JSONObject o,String field){return o==null || o.isNull(field)?"":o.optString(field,"");}
  static String value(JSONObject o,String key){return text(o.optJSONObject(key),"value");}
  static String safeImage(String address,String host){try{Uri u=Uri.parse(address);return "https".equals(u.getScheme()) && host.equals(u.getHost()) && u.getUserInfo()==null?address:"";}catch(Exception e){return "";}}
  static String get(String address)throws Exception{
    URI uri=new URI(address);if(!"https".equals(uri.getScheme()) || uri.getUserInfo()!=null || !Arrays.asList("api.tvmaze.com","query.wikidata.org","commons.wikimedia.org").contains(uri.getHost()))throw new IOException("Unsupported catalogue host");
    HttpURLConnection c=(HttpURLConnection)uri.toURL().openConnection();
    try{
      c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setUseCaches(false);
      c.setRequestProperty("User-Agent","FlowTV/0.4.2 (https://github.com/sduos4029-CODE/TVAPP)");c.setRequestProperty("Accept","application/json");
      int code=c.getResponseCode();if(code!=200)throw new IOException("Source returned HTTP "+code);
      if(c.getContentLength()>2*1024*1024)throw new IOException("Source response exceeds limit");
      try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
        byte[] buffer=new byte[8192];int read,total=0;while((read=in.read(buffer))!=-1){total+=read;if(total>2*1024*1024)throw new IOException("Source response exceeds limit");out.write(buffer,0,read);}return out.toString("UTF-8");
      }
    }finally{c.disconnect();}
  }
  static String error(Exception e){if(e instanceof JSONException)return "Source returned unreadable catalogue data.";String message=e.getMessage();return message!=null && message.startsWith("Source returned HTTP ")?message:"Catalogue could not be reached. Check connectivity, source availability or network restrictions.";}
  File file(Request r){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(r.key().getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:digest)s.append(String.format(Locale.ROOT,"%02x",b));return new File(folder,s+".json");}catch(Exception e){throw new IllegalStateException(e);}}
  Result read(Request r){try{File f=file(r);if(f.length()>512*1024)return null;JSONObject o=new JSONObject(new String(java.nio.file.Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));Result result=new Result();result.time=o.getLong("time");if(result.time>System.currentTimeMillis() || System.currentTimeMillis()-result.time>7*24*TTL*2)return null;JSONArray cards=o.getJSONArray("cards");for(int i=0;i<Math.min(40,cards.length());i++)result.cards.add(Card.from(cards.getJSONObject(i)));result.cached=true;return result;}catch(Exception e){return null;}}
  void write(Request request,Result result){
    try{JSONArray cards=new JSONArray();for(Card c:result.cards)cards.put(c.json());String content=new JSONObject().put("time",result.time).put("cards",cards).toString();if(content.length()>512*1024)return;try(FileOutputStream out=new FileOutputStream(file(request))){out.write(content.getBytes(StandardCharsets.UTF_8));}File[] files=folder.listFiles();if(files!=null && files.length>12){Arrays.sort(files,Comparator.comparingLong(File::lastModified));for(int i=0;i<files.length-12;i++)files[i].delete();}}catch(Exception ignored){}
  }
  ArrayList<Card> saved(){ArrayList<Card> result=new ArrayList<>();try{JSONArray array=new JSONArray(a.prefs.getString("openCatalogueList","[]"));for(int i=0;i<Math.min(100,array.length());i++){Card c=Card.from(array.getJSONObject(i));if(!c.id.isEmpty() && !c.title.isEmpty())result.add(c);}}catch(Exception ignored){}return result;}
  boolean isSaved(Card card){for(Card c:saved())if(c.key().equals(card.key()))return true;return false;}
  void toggle(Card card){try{boolean remove=isSaved(card);JSONArray result=new JSONArray();for(Card c:saved())if(!c.key().equals(card.key()))result.put(c.json());if(!remove){if(result.length()>=100){a.toast("My list is full");return;}result.put(card.json());}a.prefs.edit().putString("openCatalogueList",result.toString()).apply();a.toast(remove?"Removed from My list":"Saved title");}catch(JSONException ignored){}}
  void clear(){generation++;memory.clear();File[] files=folder.listFiles();if(files!=null)for(File f:files)f.delete();}
  void close(){closed=true;worker.shutdownNow();}
}
