package com.thanksmister.iot.wallpanellegacy.root;

import android.content.Context;
import android.net.LocalServerSocket;
import android.os.Build;
import android.os.PowerManager;
import org.json.JSONObject;
import java.io.*;
import java.net.*;

/** Root process: existing app retains timeout, keys, camera and proximity ownership. */
public final class SamsungComputerDisplay {
    private static final File CONFIG = new File("/data/local/tmp/samsung-computer-display.json");
    private static final File OWNED = new File("/data/local/tmp/samsung-computer-display.owned");
    private static final File STATUS = new File("/data/local/tmp/samsung-computer-display.status");
    private static final File EVENTS = new File("/data/local/tmp/samsung-computer-display.events");
    private static LocalServerSocket singleton;
    private static void event(String value) {
        try {
            if(EVENTS.length()>65536){File old=new File(EVENTS.getPath()+".1");old.delete();EVENTS.renameTo(old);}
            FileOutputStream out=new FileOutputStream(EVENTS,true);
            out.write((System.currentTimeMillis()/1000+" "+value+"\n").getBytes("UTF-8"));out.close();
        }catch(Exception ignored){}
    }
    private static volatile boolean owned;
    private static PowerManager.WakeLock cpu;
    private static Context gateContext;
    private static boolean gateUnavailableLogged;
    private static Boolean gateLastState;
    private static void gate(boolean active) {
        if(gateContext==null || (!active && Boolean.FALSE.equals(gateLastState))) return;
        try {
            long now=android.os.SystemClock.elapsedRealtime();
            String value=active?now+":"+(now+30000):"";
            final Process process=new ProcessBuilder("/system/bin/settings","put","system",
                    "wallpanel_samsung_computer_sleep_lease",value).redirectErrorStream(true).start();
            android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
            Runnable abort=new Runnable(){public void run(){process.destroy();}};
            handler.postDelayed(abort,4000);
            try{read(process.getInputStream(),1024);if(process.waitFor()!=0)throw new IOException("gate write");}
            finally{handler.removeCallbacks(abort);}
            gateLastState=Boolean.valueOf(active);gateUnavailableLogged=false;
        }catch(Exception unavailable){if(!gateUnavailableLogged){event("wake_gate_write_failed");gateUnavailableLogged=true;}}
    }

