package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/** Registers one foreground-only discovery attempt for CLEAN 06. */
public final class RedLightDiscoveryProvider extends ContentProvider {
    private static final String TAG="ProjectivyRedLight";
    private static final AtomicBoolean STARTED=new AtomicBoolean(false);

    @Override public boolean onCreate(){
        Context c=getContext(); if(c==null)return true;
        Application app=(Application)c.getApplicationContext();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            boolean done;
            @Override public void onActivityResumed(Activity a){
                if(done||!a.getClass().getName().equals("com.spocky.projengmenu.ui.home.MainActivity"))return;
                done=true; app.unregisterActivityLifecycleCallbacks(this);
                if(!STARTED.compareAndSet(false,true))return;
                Log.i(TAG,"CLEAN06 discovery armed for MainActivity resume");
                new Thread(() -> {
                    try{Thread.sleep(1800L);}catch(InterruptedException ignored){}
                    boolean ok=RedLightDiscoveryEngine.syncOnce(app);
                    Log.i(TAG,ok?"CLEAN06 discovery COMPLETE":"CLEAN06 discovery NO_ROW");
                },"clean06-redlight").start();
            }
            public void onActivityCreated(Activity a,Bundle b){}
            public void onActivityStarted(Activity a){}
            public void onActivityPaused(Activity a){}
            public void onActivityStopped(Activity a){}
            public void onActivitySaveInstanceState(Activity a,Bundle b){}
            public void onActivityDestroyed(Activity a){}
        });
        return true;
    }
    public String getType(Uri u){return null;} public Cursor query(Uri u,String[]p,String s,String[]a,String o){return null;} public Uri insert(Uri u,ContentValues v){return null;} public int delete(Uri u,String s,String[]a){return 0;} public int update(Uri u,ContentValues v,String s,String[]a){return 0;}
}