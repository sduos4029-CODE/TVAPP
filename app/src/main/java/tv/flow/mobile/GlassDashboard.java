package tv.flow.mobile;

import android.app.ActivityManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.TrafficStats;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Rounded Option C widgets; every value comes from the installed device. */
final class GlassDashboard {
  final MainActivity a;
  final FlowDesign d;
  final Map<String,TextView> values=new LinkedHashMap<>();
  final Map<String,TextView> captions=new HashMap<>();
  final Map<String,Ring> rings=new HashMap<>();
  final Map<String,View> widgets=new HashMap<>();
  final int slate=0xff141c2e, rim=0xff304457;
  GlassDashboard(MainActivity activity,FlowDesign design){a=activity;d=design;}

  void render(){
    LinearLayout heading=d.row();
    LinearLayout words=d.col();
    words.addView(d.bold("Your device, at a glance.",26,FlowDesign.TEXT));
    words.addView(d.label("MINIMAL GLASS  /  "+(a.tv()?"LIVE TV DATA":"LIVE PHONE DATA"),10,FlowDesign.MUTED));
    heading.addView(words,new LinearLayout.LayoutParams(0,-2,1));
    Button settings=d.button("⚙  Device settings",()->a.openSettings(android.provider.Settings.ACTION_SETTINGS));
    if(a.landscape())heading.addView(settings);
    a.content.addView(heading);d.gap(a.content,8);
    List<String> top=Arrays.asList("RAM","Storage","CPU");
    List<String> bottom=new ArrayList<>();bottom.add("Wi-Fi");
    if(a.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))bottom.add("Bluetooth");
    if(TrafficStats.getTotalRxBytes()>=0 && TrafficStats.getTotalTxBytes()>=0)bottom.add("Network");
    if(!a.landscape()){
      ArrayList<String> all=new ArrayList<>(top);all.addAll(bottom);grid(all,2);
    }else{grid(top,3);grid(bottom,3);}
    LinearLayout ports=widget("Ports & Inputs",false);
    LinearLayout.LayoutParams pl=new LinearLayout.LayoutParams(-1,d.u(a.landscape()?88:117));
    pl.setMargins(d.u(5),d.u(7),d.u(5),d.u(4));a.content.addView(ports,pl);
    LinearLayout footer=d.row();
    TextView model=d.label(Build.MANUFACTURER+" "+Build.MODEL+"   ·   Android "+Build.VERSION.RELEASE+"   ·   Flow "+a.appVersion(),11,FlowDesign.MUTED);
    footer.addView(model,new LinearLayout.LayoutParams(0,-2,1));
    TextView chip=d.label(a.tv()?"TV readings":"Phone readings",10,FlowDesign.LAV);
    chip.setBackground(d.border(0xff202a43,0xff414365,14,1));
    chip.setPadding(d.u(11),d.u(4),d.u(11),d.u(4));footer.addView(chip);
    footer.setPadding(d.u(12),d.u(8),d.u(12),d.u(8));
    footer.setBackground(d.border(slate,rim,18,1));
    footer.setContentDescription("System, actual device information, open details");
    d.focus(footer,()->a.detail("System"));
    a.content.addView(footer);
    d.note(a.content,"Tap a widget for details · unavailable measurements are hidden");
    refresh();
  }
  void grid(List<String> keys,int columns){
    for(int start=0;start<keys.size();start+=columns){
      LinearLayout row=d.row();
      for(int n=start;n<Math.min(start+columns,keys.size());n++){
        String key=keys.get(n);LinearLayout widget=widget(key,true);
        int h=(key.equals("RAM")||key.equals("Storage")||key.equals("CPU"))?155:124;
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,d.u(a.landscape()?h:159),1);
        lp.setMargins(d.u(5),d.u(5),d.u(5),d.u(5));row.addView(widget,lp);
      }
      while(row.getChildCount()<columns)row.addView(new View(a),new LinearLayout.LayoutParams(0,1,1));
      a.content.addView(row);
    }
  }
  LinearLayout widget(String key,boolean large){
    int accent=key.equals("Storage")||key.equals("Bluetooth")?FlowDesign.LAV:FlowDesign.MINT;
    LinearLayout card=d.col();card.setPadding(d.u(18),d.u(14),d.u(18),d.u(12));
    card.setBackground(d.border(slate,rim,28,1));
    card.setClipToOutline(true);
    LinearLayout header=d.row();
    ImageView icon=new ImageView(a);
    icon.setImageDrawable(d.new LineIcon(key.toLowerCase(Locale.ROOT),accent));
    FrameLayout iconDisk=new FrameLayout(a);iconDisk.setBackground(d.solid(key.equals("Storage")||key.equals("Bluetooth")?0xff2b3150:0xff1d3c43,22));
    FrameLayout.LayoutParams il=new FrameLayout.LayoutParams(d.u(21),d.u(21),Gravity.CENTER);iconDisk.addView(icon,il);
    d.add(header,iconDisk,35,35);
    TextView title=d.label(key,12,FlowDesign.MUTED);title.setPadding(d.u(10),0,0,0);
    header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
    header.addView(d.label("›",18,FlowDesign.MUTED));card.addView(header);
    LinearLayout body=d.row();
    LinearLayout text=d.col();
    TextView value=d.bold("",key.equals("RAM")||key.equals("Storage")?21:15,FlowDesign.TEXT);
    value.setPadding(0,d.u(7),0,0);value.setMaxLines(3);text.addView(value);
    TextView caption=d.label("",10,FlowDesign.MUTED);caption.setPadding(0,d.u(3),0,0);caption.setMaxLines(3);text.addView(caption);
    body.addView(text,new LinearLayout.LayoutParams(0,-2,1));
    if(key.equals("RAM")||key.equals("Storage")){
      Ring ring=new Ring(accent);rings.put(key,ring);ImageView gauge=new ImageView(a);gauge.setImageDrawable(ring);
      d.add(body,gauge,a.landscape()?82:64,a.landscape()?82:64);
      gauge.setContentDescription(key+" used percentage gauge");
    }
    card.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    widgets.put(key,card);
    values.put(key,value);captions.put(key,caption);a.metricKeys.add(key);a.metricViews.add(value);
    d.focus(card,()->a.detail(key));
    StateListDrawable focus=new StateListDrawable();
    focus.addState(new int[]{android.R.attr.state_focused},d.border(Color.TRANSPARENT,FlowDesign.MINT,28,3));
    focus.addState(new int[]{android.R.attr.state_pressed},d.border(0x157df3d3,FlowDesign.MINT,28,2));
    focus.addState(new int[]{},new ColorDrawable(Color.TRANSPARENT));card.setForeground(focus);
    card.setContentDescription(key+", actual device widget, open details");
    return card;
  }
  void set(String key,String value,String caption){values.get(key).setText(value);captions.get(key).setText(caption);}
  void refresh(){
    for(String key:values.keySet())try{
      switch(key){
        case "RAM":{
          ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();
          ((ActivityManager)a.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(m);
          if(m.totalMem<=0)throw new IllegalStateException();
          long used=m.totalMem-m.availMem;
          set(key,a.bytes(used),"of "+a.bytes(m.totalMem)+" total\n"+a.bytes(m.availMem)+" available");
          rings.get(key).update((float)used/m.totalMem);break;
        }
        case "Storage":{
          StatFs fs=new StatFs(a.getFilesDir().getAbsolutePath());long total=fs.getTotalBytes(),free=fs.getAvailableBytes();
          if(total<=0)throw new IllegalStateException();
          set(key,a.bytes(total-free),"of "+a.bytes(total)+" total\n"+a.bytes(free)+" free · data volume");
          rings.get(key).update((float)(total-free)/total);break;
        }
        case "CPU":{
          String cpu=Build.VERSION.SDK_INT>=31 && !Build.SOC_MODEL.equalsIgnoreCase("unknown")?Build.SOC_MODEL:Build.HARDWARE;
          set(key,cpu,Build.SUPPORTED_ABIS[0]+"\n"+Runtime.getRuntime().availableProcessors()+" available cores");break;
        }
        case "Network":{
          TrafficReading r=a.trafficReading;r.sample(TrafficStats.getTotalRxBytes(),TrafficStats.getTotalTxBytes(),SystemClock.elapsedRealtime());
          if(r.rx<0 || r.tx<0)throw new IllegalStateException();
          set(key,r.rxPerSecond<0?"Measuring traffic…":"↓ "+a.bytes(r.rxPerSecond)+"/s   ↑ "+a.bytes(r.txPerSecond)+"/s","Totals since boot: ↓ "+a.bytes(r.rx)+" ↑ "+a.bytes(r.tx)+"\nAll supported interfaces, not Wi-Fi-only");break;
        }
        case "Ports & Inputs":set(key,a.metric(key,false).replace('\n',' ').replace("TV input controls: "," · TV: "),"");values.get(key).setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,d.u(12));break;
        case "Bluetooth":set(key,"Hardware present","Connection details not requested\nOpen Android settings to pair");break;
        case "Wi-Fi":{
          android.net.ConnectivityManager manager=(android.net.ConnectivityManager)a.getSystemService(Context.CONNECTIVITY_SERVICE);
          android.net.NetworkCapabilities c=manager.getNetworkCapabilities(manager.getActiveNetwork());
          set(key,c==null?"Offline":c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi connected":"Other network",c==null?"No active connection":"Network validation: "+(c.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)?"online":"not validated"));break;
        }
      }
      widgets.get(key).setVisibility(View.VISIBLE);
    }catch(Exception e){widgets.get(key).setVisibility(View.GONE);}
  }
  final class Ring extends Drawable{
    final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final int color;float fraction;
    Ring(int accent){color=accent;}
    void update(float value){fraction=Math.max(0,Math.min(1,value));invalidateSelf();}
    @Override public void draw(Canvas canvas){
      Rect b=getBounds();float side=Math.min(b.width(),b.height()),stroke=side*.085f;
      RectF arc=new RectF(b.centerX()-side*.41f,b.centerY()-side*.41f,b.centerX()+side*.41f,b.centerY()+side*.41f);
      paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setStrokeCap(Paint.Cap.ROUND);
      paint.setColor(0xff2b3d53);canvas.drawArc(arc,0,360,false,paint);
      paint.setColor(color);canvas.drawArc(arc,-90,360*fraction,false,paint);
      paint.setStyle(Paint.Style.FILL);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextAlign(Paint.Align.CENTER);
      paint.setTextSize(side*.22f);paint.setColor(FlowDesign.TEXT);
      canvas.drawText(Math.round(fraction*100)+"%",b.centerX(),b.centerY()+side*.035f,paint);
      paint.setTextSize(side*.10f);paint.setColor(FlowDesign.MUTED);canvas.drawText("USED",b.centerX(),b.centerY()+side*.20f,paint);
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
  }
}