    private static String read(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buf = new byte[1024]; int n;
        while ((n = in.read(buf)) != -1) { if (out.size()+n > limit) throw new IOException("oversize"); out.write(buf,0,n); }
        return out.toString("UTF-8");
    }
    private static void key(String value) throws Exception {
        Process p = new ProcessBuilder("/system/bin/input", "keyevent", value).redirectErrorStream(true).start();
        read(p.getInputStream(), 4096); if (p.waitFor()!=0) throw new IOException("key failed");
    }
    private static void wakeOwned() {
        gate(false);
        if (owned) {
            try { event("wake_request");key("224"); owned=false; OWNED.delete(); } catch (Exception ignored) { /* Retry next iteration. */ }
        }
        if (!owned && cpu!=null && cpu.isHeld()) cpu.release();
    }
    private static int brightness() {
        try {
            Process p=new ProcessBuilder("/system/bin/settings","get","system","screen_brightness").start();
            String value=read(p.getInputStream(),128).trim();
            return p.waitFor()==0?Integer.parseInt(value):-1;
        }catch(Exception unavailable){return -1;}
    }
    private static boolean panelForeground() throws Exception {
        Process p = new ProcessBuilder("/system/bin/dumpsys","window","windows").start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(),"UTF-8"));
        boolean found=false; String line;
        while((line=r.readLine())!=null) if(line.contains("mCurrentFocus=")) found=line.contains("com.thanksmister.iot.wallpanellegacy.samsung/") && line.contains("BrowserActivity");
        r.close();p.waitFor();return found;
    }
    private static ComputerDisplayPolicy.Presence report(URL url, long stale, boolean computerState) throws Exception {
        HttpURLConnection c=(HttpURLConnection)url.openConnection();
        c.setConnectTimeout(4000);c.setReadTimeout(4000);c.setUseCaches(false);c.setInstanceFollowRedirects(false);
        try {
            if(c.getResponseCode()!=200) throw new IOException("http");
            InputStream in=c.getInputStream();String text;try{text=read(in,8192);}finally{in.close();}
            JSONObject e=new JSONObject(text);
            if(!e.has("entity_id") || !e.has("attributes")) throw new IOException("invalid state");
            long seen=e.getJSONObject("attributes").optLong("last_report_epoch",0);
            long now=System.currentTimeMillis()/1000;
            return computerState?ComputerDisplayPolicy.computerPresence(now,seen,stale,e.optString("state",""))
                    :ComputerDisplayPolicy.presence(now,seen,stale);
        }finally{c.disconnect();}
    }
    private static void status(String mode) {
        try {
            File tmp=new File(STATUS.getPath()+".tmp");FileOutputStream out=new FileOutputStream(tmp);
            out.write((mode+" owned="+owned+"\n").getBytes("UTF-8"));out.close();tmp.renameTo(STATUS);
        }catch(Exception ignored){}
    }
    public static void main(String[] args) throws Exception {
        if(!"SM-J510FN".equals(Build.MODEL)||Build.VERSION.SDK_INT!=23||!CONFIG.isFile()) return;
        try{singleton=new LocalServerSocket("wallpanel-samsung-computer-display");}catch(IOException duplicate){return;}
        FileInputStream in=new FileInputStream(CONFIG);JSONObject cfg;try{cfg=new JSONObject(read(in,8192));}finally{in.close();}
        final URL url=new URL(cfg.getString("url"));
        if(!("http".equals(url.getProtocol())||"https".equals(url.getProtocol()))||url.getUserInfo()!=null) throw new IOException("invalid url");
        final long stale=Math.max(60,cfg.optLong("stale_seconds",120));
        final boolean computerState=cfg.optBoolean("computer_state",false);
        final long runSeconds=args.length==2&&"--test-seconds".equals(args[0])?Math.max(10,Math.min(180,Long.parseLong(args[1]))):0;
        android.os.Looper.prepareMainLooper();
        Class<?> at=Class.forName("android.app.ActivityThread");Object thread=at.getMethod("systemMain").invoke(null);
        final Context context=(Context)at.getMethod("getSystemContext").invoke(thread);
        gateContext=context;
        final PowerManager pm=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        cpu=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"wallPanel:computerDisplay");cpu.setReferenceCounted(false);
        owned=OWNED.isFile();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){public void run(){wakeOwned();}}));
        new Thread(new Runnable(){public void run(){
            try{runPolicy(context,pm,url,stale,runSeconds,computerState);}catch(Exception failure){status("ERROR");System.err.println("Policy failure: "+failure.getClass().getSimpleName());for(StackTraceElement frame:failure.getStackTrace())System.err.println(frame.getClassName()+"."+frame.getMethodName()+":"+frame.getLineNumber());}
            finally{wakeOwned();System.exit(0);}
        }},"computer-display-policy").start();
        android.os.Looper.loop();
    }
    private static void runPolicy(Context context,PowerManager pm,URL url,long stale,long runSeconds,boolean computerState) throws Exception {
        long started=android.os.SystemClock.elapsedRealtime(),offlineSince=-1;int failures=0;
        ComputerDisplayPolicy.Presence previous=null;
        try {
            while(runSeconds==0 || android.os.SystemClock.elapsedRealtime()-started<runSeconds*1000) {
                if(owned){cpu.acquire(90000);if(pm.isInteractive()){event("external_wake_observed");owned=false;OWNED.delete();gate(false);cpu.release();}else{gate(true);}}
                ComputerDisplayPolicy.Presence presence;
                try{presence=report(url,stale,computerState);failures=0;}
                catch(Exception unavailable){presence=ComputerDisplayPolicy.Presence.UNKNOWN;failures++;}
                if(presence!=previous){event("presence="+presence.name()+" owned="+owned+" failures="+failures);previous=presence;}
                long now=android.os.SystemClock.elapsedRealtime();
                if(presence==ComputerDisplayPolicy.Presence.OFFLINE){
                    if(offlineSince<0) offlineSince=now;
                }else{offlineSince=-1;}
                if(presence==ComputerDisplayPolicy.Presence.ONLINE){gate(false);if(ComputerDisplayPolicy.shouldWake(owned,presence)){wakeOwned();}}
                else if(presence==ComputerDisplayPolicy.Presence.OFFLINE && pm.isInteractive() && panelForeground()) {
                    int level=brightness();
                    if(ComputerDisplayPolicy.shouldSleep(presence,true,true,level,now-offlineSince)) {
                        event("sleep_request brightness="+level+" offline_ms="+(now-offlineSince));
                        cpu.acquire(90000);OWNED.createNewFile();owned=true;gate(true);
                        try{key("223");}catch(Exception failed){event("sleep_request_failed");wakeOwned();}
                    }
                }
                status(presence.name());Thread.sleep(10000);
            }
        }finally{wakeOwned();status("STOPPED");singleton.close();}
    }
}
