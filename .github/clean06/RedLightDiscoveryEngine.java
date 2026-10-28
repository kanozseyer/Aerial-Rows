package com.spocky.projengmenu.embedded;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** CLEAN 06: discover Red Light and publish only Trending Movies. */
public final class RedLightDiscoveryEngine {
    private static final String TAG="ProjectivyRedLight";
    private static final String KODI_PACKAGE="org.xbmc.kodi";
    private static final String AUTH="org.xbmc.kodi.file";
    private static final String VIDEO_ADDONS="addons://sources/video/";
    private static final String KEY="redlight:shelf:trending:movies";
    private static final String TITLE="Red Light · Trending Movies";
    private static final int MAX_DEPTH=7, MAX_SCANS=72, MAX_CHILDREN=100, MAX_PROGRAMS=60;

    private RedLightDiscoveryEngine() {}

    static final class Entry {
        String name, category, uri, mediaType;
        boolean dir(){ return "directory".equalsIgnoreCase(category)||"folder".equalsIgnoreCase(category)||"directory".equalsIgnoreCase(mediaType); }
    }
    static final class Node {
        Entry e; List<String> trail; int depth;
        Node(Entry e,List<String> t,int d){this.e=e;trail=t;depth=d;}
    }

    public static boolean syncOnce(Context c) {
        try {
            c.getPackageManager().getApplicationInfo(KODI_PACKAGE,0);
            Entry root=findRedLight(c);
            if(root==null){Log.i(TAG,"CLEAN06 Red Light root unavailable");return false;}
            Entry trending=findTrendingMovies(c,root);
            if(trending==null){Log.i(TAG,"CLEAN06 Trending Movies not found");return false;}
            long channel=ensureChannel(c,trending.uri);
            if(channel<=0){Log.e(TAG,"CLEAN06 channel create/update failed");return false;}
            int programs=syncPrograms(c,channel,trending.uri);
            Log.i(TAG,"CLEAN06 SUCCESS channelId="+channel+" programs="+programs+" uri="+trending.uri);
            return programs>0;
        } catch(Throwable t){ Log.e(TAG,"CLEAN06 FAILED",t); return false; }
    }

    private static Entry findRedLight(Context c){
        List<Entry>a=list(c,VIDEO_ADDONS); if(a==null)return null;
        for(Entry e:a)if("red light".equals(norm(e.name)))return e;
        for(Entry e:a)if(norm(e.name).contains("red light"))return e;
        return null;
    }

    private static Entry findTrendingMovies(Context c,Entry root){
        List<Entry> first=list(c,raw(root.uri)); if(first==null)return null;
        ArrayDeque<Node>q=new ArrayDeque<>();
        for(Entry e:first)if(e.dir())q.add(new Node(e,single(e.name),1));
        Set<String>seen=new HashSet<>(); int scans=0;
        while(!q.isEmpty()&&scans<MAX_SCANS){
            Node n=q.removeFirst(); String u=raw(n.e.uri); if(!seen.add(u))continue;
            if(isTrendingMovies(n.e,n.trail))return n.e;
            if(n.depth>=MAX_DEPTH||!shouldTraverse(n.e,n.trail))continue;
            scans++; List<Entry>kids=list(c,u); if(kids==null)continue; int count=0;
            for(Entry k:kids){ if(!k.dir())continue; if(++count>MAX_CHILDREN)break; ArrayList<String>t=new ArrayList<>(n.trail);t.add(k.name);q.add(new Node(k,t,n.depth+1)); }
        }
        return null;
    }

    private static boolean isTrendingMovies(Entry e,List<String>trail){
        Uri u; try{u=Uri.parse(raw(e.uri));}catch(Throwable t){return false;}
        String mode=q(u,"mode"), label=norm(e.name), path=norm(join(trail));
        boolean movie="build_movie_list".equals(mode)||path.contains("movie")||"movie".equalsIgnoreCase(e.mediaType)||norm(q(u,"media_type")).contains("movie");
        String route=norm(mode+" "+q(u,"action")+" "+q(u,"type")+" "+q(u,"list_type"));
        boolean trending=label.equals("trending")||label.startsWith("trending ")||label.endsWith(" trending")||label.contains(" trending ")||route.contains("trending");
        return movie&&trending;
    }

    private static boolean shouldTraverse(Entry e,List<String>trail){
        Uri u;try{u=Uri.parse(raw(e.uri));}catch(Throwable t){return false;}
        String mode=q(u,"mode"), action=q(u,"action"), label=norm(e.name);
        if(label.equals("tools")||label.equals("search")||label.equals("people")||label.equals("downloads")||label.equals("my services")||label.equals("random lists"))return false;
        if(label.equals("discover"))return trail.size()<=4;
        if("navigator.main".equals(mode))return action.equals("RootList")||action.equals("MovieList")||trail.size()<=2;
        return mode.equals("navigator.discover")||mode.equals("navigator.movies");
    }

