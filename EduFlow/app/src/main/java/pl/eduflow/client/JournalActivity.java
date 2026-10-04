package pl.eduflow.client;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.Locale;
import java.util.concurrent.*;

public class JournalActivity extends Activity {
    private static final String UI="https://ui.eduflow.invalid/",PUPIL="uczen.eduvulcan.pl",MAIL="wiadomosci.eduvulcan.pl";
    private final Handler clock=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final AutoSyncPolicy policy=new AutoSyncPolicy();
    private final ZoneId zone=ZoneId.of("Europe/Warsaw");
    private SharedPreferences prefs;private JournalStore store;
    private WebView ui,pupil,mail;private SessionJob pupilJob,mailJob;
    private FrameLayout body;private LinearLayout root,loginBar;
    private String pupilScript,mailScript,status="",selectedId="",base="";
    private JSONObject profile,detail;private JSONArray profiles=new JSONArray(),students=new JSONArray(),messages=new JSONArray();
    private LocalDate day=LocalDate.now(zone);
    private boolean active=false,uiReady=false,loaded=false,connected=false,loginVisible=false,pupilReady=false,mailReady=false,wantInbox=false,choosing=false,error=false;
    private int epoch=0;private long messagesAt=0;
    private final Runnable tick=new Runnable(){public void run(){if(!active)return;if(loaded&&!loginVisible&&!choosing&&policy.due(SystemClock.elapsedRealtime()))refresh(false);clock.postDelayed(this,5000);}};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=getSharedPreferences("eduflow",MODE_PRIVATE);store=new JournalStore(this);
        try{pupilScript=asset("journal-api.js");mailScript=asset("messages-api.js");}catch(Exception e){finish();return;}
        if(state!=null)try{day=LocalDate.parse(state.getString("day",day.toString()));}catch(Exception ignored){}
        root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.parseColor("#15151e"));setContentView(root);
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);else getWindow().getDecorView().setSystemUiVisibility(1792);
        root.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets a=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(a.left,a.top,a.right,a.bottom);}else v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});root.requestApplyInsets();
        loginBar=new LinearLayout(this);Button back=new Button(this);back.setText("Wróć do EduFlow");back.setAllCaps(false);back.setOnClickListener(v->hideLogin());loginBar.addView(back);loginBar.setVisibility(View.GONE);root.addView(loginBar);
        body=new FrameLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        pupil=session(false);mail=session(true);body.addView(pupil,new FrameLayout.LayoutParams(1,1));body.addView(mail,new FrameLayout.LayoutParams(1,1));
        pupilJob=new SessionJob(this,pupil,PUPIL);mailJob=new SessionJob(this,mail,MAIL);
        ui=new WebView(this);WebSettings us=ui.getSettings();us.setJavaScriptEnabled(true);us.setAllowFileAccess(false);us.setAllowContentAccess(false);us.setBlockNetworkLoads(true);us.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        ui.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){if(r.isForMainFrame()&&UI.equals(v.getUrl())&&"eduflow".equals(r.getUrl().getScheme()))command(r.getUrl());return true;}
            @Override public void onPageFinished(WebView v,String url){if(UI.equals(url)){uiReady=true;render();}}
            @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail d){pupilJob.cancel();mailJob.cancel();body.removeView(v);v.destroy();ui=null;recreate();return true;}
        });body.addView(ui,new FrameLayout.LayoutParams(-1,-1));
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        try{ui.loadDataWithBaseURL(UI,asset("journal-ui.html"),"text/html","UTF-8",null);}catch(Exception e){finish();return;}
        policy.enable(prefs.getBoolean("journalAuto",true),SystemClock.elapsedRealtime());load();
    }
    private WebView session(boolean inbox){
        WebView w=new WebView(this);w.setAlpha(0);w.setFocusable(false);w.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setSafeBrowsingEnabled(true);s.setUseWideViewPort(true);s.setLoadWithOverviewMode(true);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(w,prefs.getBoolean("thirdPartyCookies",false));
        w.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){if(!r.isForMainFrame())return false;return !trusted(r.getUrl());}
            @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap b){
                if(inbox){if(mailJob!=null)mailJob.cancel();mailReady=false;}else{if(pupilJob!=null)pupilJob.cancel();pupilReady=false;connected=false;if(!loginVisible&&policy.running())policy.failure(SystemClock.elapsedRealtime(),false);}
                if(!trusted(Uri.parse(url))&&!url.equals("about:blank")){v.stopLoading();failure("WRONG_PORTAL",inbox);}
            }
            @Override public void onPageFinished(WebView v,String url){
                if(url.equals("about:blank")||!trusted(Uri.parse(url)))return;
                String b=SessionJob.base(url,inbox?MAIL:PUPIL);
                if(inbox){mailReady=b!=null&&b.equals(mailBase());if(wantInbox){if(mailReady)fetchInbox();else failure("MAIL_LOGIN_REQUIRED",true);}}
                else{pupilReady=b!=null;if(b!=null){base=b;prefs.edit().putString("journalPortal",b).apply();if(active&&loginVisible){policy.authenticated(SystemClock.elapsedRealtime());refresh(true);}}else if(!loginVisible){failure("LOGIN_REQUIRED",false);}}
            }
            @Override public void onReceivedSslError(WebView v,SslErrorHandler h,SslError e){h.cancel();failure("SSL_ERROR",inbox);}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())failure("NETWORK_ERROR",inbox);}
            @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse e){if(r.isForMainFrame())failure("HTTP_"+e.getStatusCode(),inbox);}
            @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail d){if(pupilJob!=null)pupilJob.cancel();if(mailJob!=null)mailJob.cancel();body.removeView(v);v.destroy();if(inbox)mail=null;else pupil=null;recreate();return true;}
        });return w;
    }
    private boolean trusted(Uri u){if(!"https".equalsIgnoreCase(u.getScheme())||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))return false;String h=u.getHost();if(h==null)return false;h=h.toLowerCase(Locale.ROOT);for(String d:new String[]{"eduvulcan.pl","vulcan.net.pl","vulcan.edu.pl"})if(h.equals(d)||h.endsWith("."+d))return true;return false;}
    private String asset(String name)throws IOException{try(InputStream in=getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>2097152)throw new IOException();}return out.toString("UTF-8");}}
    private void load(){final int e=epoch;io.execute(()->{try{JSONObject data=store.load();runOnUiThread(()->{if(e!=epoch||isDestroyed())return;profiles=data.optJSONArray("profiles");if(profiles==null)profiles=new JSONArray();selectedId=prefs.getString("journalSelected","");profile=cached(selectedId);loaded=true;String b=prefs.getString("journalPortal","");if(SessionJob.base(b+"/",PUPIL)!=null){base=b;pupil.loadUrl(b+"/");}render();});}catch(Exception ex){runOnUiThread(()->{if(!isDestroyed()){loaded=true;status="Nie można odczytać zapisu offline. Możesz go wyczyścić w ustawieniach.";error=true;render();}});}});}
    private JSONObject cached(String id){for(int i=0;i<profiles.length();i++){JSONObject p=profiles.optJSONObject(i);if(p!=null&&p.optString("id").equals(id))return p;}return null;}
    private String id(JSONObject s)throws Exception{byte[] bytes=MessageDigest.getInstance("SHA-256").digest((base+"|"+s.getString("key")+"|"+s.getString("journalId")).getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:bytes)b.append(String.format(Locale.ROOT,"%02x",x&255));return b.toString();}
    private void render(){if(!uiReady||ui==null||isDestroyed())return;try{JSONObject s=new JSONObject().put("profile",profile==null?JSONObject.NULL:profile).put("messages",messages).put("messagesFetchedAt",messagesAt).put("message",detail==null?JSONObject.NULL:detail).put("auto",prefs.getBoolean("journalAuto",true)).put("theme",prefs.getString("theme","dark")).put("busy",policy.running()||(mailJob!=null&&mailJob.running())).put("status",status).put("error",error).put("needsLogin",!connected).put("targetDate",day.toString());ui.evaluateJavascript("window.EduFlow&&window.EduFlow.render("+s.toString()+")",null);}catch(Exception ignored){}}
    private void command(Uri u){if(!active||u.getUserInfo()!=null||u.getPort()!=-1)return;String a=u.getHost();if(a==null)return;
        switch(a){
            case "sync":refresh(true);break;
            case "login":login();break;
            case "profile":if(policy.running()){toast("Poczekaj na zakończenie synchronizacji.");break;}selectedId="";refresh(true);break;
            case "date":try{day=LocalDate.parse(u.getQueryParameter("date"));refresh(true);}catch(Exception e){toast("Nieprawidłowa data.");}break;
            case "auto":boolean enabled="1".equals(u.getQueryParameter("enabled"));prefs.edit().putBoolean("journalAuto",enabled).apply();policy.enable(enabled,SystemClock.elapsedRealtime());render();break;
            case "theme":prefs.edit().putString("theme",prefs.getString("theme","dark").equals("light")?"dark":"light").apply();render();break;
            case "inbox":inbox();break;
            case "read":read(u.getQueryParameter("id"));break;
            case "logout":new AlertDialog.Builder(this).setTitle("Usunąć zapis i sesję?").setMessage("Usunie lokalne oceny, plan, pamięć skrzynki i cookies. Nie usuwa konta VULCAN.").setNegativeButton("Anuluj",null).setPositiveButton("Usuń",(d,n)->clear()).show();break;
        }
    }
    private void login(){epoch++;pupilJob.cancel();mailJob.cancel();policy.pause();policy.resume(SystemClock.elapsedRealtime());policy.authenticated(SystemClock.elapsedRealtime());connected=false;messages=new JSONArray();messagesAt=0;detail=null;mailReady=false;wantInbox=false;loginVisible=true;status="Zaloguj się i wybierz dziennik ucznia.";error=false;loginBar.setVisibility(View.VISIBLE);ui.setVisibility(View.GONE);pupil.setAlpha(1);pupil.setFocusableInTouchMode(true);pupil.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);pupil.setLayoutParams(new FrameLayout.LayoutParams(-1,-1));pupil.bringToFront();pupil.loadUrl("https://eduvulcan.pl/logowanie");render();}
    private void hideLogin(){loginVisible=false;loginBar.setVisibility(View.GONE);pupil.setAlpha(0);pupil.setFocusable(false);pupil.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);pupil.setLayoutParams(new FrameLayout.LayoutParams(1,1));ui.setVisibility(View.VISIBLE);ui.bringToFront();render();}
    private void refresh(boolean manual){
        if(!active||choosing||!loaded||!policy.begin(SystemClock.elapsedRealtime(),manual))return;
        if(!pupilReady||SessionJob.base(pupil.getUrl(),PUPIL)==null){failure("LOGIN_REQUIRED",false);return;}
        status="Sprawdzam sesję i profil…";error=false;render();final int e=epoch;
        try{pupilJob.request(pupilScript,new JSONObject().put("operation","context"),new SessionJob.Callback(){public void failure(String c){JournalActivity.this.failure(c,false);}public void success(JSONObject data){
            if(e!=epoch||!active)return;students=data.optJSONArray("students");if(students==null||students.length()==0||students.length()>32){JournalActivity.this.failure("SCHEMA_CONTEXT",false);return;}
            JSONObject chosen=null;try{for(int i=0;i<students.length();i++){JSONObject s=students.getJSONObject(i);if(id(s).equals(selectedId))chosen=s;}}catch(Exception ex){JournalActivity.this.failure("SCHEMA_CONTEXT",false);return;}
            if(chosen!=null){select(chosen,e);return;}if(students.length()==1){select(students.optJSONObject(0),e);return;}
            connected=false;profile=null;messages=new JSONArray();detail=null;render();choosing=true;String[] names=new String[students.length()];for(int i=0;i<names.length;i++){JSONObject s=students.optJSONObject(i);if(s==null){choosing=false;JournalActivity.this.failure("SCHEMA_CONTEXT",false);return;}names[i]=s.optString("name")+" · "+s.optString("className")+" · "+s.optString("school");}
            new AlertDialog.Builder(JournalActivity.this).setTitle("Wybierz profil EduFlow").setItems(names,(d,n)->{choosing=false;if(e==epoch&&active)select(students.optJSONObject(n),e);}).setNegativeButton("Anuluj",(d,n)->{choosing=false;JournalActivity.this.failure("PROFILE_REQUIRED",false);}).setOnCancelListener(d->{choosing=false;JournalActivity.this.failure("PROFILE_REQUIRED",false);}).show();
        }});}catch(Exception ex){failure("CONFIG_ERROR",false);}
    }
    private void select(JSONObject student,int e){if(student==null){failure("SCHEMA_CONTEXT",false);return;}try{
        String selected=id(student);if(!selected.equals(selectedId)){epoch++;e=epoch;mailJob.cancel();mailReady=false;messages=new JSONArray();messagesAt=0;detail=null;selectedId=selected;profile=cached(selected);}
        connected=true;hideLogin();status="Pobieram plan i oceny z tablicy…";final int token=e;final String key=selected;final LocalDate date=day;render();
        JSONObject config=new JSONObject().put("operation","student").put("studentKey",student.getString("key")).put("journalId",student.getString("journalId")).put("date",date.toString()).put("from",date.atStartOfDay(zone).toInstant().toString()).put("to",date.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1).toString());
        pupilJob.request(pupilScript,config,new SessionJob.Callback(){public void failure(String c){JournalActivity.this.failure(c,false);}public void success(JSONObject data){
            if(token!=epoch||!active)return;if(!(data.opt("grades") instanceof JSONArray)||!(data.opt("lessons") instanceof JSONArray)||!date.toString().equals(data.optString("date"))){JournalActivity.this.failure("SCHEMA_RESULT",false);return;}
            io.execute(()->{try{data.put("id",key);store.save(data);JSONObject all=store.load();runOnUiThread(()->{if(token!=epoch||!active||isDestroyed())return;profiles=all.optJSONArray("profiles");if(profiles==null)profiles=new JSONArray();profile=data;prefs.edit().putString("journalSelected",key).apply();policy.success(SystemClock.elapsedRealtime());status="Dane zaktualizowane. Następne odświeżanie podczas używania aplikacji.";error=false;render();if(wantInbox)inbox();});}catch(Exception ex){runOnUiThread(()->{if(token==epoch&&active)JournalActivity.this.failure("CACHE_WRITE",false);});}});
        }});
    }catch(Exception ex){failure("CONFIG_ERROR",false);}}
    private String mailBase(){return base.startsWith("https://"+PUPIL+"/")?base.replace("https://"+PUPIL+"/","https://"+MAIL+"/"):"";}
    private void inbox(){wantInbox=true;detail=null;if(!active)return;if(!connected){status="Połącz dziennik, aby odczytać skrzynkę tego konta.";error=true;render();return;}if(mailJob.running())return;if(mailReady&&mailBase().equals(SessionJob.base(mail.getUrl(),MAIL))){fetchInbox();return;}String b=mailBase();if(SessionJob.base(b+"/",MAIL)==null){failure("WRONG_PORTAL",true);return;}status="Łączę webową skrzynkę konta…";render();mail.loadUrl(b+"/");}
    private void fetchInbox(){if(!active||!connected||mailJob.running())return;final int e=epoch;status="Pobieram listę wiadomości…";try{mailJob.request(mailScript,new JSONObject().put("operation","inbox"),new SessionJob.Callback(){public void failure(String c){JournalActivity.this.failure(c,true);}public void success(JSONObject data){if(e!=epoch||!active)return;JSONArray list=data.optJSONArray("messages");if(list==null||list.length()>200){JournalActivity.this.failure("SCHEMA_INBOX",true);return;}messages=list;messagesAt=data.optLong("fetchedAt");status="Skrzynka konta: ostatnie 50 wiadomości. Lista jest w pamięci tej sesji.";error=false;render();}});render();}catch(Exception ex){failure("CONFIG_ERROR",true);}}
    private void read(String id){if(!active||!connected||!mailReady||mailJob.running())return;JSONObject found=null;for(int i=0;i<messages.length();i++){JSONObject m=messages.optJSONObject(i);if(m!=null&&m.optString("id").equals(id))found=m;}if(found==null||id==null){status="Wiadomość nie należy do aktualnie pobranej listy.";error=true;render();return;}final JSONObject row=found;final int e=epoch;
        try{mailJob.request(mailScript,new JSONObject().put("operation","read").put("id",id),new SessionJob.Callback(){public void failure(String c){JournalActivity.this.failure(c,true);}public void success(JSONObject data){if(e!=epoch||!active||!id.equals(data.optString("id")))return;try{detail=new JSONObject(row.toString()).put("body",android.text.Html.fromHtml(data.optString("body"),android.text.Html.FROM_HTML_MODE_LEGACY).toString());row.put("unread",false);status="Otwarto wiadomość. Serwis może zmienić status jej przeczytania.";error=false;render();ui.evaluateJavascript("window.EduFlow&&window.EduFlow.showMessage("+detail.toString()+")",null);}catch(Exception ex){JournalActivity.this.failure("SCHEMA_BODY",true);}}});render();}catch(Exception ex){failure("CONFIG_ERROR",true);}
    }
    private void failure(String code,boolean inbox){if(isDestroyed())return;boolean auth=code.equals("HTTP_401")||code.equals("HTTP_403")||code.equals("LOGIN_REQUIRED")||code.equals("MAIL_LOGIN_REQUIRED")||code.equals("NOT_JSON_LOGIN_OR_SCHEMA")||code.equals("LOGIN_OR_SCHEMA");if(inbox){mailJob.cancel();wantInbox=false;mailReady=false;if(auth){messages=new JSONArray();messagesAt=0;detail=null;}}else{pupilJob.cancel();policy.failure(SystemClock.elapsedRealtime(),auth);}if(auth){connected=false;policy.failure(SystemClock.elapsedRealtime(),true);}status="Nie udało się odświeżyć "+(inbox?"skrzynki":"dziennika")+". Kod: "+code+". "+(auth?"Zaloguj się ponownie.":"Zapis ocen i planu nie został usunięty.");error=true;render();}
    private void clear(){epoch++;pupilJob.cancel();mailJob.cancel();policy.pause();policy.resume(SystemClock.elapsedRealtime());policy.failure(SystemClock.elapsedRealtime(),true);connected=false;pupilReady=false;mailReady=false;wantInbox=false;messages=new JSONArray();messagesAt=0;detail=null;profile=null;profiles=new JSONArray();selectedId="";base="";prefs.edit().remove("journalSelected").remove("journalPortal").apply();hideLogin();pupil.stopLoading();mail.stopLoading();pupil.loadUrl("about:blank");mail.loadUrl("about:blank");io.execute(()->store.clear());CookieManager.getInstance().removeAllCookies(ok->{if(isDestroyed())return;CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();pupil.clearCache(true);pupil.clearHistory();mail.clearHistory();status="Usunięto lokalny zapis i sesję.";error=false;render();});}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onBackPressed(){if(loginVisible)hideLogin();else super.onBackPressed();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("day",day.toString());}
    @Override protected void onResume(){super.onResume();if(ui==null)return;active=true;ui.onResume();pupil.onResume();mail.onResume();messages=new JSONArray();messagesAt=0;detail=null;connected=false;mailReady=false;policy.resume(SystemClock.elapsedRealtime());clock.removeCallbacks(tick);clock.post(tick);render();}
    @Override protected void onPause(){active=false;epoch++;clock.removeCallbacks(tick);policy.pause();if(pupilJob!=null)pupilJob.cancel();if(mailJob!=null)mailJob.cancel();if(ui!=null){ui.onPause();if(pupil!=null)pupil.onPause();if(mail!=null)mail.onPause();}CookieManager.getInstance().flush();super.onPause();}
    @Override protected void onDestroy(){active=false;epoch++;clock.removeCallbacksAndMessages(null);if(pupilJob!=null)pupilJob.cancel();if(mailJob!=null)mailJob.cancel();io.shutdown();for(WebView w:new WebView[]{ui,pupil,mail})if(w!=null){body.removeView(w);w.stopLoading();w.destroy();}super.onDestroy();}
}
