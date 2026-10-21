package tv.flow.mobile;

/** Actual byte-counter deltas, measured only while Dashboard is visible. -1 means unsupported. */
final class TrafficReading {
  long rx=-1,tx=-1,elapsed=-1,rxPerSecond=-1,txPerSecond=-1;
  void reset(){rx=tx=elapsed=rxPerSecond=txPerSecond=-1;}
  void sample(long received,long sent,long now){
    rxPerSecond=txPerSecond=-1;
    if(received<0 || sent<0){reset();return;}
    long interval=now-elapsed;
    if(elapsed>=0 && interval>=500 && received>=rx && sent>=tx){
      rxPerSecond=(long)((received-rx)*1000.0/interval);txPerSecond=(long)((sent-tx)*1000.0/interval);
    }
    rx=received;tx=sent;elapsed=now;
  }
}