    private static long ensureChannel(Context c,String pluginUri){
        long id=findChannel(c);
        ContentValues v=new ContentValues();
        v.put("type","TYPE_PREVIEW"); v.put("display_name",TITLE); v.put("internal_provider_id",KEY);
        v.put("app_link_intent_uri",homeIntent(c).toUri(0));
        try{
            if(id>0){c.getContentResolver().update(ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI,id),v,null,null);return id;}
            Uri inserted=c.getContentResolver().insert(TvContract.Channels.CONTENT_URI,v); if(inserted==null)return -1;
            id=ContentUris.parseId(inserted); try{TvContract.requestChannelBrowsable(c,id);}catch(Throwable ignored){} return id;
        }catch(Throwable t){Log.e(TAG,"CLEAN06 channel provider error",t);return -1;}
    }

    private static long findChannel(Context c){
        try(Cursor cur=c.getContentResolver().query(TvContract.Channels.CONTENT_URI,new String[]{"_id","internal_provider_id"},null,null,null)){
            if(cur==null)return -1; while(cur.moveToNext())if(KEY.equals(cur.getString(1)))return cur.getLong(0);
        }catch(Throwable ignored){} return -1;
    }

    private static int syncPrograms(Context c,long channel,String pluginUri){
        List<Entry>entries=list(c,pluginUri); if(entries==null)return 0;
        try{c.getContentResolver().delete(TvContract.PreviewPrograms.CONTENT_URI,"channel_id=?",new String[]{String.valueOf(channel)});}catch(Throwable ignored){}
        int n=0;
        for(Entry e:entries){
            if(n>=MAX_PROGRAMS)break;
            if(e.dir()&&!raw(e.uri).contains("tmdb_id="))continue;
            ContentValues v=new ContentValues(); v.put("channel_id",channel); v.put("title",e.name); v.put("description",e.mediaType!=null?e.mediaType:"Red Light");
            v.put("type",programType(e)); v.put("intent_uri",homeIntent(c).toUri(0)); v.put("internal_provider_id",raw(e.uri)); v.put("weight",MAX_PROGRAMS-n); v.put("poster_art_aspect_ratio",5);
            try{if(c.getContentResolver().insert(TvContract.PreviewPrograms.CONTENT_URI,v)!=null)n++;}catch(Throwable ignored){}
        }
        return n;
    }

    private static Intent homeIntent(Context c){
        Intent i=new Intent(Intent.ACTION_MAIN); i.setComponent(new ComponentName(c.getPackageName(),"com.spocky.projengmenu.ui.home.MainActivity")); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP); return i;
    }

    private static int programType(Entry e){String m=e.mediaType==null?"":e.mediaType.toLowerCase(Locale.ROOT);if("movie".equals(m))return 0;if("tvshow".equals(m))return 1;if("episode".equals(m))return 2;return 5;}

    private static List<Entry> list(Context c,String kodiUri){
        Uri u=new Uri.Builder().scheme("content").authority(AUTH).fragment(kodiUri).build();
        try(Cursor cur=c.getContentResolver().query(u,null,null,null,null)){
            if(cur==null)return null; int name=cur.getColumnIndex("name"),cat=cur.getColumnIndex("category"),uri=cur.getColumnIndex("uri"),mt=cur.getColumnIndex("mediatype"); ArrayList<Entry>out=new ArrayList<>();
            while(cur.moveToNext()){String x=uri>=0&&!cur.isNull(uri)?cur.getString(uri):null;if(x==null||x.trim().isEmpty())continue;Entry e=new Entry();e.uri=raw(x);e.name=name>=0&&!cur.isNull(name)?cur.getString(name):e.uri;e.category=cat>=0&&!cur.isNull(cat)?cur.getString(cat):null;e.mediaType=mt>=0&&!cur.isNull(mt)?cur.getString(mt):null;out.add(e);}return out;
        }catch(Throwable t){Log.i(TAG,"CLEAN06 Kodi provider query unavailable: "+kodiUri);return null;}
    }

    private static String raw(String value){if(value==null)return "";try{Uri u=Uri.parse(value);if("content".equalsIgnoreCase(u.getScheme())&&AUTH.equals(u.getAuthority())&&u.getFragment()!=null)return u.getFragment();}catch(Throwable ignored){}return value;}
    private static String q(Uri u,String k){String v=u==null?null:u.getQueryParameter(k);return v==null?"":v;}
    private static String norm(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replaceAll("\\[[^]]*]","").replace('&',' ').replaceAll("[^a-z0-9]+"," ").trim();}
    private static List<String> single(String s){ArrayList<String>a=new ArrayList<>();a.add(s==null?"":s);return a;}
    private static String join(List<String>x){StringBuilder b=new StringBuilder();for(String s:x){if(b.length()>0)b.append('/');b.append(s);}return b.toString();}
}