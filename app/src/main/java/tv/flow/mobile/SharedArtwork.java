package tv.flow.mobile;

import android.graphics.*;
import android.net.Uri;
import android.util.LruCache;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/** Loads only artwork URLs actually published with a TV card; no catalogue inference. */
final class SharedArtwork {
  final MainActivity a;
  final ExecutorService worker=Executors.newFixedThreadPool(2);
  final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(6*1024*1024){
    @Override protected int sizeOf(String key,Bitmap value){return value.getAllocationByteCount();}
  };
  SharedArtwork(MainActivity activity){a=activity;}
  void load(String address,ImageView view,TextView empty){load(address,view,empty,false);}
  void load(String address,ImageView view,TextView empty,boolean catalogue){
    if(catalogue && !a.openCatalogue.enabled()){empty.setText("Catalogue networking off");return;}
    if(address==null || address.isEmpty()){empty.setText("No shared\nartwork");return;}
    view.setTag(address);
    Bitmap stored=cache.get(address);if(stored!=null){view.setImageBitmap(stored);empty.setVisibility(View.GONE);return;}
    worker.execute(()->{
      Bitmap bitmap=null;
      try{
        if(catalogue && !a.openCatalogue.enabled())return;
        Uri uri=Uri.parse(address);InputStream input;HttpURLConnection connection=null;
        if("content".equals(uri.getScheme()) || "android.resource".equals(uri.getScheme()))input=a.getContentResolver().openInputStream(uri);
        else if("https".equals(uri.getScheme()) && uri.getHost()!=null && uri.getUserInfo()==null){
          connection=(HttpURLConnection)new URL(address).openConnection();connection.setInstanceFollowRedirects(false);
          connection.setConnectTimeout(8000);connection.setReadTimeout(8000);
          if(connection.getResponseCode()!=200 || connection.getContentLength()>3*1024*1024){connection.disconnect();throw new IOException();}
          input=connection.getInputStream();
        }else throw new IOException();
        try(InputStream stream=input;ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
          if(stream==null)throw new IOException();
          byte[] buffer=new byte[8192];int count,total=0;
          while((count=stream.read(buffer))!=-1){total+=count;if(total>3*1024*1024)throw new IOException();bytes.write(buffer,0,count);}
          byte[] data=bytes.toByteArray();BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
          BitmapFactory.decodeByteArray(data,0,data.length,bounds);
          if(bounds.outWidth<=0 || bounds.outHeight<=0)throw new IOException();
          int sample=1;while(bounds.outWidth/sample>256 || bounds.outHeight/sample>384)sample*=2;
          BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sample;options.inPreferredConfig=Bitmap.Config.RGB_565;
          bitmap=BitmapFactory.decodeByteArray(data,0,data.length,options);
        }finally{if(connection!=null)connection.disconnect();}
      }catch(Exception ignored){}
      final Bitmap result=bitmap;
      a.handler.post(()->{
        if(a.isDestroyed() || catalogue && !a.openCatalogue.enabled())return;
        if(result!=null){cache.put(address,result);if(address.equals(view.getTag())){view.setImageBitmap(result);empty.setVisibility(View.GONE);}}
        else if(address.equals(view.getTag()))empty.setText("Artwork\nunavailable");
      });
    });
  }
  void close(){worker.shutdownNow();cache.evictAll();}
}
