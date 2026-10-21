package tv.flow.mobile;

import android.net.Uri;
import android.text.Html;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Commons artwork is displayed only after its public licence and source credit resolve. */
final class CatalogueImages {
  static final class Credit {String image="",page="",artist="",license="",licenseUrl="";}
  final MainActivity a;final ExecutorService worker=Executors.newFixedThreadPool(2);
  final Map<String,Credit> credits=Collections.synchronizedMap(new LinkedHashMap<>());
  volatile boolean closed;
  CatalogueImages(MainActivity activity){a=activity;}
  void load(OpenCatalogue.Card card,ImageView image,TextView label){
    if(!a.openCatalogue.enabled()){label.setText("Catalogue networking off");return;}
    if(card.source.equals("TVmaze")){a.sharedArtwork.load(OpenCatalogue.safeImage(card.image,"static.tvmaze.com"),image,label,true);return;}
    String address=OpenCatalogue.safeImage(card.image,"commons.wikimedia.org");
    if(address.isEmpty()){label.setText("No source image");return;}
    Credit cached=credits.get(address);if(cached!=null){show(cached,image,label);return;}
    label.setText("Checking image credit…");
    worker.execute(()->{
      Credit credit=new Credit();
      try{
        if(closed || !a.openCatalogue.enabled())return;
        Uri uri=Uri.parse(address);String path=uri.getPath();int marker=path.indexOf("Special:FilePath/");
        if(marker<0)throw new IllegalArgumentException();String name=path.substring(marker+17);if(name.isEmpty() || name.length()>255)throw new IllegalArgumentException();
        String endpoint="https://commons.wikimedia.org/w/api.php?action=query&format=json&prop=imageinfo&iiprop=url%7Cextmetadata&iiurlwidth=320&titles="+Uri.encode("File:"+name);
        credit=parse(OpenCatalogue.get(endpoint));
      }catch(Exception ignored){}
      final Credit result=credit;
      a.handler.post(()->{if(closed || a.isDestroyed() || !a.openCatalogue.enabled())return;credits.put(address,result);while(credits.size()>40)credits.remove(credits.keySet().iterator().next());show(result,image,label);});
    });
  }
  static Credit parse(String raw)throws JSONException{
    Credit c=new Credit();JSONObject pages=new JSONObject(raw).getJSONObject("query").getJSONObject("pages");
    Iterator<String> keys=pages.keys();if(!keys.hasNext())return c;
    JSONArray info=pages.getJSONObject(keys.next()).optJSONArray("imageinfo");if(info==null || info.length()==0)return c;
    JSONObject image=info.getJSONObject(0),meta=image.optJSONObject("extmetadata");if(meta==null)return c;
    String license=OpenCatalogue.value(meta,"LicenseUrl").replace("http://creativecommons.org/","https://creativecommons.org/");
    boolean publicDomain=license.startsWith("https://creativecommons.org/publicdomain/");
    boolean permitted=publicDomain || license.startsWith("https://creativecommons.org/licenses/by/") || license.startsWith("https://creativecommons.org/licenses/by-sa/");
    String artist=Html.fromHtml(OpenCatalogue.value(meta,"Artist"),Html.FROM_HTML_MODE_LEGACY).toString().trim();
    if(!permitted || !publicDomain && artist.isEmpty())return c;
    c.image=OpenCatalogue.safeImage(OpenCatalogue.text(image,"thumburl"),"upload.wikimedia.org");
    c.page=OpenCatalogue.safeImage(OpenCatalogue.text(image,"descriptionurl"),"commons.wikimedia.org");
    if(c.page.isEmpty()){c.image="";return c;}
    c.license=OpenCatalogue.value(meta,"LicenseShortName");c.licenseUrl=license;c.artist=artist.length()>500?artist.substring(0,500):artist;return c;
  }
  void show(Credit credit,ImageView image,TextView label){if(!a.openCatalogue.enabled())label.setText("Catalogue networking off");else if(credit.image.isEmpty())label.setText("Source image unavailable\nor licence not verified");else a.sharedArtwork.load(credit.image,image,label,true);}
  void close(){closed=true;worker.shutdownNow();credits.clear();}
}
