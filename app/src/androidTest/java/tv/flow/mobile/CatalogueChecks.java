package tv.flow.mobile;

import android.os.SystemClock;
import org.json.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Synthetic parser/transport fixtures. Never represents an actual TVmaze/Wikidata connection. */
final class CatalogueChecks {
  static String shows()throws Exception{
    JSONArray records=new JSONArray();
    records.put(new JSONObject().put("id",42).put("name","Synthetic family series").put("language",JSONObject.NULL).put("premiered",JSONObject.NULL).put("summary","<p>Fixture &amp; parser test</p>").put("genres",new JSONArray().put("Children")).put("image",new JSONObject().put("medium","https://static.tvmaze.com/uploads/images/medium_portrait/fixture.jpg")));
    records.put(new JSONObject().put("id",43).put("name","Synthetic adult anime").put("language","Japanese").put("genres",new JSONArray().put("Anime")));
    return records.toString();
  }
  static String movies()throws Exception{
    JSONArray rows=new JSONArray();
    rows.put(movie("Q999999991","Synthetic movie fixture",LocalDate.now().minusDays(2).toString()));
    rows.put(movie("Q999999992","Future fixture",LocalDate.now().plusDays(10).toString()));
    rows.put(movie("Q999999991","Duplicate fixture",LocalDate.now().minusDays(2).toString()));
    rows.put(movie("Q999999993","Q999999993",LocalDate.now().toString()));
    rows.put(movie("Q999999994","Old fixture","2000-01-01"));
    return new JSONObject().put("results",new JSONObject().put("bindings",rows)).toString();
  }
  static JSONObject movie(String id,String title,String date)throws Exception{return new JSONObject().put("movie",new JSONObject().put("value","http://www.wikidata.org/entity/"+id)).put("movieLabel",new JSONObject().put("value",title)).put("pubDate",new JSONObject().put("value",date+"T00:00:00Z")).put("youtubeId",new JSONObject().put("value","not-a-verified-trailer"));}
  static void await(FlowSmoke t,OpenCatalogue.Result result)throws Exception{long end=SystemClock.uptimeMillis()+8000;final boolean[] pending={true};while(SystemClock.uptimeMillis()<end){t.runOnMainSync(()->pending[0]=result.loading);if(!pending[0])return;SystemClock.sleep(75);}throw new AssertionError("Fixture transport did not finish");}
  static void run(FlowSmoke t)throws Exception{
    MainActivity a=t.app;String rawShows=shows(),rawMovies=movies();
    OpenCatalogue.Request show=new OpenCatalogue.Request("show","","All languages",false,false,0);
    OpenCatalogue.Request recentMovie=new OpenCatalogue.Request("movie","","Hindi",false,true,0);
    t.test("public show parser preserves missing fields and source identity",()->{
      try{ArrayList<OpenCatalogue.Card> cards=OpenCatalogue.parseShows(rawShows,show);t.check(cards.size()==2,"Series not parsed");OpenCatalogue.Card c=cards.get(0);t.check(c.language.isEmpty() && c.date.isEmpty(),"Missing source fields fabricated");t.check(c.summary.equals("Fixture & parser test"),"Summary HTML not decoded");t.check(c.kind.equals("show") && c.source.equals("TVmaze"),"Wrong source/type");}catch(Exception e){throw new AssertionError(e);}
    });
    t.test("anime alone never qualifies for the children collection",()->{try{ArrayList<OpenCatalogue.Card> cards=OpenCatalogue.parseShows(rawShows,new OpenCatalogue.Request("show","","All languages",true,false,0));t.check(cards.size()==1 && cards.get(0).children,"Unrated anime inferred as child-safe");}catch(Exception e){throw new AssertionError(e);}});
    t.test("new episodes require actual source schedule date",()->{
      try{JSONObject showObject=new JSONArray(rawShows).getJSONObject(0);JSONArray schedule=new JSONArray().put(new JSONObject().put("airdate",LocalDate.now().toString()).put("season",2).put("number",3).put("_embedded",new JSONObject().put("show",showObject))).put(new JSONObject().put("airdate","2001-01-01").put("_embedded",new JSONObject().put("show",new JSONArray(rawShows).getJSONObject(1))));ArrayList<OpenCatalogue.Card> cards=OpenCatalogue.parseShows(schedule.toString(),new OpenCatalogue.Request("show","","All languages",false,true,0));t.check(cards.size()==1 && cards.get(0).runtime.startsWith("S2 E3"),"Directory sorting passed off as episode releases");}catch(Exception e){throw new AssertionError(e);}
    });
    t.test("new film parser rejects future dates duplicates and unlabeled entities",()->{try{ArrayList<OpenCatalogue.Card> cards=OpenCatalogue.parseMovies(rawMovies,recentMovie);t.check(cards.size()==1 && cards.get(0).title.equals("Synthetic movie fixture"),"Release filtering failed");t.check(cards.get(0).url.contains("wikidata.org/wiki/") && cards.get(0).network.isEmpty(),"Metadata link became playback/provider availability");}catch(Exception e){throw new AssertionError(e);}});
    t.test("movie search safely quotes input and uses actual language mapping",()->{String input="a\" ) } UNION { ?secret ?p ?o } # \\ test";String query=OpenCatalogue.movieQuery(new OpenCatalogue.Request("movie",input,"Kannada",false,false,0));t.check(query.contains(JSONObject.quote(input)) && query.contains("wd:Q3365"),"Search interpolation unsafe or language mapping wrong");t.check(query.contains("MIN(?released)") && !query.contains("P1651"),"Later re-release or generic video inferred as trailer");});
    t.test("catalogue image URLs reject cleartext credentials and spoofed hosts",()->{t.check(OpenCatalogue.safeImage("http://static.tvmaze.com/image","static.tvmaze.com").isEmpty(),"Cleartext image accepted");t.check(OpenCatalogue.safeImage("https://static.tvmaze.com.evil/image","static.tvmaze.com").isEmpty(),"Spoofed host accepted");t.check(OpenCatalogue.safeImage("https://secret@static.tvmaze.com/image","static.tvmaze.com").isEmpty(),"Credential URL accepted");});
    t.test("Commons artwork requires a permitted licence and source attribution",()->{
      try{JSONObject meta=new JSONObject().put("LicenseUrl",new JSONObject().put("value","https://creativecommons.org/licenses/by-sa/4.0/")).put("Artist",new JSONObject().put("value","<b>Fixture creator</b>")).put("LicenseShortName",new JSONObject().put("value","CC BY-SA 4.0"));JSONObject image=new JSONObject().put("thumburl","https://upload.wikimedia.org/fixture.jpg").put("descriptionurl","https://commons.wikimedia.org/wiki/File:Fixture.jpg").put("extmetadata",meta);JSONObject payload=new JSONObject().put("query",new JSONObject().put("pages",new JSONObject().put("1",new JSONObject().put("imageinfo",new JSONArray().put(image)))));t.check(CatalogueImages.parse(payload.toString()).artist.equals("Fixture creator"),"Image credit not retained");meta.put("LicenseUrl",new JSONObject().put("value","https://example.invalid/copyright"));t.check(CatalogueImages.parse(payload.toString()).image.isEmpty(),"Unverified image licence displayed");}catch(Exception e){throw new AssertionError(e);}
    });
    t.test("network rates use actual deltas and elapsed time",()->{TrafficReading r=new TrafficReading();r.sample(1000,2000,1000);t.check(r.rxPerSecond<0,"Rate fabricated from one sample");r.sample(3000,5000,3000);t.check(r.rxPerSecond==1000 && r.txPerSecond==1500,"Counter rates incorrect");r.reset();t.check(r.elapsed<0,"Leaving Dashboard did not reset baseline");});
    t.test("unsupported and reset traffic counters never become fabricated zero readings",()->{TrafficReading r=new TrafficReading();r.sample(-1,100,1000);t.check(r.rx<0 && r.rxPerSecond<0,"Unsupported counter coerced to zero");r.sample(500,500,2000);r.sample(100,100,3000);t.check(r.rxPerSecond<0,"Counter reset inferred as live rate");});
    t.test("networking off blocks catalogue and artwork work including resolved credits",()->{
      a.prefs.edit().putBoolean("catalogueEnabled",false).apply();
      t.check(!a.openCatalogue.fetch(show,true).loading,"Disabled source request started");
      android.widget.ImageView image=new android.widget.ImageView(a);android.widget.TextView label=new android.widget.TextView(a);
      CatalogueImages.Credit credit=new CatalogueImages.Credit();credit.image="https://upload.wikimedia.org/fixture.jpg";
      a.catalogueImages.show(credit,image,label);t.check(label.getText().toString().contains("networking off"),"Resolved image bypassed switch");
      a.sharedArtwork.load("https://static.tvmaze.com/fixture.jpg",image,label,true);t.check(label.getText().toString().contains("networking off"),"Image loader bypassed switch");
    });
    final OpenCatalogue[] repository=new OpenCatalogue[1];final OpenCatalogue.Result[] loaded=new OpenCatalogue.Result[1];AtomicInteger calls=new AtomicInteger();
    t.test("public catalogue fetch runs through asynchronous transport",()->{
      a.navigate("Dashboard");a.prefs.edit().putBoolean("catalogueEnabled",true).apply();
      repository[0]=new OpenCatalogue(a);repository[0].clear();repository[0].transport=url->{calls.incrementAndGet();return rawShows;};
      loaded[0]=repository[0].fetch(show,true);t.check(loaded[0].loading,"Request did not enter loading state");
    });await(t,loaded[0]);
    t.test("successful fixture response caches actual parsed records without another request",()->{t.check(loaded[0].error.isEmpty() && loaded[0].cards.size()==2,"Fixture transport parse failed");t.check(repository[0].fetch(show,false)==loaded[0] && calls.get()==1,"Fresh metadata requested again");OpenCatalogue.Result disk=repository[0].read(show);t.check(disk!=null && disk.cards.size()==2 && disk.cached,"Disk cache not readable");});
    t.test("failed refresh retains cached metadata with an explicit stale error",()->{repository[0].transport=url->{throw new java.io.IOException("fixture network unavailable");};loaded[0]=repository[0].fetch(show,true);});await(t,loaded[0]);
    t.test("stale source state is visible and failure retries are bounded",()->{t.check(loaded[0].stale && loaded[0].cached && !loaded[0].error.isEmpty(),"Stale metadata passed off as live");long attempt=loaded[0].lastAttempt;repository[0].fetch(show,false);t.check(loaded[0].lastAttempt==attempt && !loaded[0].loading,"Failure immediately retried");});
    t.test("real details controls persist public metadata without a fake Watch action",()->{
      OpenCatalogue.Card card=loaded[0].cards.get(0);a.navigate("My list");new CatalogueScreen(a,a.design).details(card);t.click("+ My list");a.onBackPressed();t.check(a.openCatalogue.isSaved(card) && t.contains(a.content,"Synthetic family series"),"Catalogue title not saved in visible list");
      new CatalogueScreen(a,a.design).details(card);t.check(t.find(a.drawer,"Watch now")==null && t.contains(a.drawer,"No verified provider title link"),"Unsupported provider watch offered");t.click("Remove from My list");a.onBackPressed();t.check(!a.openCatalogue.isSaved(card),"Catalogue title not removed");
    });
    t.test("provider fallback explains app-only launch and handles missing apps",()->{new CatalogueScreen(a,a.design).providers(loaded[0].cards.get(0));t.check(t.contains(a.drawer,"not a verified page") && t.find(a.drawer,"All installed apps")!=null,"Provider fallback misleading");a.closeDrawer();});
    t.test("catalogue search submits through actual native controls",()->{a.openCatalogue.transport=url->{throw new java.io.IOException("deliberately offline instrumentation");};new CatalogueScreen(a,a.design).search();android.widget.EditText input=t.editText(a.drawer);input.setText("Synthetic query");t.click("Search films");t.check(a.page.equals("Movies") && a.catalogueQuery.equals("Synthetic query") && t.contains(a.content,"Search: Synthetic query"),"Search not connected to catalogue view");a.catalogueQuery="";a.navigate("Dashboard");});
    t.runOnMainSync(()->{repository[0].clear();repository[0].close();a.openCatalogue.clear();a.openCatalogue.transport=OpenCatalogue::get;a.prefs.edit().remove("openCatalogueList").putBoolean("catalogueEnabled",false).apply();a.navigate("Dashboard");});
  }
}
