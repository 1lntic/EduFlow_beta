package pl.eduflow.client;
import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.*;

public class JournalActivity extends Activity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final ZoneId zone=ZoneId.of("Europe/Warsaw");
    private SharedPreferences prefs;
    private JournalStore store;
    private JournalApi api;
    private WebView web;
    private LinearLayout root,content,bar,top;
    private FrameLayout body;
    private ScrollView scroll;
    private ProgressBar progress;
    private TextView subtitle;
    private JSONArray profiles=new JSONArray();
    private JSONObject profile;
    private LocalDate day=LocalDate.now(zone);
    private int tab=0,bg,ink,muted,surface,accent,epoch=0;
    private boolean busy=false,pending=false,pageFailed=false;
    private String css,injector;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=getSharedPreferences("eduflow",MODE_PRIVATE);store=new JournalStore(this);
        try{css=asset("mobile.css");injector=asset("inject.js");}catch(Exception e){finish();return;}
        if(state!=null)try{day=LocalDate.parse(state.getString("day",day.toString()));}catch(Exception ignored){}
        colors();root=new LinearLayout(this);root.setOrientation(1);setContentView(root);
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(1792);
        root.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets a=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(a.left,a.top,a.right,a.bottom);}else v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});root.requestApplyInsets();
        top=new LinearLayout(this);top.setPadding(dp(16),dp(10),dp(10),dp(10));top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout title=new LinearLayout(this);title.setOrientation(1);title.addView(text("EduFlow",24,ink));subtitle=text("Sync beta",11,muted);title.addView(subtitle);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        top.addView(button("↻",this::sync),new LinearLayout.LayoutParams(dp(60),dp(48)));root.addView(top);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setVisibility(View.GONE);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(2)));
        body=new FrameLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(16),dp(16),dp(16),dp(24));scroll.addView(content);body.addView(scroll);
        web=new WebView(this);body.addView(web,new FrameLayout.LayoutParams(-1,-1));web.setVisibility(View.GONE);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setSafeBrowsingEnabled(true);s.setUseWideViewPort(true);s.setLoadWithOverviewMode(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,prefs.getBoolean("thirdPartyCookies",false));WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        try{api=new JournalApi(this,web,asset("journal-api.js"));}catch(Exception e){finish();return;}
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){if(!r.isForMainFrame()||trusted(r.getUrl()))return false;external(r.getUrl());return true;}
            @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap icon){api.cancel();busy=false;pageFailed=false;if(!trusted(Uri.parse(url))&&!url.equals("about:blank")){v.stopLoading();pageFailed=true;pending=false;progress.setVisibility(View.GONE);external(Uri.parse(url));return;}progress.setVisibility(View.VISIBLE);subtitle.setText(Uri.parse(url).getHost());}
            @Override public void onPageFinished(WebView v,String url){progress.setVisibility(View.GONE);if(pageFailed)return;inject();String base=JournalApi.portalBase(url);if(base!=null)prefs.edit().putString("journalPortal",base).apply();if(pending){pending=false;if(base!=null)context();else toast("Zaloguj się, otwórz dziennik ucznia i naciśnij ↻.");}}
            @Override public void onReceivedSslError(WebView v,SslErrorHandler h,SslError e){h.cancel();api.cancel();busy=false;pending=false;pageFailed=true;progress.setVisibility(View.GONE);toast("Błąd certyfikatu HTTPS — połączenie zablokowane.");}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()){pageFailed=true;pending=false;progress.setVisibility(View.GONE);toast("Nie udało się otworzyć strony. Zapis offline pozostaje dostępny.");}}
            @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse e){if(r.isForMainFrame()){pageFailed=true;pending=false;progress.setVisibility(View.GONE);toast("Strona zwróciła HTTP "+e.getStatusCode());}}
            @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail d){api.cancel();body.removeView(web);web.destroy();web=null;recreate();return true;}
        });
        bar=new LinearLayout(this);bar.setPadding(dp(4),dp(4),dp(4),dp(4));root.addView(bar);String[] names={"Start","Oceny","Plan","WWW","Więcej"};
        for(int i=0;i<5;i++){final int n=i;LinearLayout item=new LinearLayout(this);item.setOrientation(1);item.setGravity(Gravity.CENTER);item.addView(new IconView(this,i,accent),new LinearLayout.LayoutParams(dp(23),dp(23)));item.addView(text(names[i],10,muted));bar.addView(item,new LinearLayout.LayoutParams(0,dp(60),1));item.setContentDescription(names[i]);item.setOnClickListener(v->{if(n==4)more();else if(n==3)openWeb();else{tab=n;render();}});}
        render();load();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(accent);b.setOnClickListener(v->action.run());return b;}
    private void colors(){String t=prefs.getString("theme","dark");boolean d=t.equals("dark")||(t.equals("system")&&(getResources().getConfiguration().uiMode&48)==32);bg=Color.parseColor(d?"#0E1420":"#F3F6FC");surface=Color.parseColor(d?"#182131":"#FFFFFF");ink=Color.parseColor(d?"#EDF3FF":"#17233A");muted=Color.parseColor(d?"#A9B8D0":"#4D607F");accent=Color.parseColor(d?"#86ADFF":"#285EC7");}
    private void shell(){colors();root.setBackgroundColor(bg);top.setBackgroundColor(bg);bar.setBackgroundColor(surface);web.setBackgroundColor(bg);subtitle.setTextColor(muted);LinearLayout heading=(LinearLayout)top.getChildAt(0);((TextView)heading.getChildAt(0)).setTextColor(ink);((Button)top.getChildAt(1)).setTextColor(accent);for(int i=0;i<bar.getChildCount();i++){LinearLayout item=(LinearLayout)bar.getChildAt(i);((IconView)item.getChildAt(0)).tint(i==tab?accent:muted);((TextView)item.getChildAt(1)).setTextColor(i==tab?accent:muted);}getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(surface);if(Build.VERSION.SDK_INT>=30){WindowInsetsController c=getWindow().getInsetsController();if(c!=null)c.setSystemBarsAppearance(bg==Color.parseColor("#0E1420")?0:24,24);}}
    private LinearLayout card(String title,String details){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(16),dp(15),dp(16),dp(15));GradientDrawable g=new GradientDrawable();g.setColor(surface);g.setCornerRadius(dp(18));c.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);content.addView(c,p);TextView h=text(title,19,ink);h.setTypeface(null,Typeface.BOLD);c.addView(h);TextView d=text(details,13,muted);d.setPadding(0,dp(8),0,0);c.addView(d);return c;}
    private String val(JSONObject o,String key){Object v=o.opt(key);return v==null||v==JSONObject.NULL?"":String.valueOf(v);}
    private String hour(String value){java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?:T|^)(\\d{2}:\\d{2})").matcher(value);return m.find()?m.group(1):value;}
    private String updated(){return Instant.ofEpochMilli(profile.optLong("fetchedAt")).atZone(zone).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));}
    private void render(){if(isDestroyed()||web==null)return;shell();web.setVisibility(View.GONE);scroll.setVisibility(View.VISIBLE);content.removeAllViews();subtitle.setText("Dane lokalne • Sync beta");if(!busy)progress.setVisibility(View.GONE);
        if(profile==null){LinearLayout c=card("Twój dziennik, po Twojemu","Zaloguj się w WWW, otwórz dziennik ucznia i naciśnij ↻. Integracja testowa: plan jednego dnia i oceny z tablicy, niekoniecznie cały semestr.");c.addView(button("Połącz eduVULCAN",this::openWeb));return;}
        JSONArray grades=profile.optJSONArray("grades"),lessons=profile.optJSONArray("lessons");
        if(tab==0){card("Cześć, "+val(profile,"name"),val(profile,"className")+" · "+val(profile,"school"));LinearLayout c=card("Ostatni zapis: "+updated(),(grades==null?0:grades.length())+" ocen z tablicy · "+(lessons==null?0:lessons.length())+" zajęć\nPlan: "+val(profile,"date")+"\nDane dostępne offline. Brak synchronizacji w tle.");c.addView(button("Synchronizuj",this::sync));c.addView(button("Zmień profil",this::choose));}
        else if(tab==1){card("Oceny z tablicy","Zapis: "+updated()+". Endpoint może nie zawierać wszystkich ocen semestru.");if(grades==null||grades.length()==0)card("Brak ocen na tablicy","Ostatnia udana odpowiedź zwróciła pustą listę.");else for(int i=0;i<grades.length();i++){JSONObject g=grades.optJSONObject(i);if(g!=null)card(val(g,"grade")+" · "+val(g,"subject"),val(g,"category")+" · "+val(g,"date")+(g.isNull("weight")?"":" · waga "+val(g,"weight"))+"\n"+val(g,"comment"));}}
        else{LinearLayout c=card("Plan: "+val(profile,"date"),"Zapis: "+updated()+". Wybór daty ustawia kolejne pobranie — następnie naciśnij ↻.");c.addView(button("Dzień do pobrania: "+day,this::pickDay));if(lessons==null||lessons.length()==0)card("Brak zajęć w pobranym dniu","Serwer zwrócił pustą listę.");else for(int i=0;i<lessons.length();i++){JSONObject l=lessons.optJSONObject(i);if(l!=null)card(val(l,"subject"),hour(val(l,"startsAt"))+" – "+hour(val(l,"endsAt"))+"\n"+val(l,"teacher")+" · sala "+val(l,"room"));}}
    }
    private void load(){final int e=epoch;io.execute(()->{try{JSONObject data=store.load();runOnUiThread(()->{if(e!=epoch||isDestroyed())return;profiles=data.optJSONArray("profiles");if(profiles==null)profiles=new JSONArray();profile=null;String id=prefs.getString("journalSelected","");for(int i=0;i<profiles.length();i++){JSONObject p=profiles.optJSONObject(i);if(p!=null&&p.optString("id").equals(id))profile=p;}if(profile==null&&profiles.length()>0)profile=profiles.optJSONObject(0);if(tab!=3)render();});}catch(Exception ex){runOnUiThread(()->{if(!isDestroyed())error("CACHE_READ");});}});}
    private void choose(){if(profiles.length()==0){toast("Brak zapisanych profili.");return;}String[] names=new String[profiles.length()];for(int i=0;i<names.length;i++){JSONObject p=profiles.optJSONObject(i);names[i]=p==null?"Profil":val(p,"name")+" · "+val(p,"className")+" · "+val(p,"school");}new AlertDialog.Builder(this).setTitle("Zapisane profile").setItems(names,(d,n)->{profile=profiles.optJSONObject(n);if(profile!=null)prefs.edit().putString("journalSelected",profile.optString("id")).apply();tab=0;render();}).show();}
    private void pickDay(){new DatePickerDialog(this,(v,y,m,d)->{day=LocalDate.of(y,m+1,d);if(tab!=3)render();},day.getYear(),day.getMonthValue()-1,day.getDayOfMonth()).show();}
    private boolean trusted(Uri u){if(!"https".equalsIgnoreCase(u.getScheme())||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))return false;String h=u.getHost();if(h==null)return false;h=h.toLowerCase(Locale.ROOT);for(String d:new String[]{"eduvulcan.pl","vulcan.net.pl","vulcan.edu.pl"})if(h.equals(d)||h.endsWith("."+d))return true;return false;}
    private void showWeb(){tab=3;shell();scroll.setVisibility(View.GONE);web.setVisibility(View.VISIBLE);subtitle.setText("Logowanie na prawdziwej stronie VULCAN");}
    private void openWeb(){showWeb();if(web.getUrl()==null||web.getUrl().equals("about:blank"))web.loadUrl("https://eduvulcan.pl/logowanie");}
    private void external(Uri u){if(!"https".equalsIgnoreCase(u.getScheme())&&!"mailto".equalsIgnoreCase(u.getScheme())&&!"tel".equalsIgnoreCase(u.getScheme())){toast("Nieobsługiwany odnośnik.");return;}new AlertDialog.Builder(this).setTitle("Otworzyć poza EduFlow?").setMessage(u.toString()).setNegativeButton("Anuluj",null).setPositiveButton("Otwórz",(d,n)->{try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception e){toast("Brak aplikacji do tego odnośnika.");}}).show();}
    private void inject(){if(web==null||!trusted(Uri.parse(web.getUrl()==null?"":web.getUrl())))return;try{JSONObject c=new JSONObject().put("enabled",prefs.getBoolean("enabled",true)).put("theme",bg==Color.parseColor("#0E1420")?"dark":"light").put("compact",prefs.getBoolean("compact",false)).put("css",css).put("customCss",prefs.getString("customCss",""));web.evaluateJavascript(injector.replace("__CONFIG__",c.toString()),null);}catch(Exception ignored){}}
    private void sync(){if(busy||pending){toast("Synchronizacja już trwa.");return;}showWeb();if(JournalApi.portalBase(web.getUrl())!=null&&!pageFailed){context();return;}String base=prefs.getString("journalPortal","");if(JournalApi.portalBase(base+"/")!=null){pending=true;web.loadUrl(base+"/");}else{if(web.getUrl()==null||web.getUrl().equals("about:blank"))web.loadUrl("https://eduvulcan.pl/logowanie");toast("Zaloguj się, otwórz dziennik ucznia i naciśnij ↻.");}}
    private void context(){busy=true;progress.setVisibility(View.VISIBLE);try{api.request(new JSONObject().put("operation","context"),new JournalApi.Callback(){public void failure(String code){error(code);}public void success(JSONObject data){progress.setVisibility(View.GONE);JSONArray students=data.optJSONArray("students");if(students==null||students.length()==0||students.length()>32){error("SCHEMA_CONTEXT");return;}String[] names=new String[students.length()];for(int i=0;i<names.length;i++){JSONObject s=students.optJSONObject(i);if(s==null){error("SCHEMA_CONTEXT");return;}names[i]=val(s,"name")+" · "+val(s,"className")+" · "+val(s,"school");}new AlertDialog.Builder(JournalActivity.this).setTitle("Synchronizuj profil").setItems(names,(d,n)->fetch(students.optJSONObject(n))).setNegativeButton("Anuluj",(d,n)->busy=false).setOnCancelListener(d->busy=false).show();}});}catch(Exception e){error("CONFIG_ERROR");}}
    private void fetch(JSONObject student){final String base=JournalApi.portalBase(web.getUrl());if(base==null||student==null){error("WRONG_PORTAL");return;}final LocalDate date=day;final int e=epoch;progress.setVisibility(View.VISIBLE);try{JSONObject c=new JSONObject().put("operation","student").put("studentKey",student.getString("key")).put("journalId",student.getString("journalId")).put("date",date.toString()).put("from",date.atStartOfDay(zone).toInstant().toString()).put("to",date.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1).toString());api.request(c,new JournalApi.Callback(){public void failure(String code){error(code);}public void success(JSONObject data){if(!(data.opt("grades") instanceof JSONArray)||!(data.opt("lessons") instanceof JSONArray)||!date.toString().equals(data.optString("date"))){error("SCHEMA_RESULT");return;}io.execute(()->{try{byte[] hash=MessageDigest.getInstance("SHA-256").digest((base+"|"+student.getString("key")+"|"+student.getString("journalId")).getBytes(StandardCharsets.UTF_8));StringBuilder id=new StringBuilder();for(byte b:hash)id.append(String.format(Locale.ROOT,"%02x",b&255));data.put("id",id.toString());store.save(data);runOnUiThread(()->{if(e!=epoch||isDestroyed())return;busy=false;progress.setVisibility(View.GONE);prefs.edit().putString("journalSelected",id.toString()).apply();tab=0;load();toast("Zapisano plan dnia i oceny z tablicy.");});}catch(Exception ex){runOnUiThread(()->{if(e==epoch&&!isDestroyed())error("CACHE_WRITE");});}});}});}catch(Exception ex){error("CONFIG_ERROR");}}
    private void error(String code){busy=false;pending=false;progress.setVisibility(View.GONE);String hint=code.startsWith("SCHEMA_")?"API zwróciło inną strukturę. Adapter wymaga dopasowania. Nazwy pól w kodzie nie są ich wartościami.":code.startsWith("CACHE_")?"Nie można odczytać lub zapisać pliku offline. Zapis nie został automatycznie usunięty; możesz go wyczyścić w Więcej.":"Sprawdź internet, sesję i dostęp do dziennika ucznia. Nie omijamy ograniczeń usługi.";new AlertDialog.Builder(this).setTitle("Synchronizacja nie powiodła się").setMessage(hint+"\n\nKod: "+code+"\n\nBłąd pobierania nie zastępuje poprzednich danych pustą listą.").setPositiveButton("OK",null).show();}
    private void more(){new AlertDialog.Builder(this).setTitle("EduFlow Sync beta").setItems(new String[]{"Synchronizuj","Wybierz dzień planu","Zmień profil","Motyw","Tryb klasyczny / CSS","Cookies między domenami","Usuń zapis offline","Wyczyść zapis i sesję","Informacje"},(d,n)->{switch(n){case 0:sync();break;case 1:pickDay();break;case 2:choose();break;case 3:new AlertDialog.Builder(this).setTitle("Motyw").setItems(new String[]{"Ciemny","Jasny","Systemowy"},(a,i)->{prefs.edit().putString("theme",new String[]{"dark","light","system"}[i]).apply();if(tab==3)shell();else render();inject();}).show();break;case 4:startActivity(new Intent(this,MainActivity.class));break;case 5:boolean enabled=!prefs.getBoolean("thirdPartyCookies",false);new AlertDialog.Builder(this).setTitle("Cookies między domenami").setMessage("Włączaj tylko, jeśli wymaga tego logowanie VULCAN. "+(enabled?"Włączyć?":"Wyłączyć?")).setNegativeButton("Anuluj",null).setPositiveButton("Zastosuj",(a,i)->{prefs.edit().putBoolean("thirdPartyCookies",enabled).apply();CookieManager.getInstance().setAcceptThirdPartyCookies(web,enabled);}).show();break;case 6:clear(false);break;case 7:clear(true);break;case 8:new AlertDialog.Builder(this).setTitle("EduFlow 0.2 • testowa integracja").setMessage("Nieoficjalny klient bez powiązania z VULCAN.\nPlan jednego dnia i oceny z tablicy, niekoniecznie cały semestr. Brak wiadomości, frekwencji, push i synchronizacji w tle.\n\nZapis offline AES-GCM, klucz Android Keystore. Do 16 profili, ostatni dzień na profil. Bez własnego serwera i odczytu hasła/cookies przez kod natywny.\n\nZgodność API na Twoim koncie wymaga testu. Sprawdź regulamin. Nie odblokowuje funkcji płatnych. Debug WebView jest dostępny w buildzie debug.").setPositiveButton("OK",null).show();break;}}).show();}
    private void clear(boolean session){new AlertDialog.Builder(this).setTitle(session?"Usunąć zapis i sesję?":"Usunąć zapis offline?").setMessage("Usunie lokalne oceny i plan."+(session?" Usunie też cookies i dane witryn, nie konto VULCAN.":" Sesja pozostanie.")) .setNegativeButton("Anuluj",null).setPositiveButton("Usuń",(d,n)->{epoch++;api.cancel();busy=false;pending=false;io.execute(()->{store.clear();runOnUiThread(()->{if(isDestroyed())return;profiles=new JSONArray();profile=null;prefs.edit().remove("journalSelected").apply();tab=0;render();if(session){prefs.edit().remove("journalPortal").apply();web.stopLoading();web.loadUrl("about:blank");CookieManager.getInstance().removeAllCookies(ok->{if(isDestroyed())return;CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();web.clearCache(true);web.clearHistory();});}});});}).show();}
    private String asset(String name)throws IOException{try(InputStream in=getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>2097152)throw new IOException();}return out.toString("UTF-8");}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onBackPressed(){if(tab==3){if(web.canGoBack())web.goBack();else{tab=0;render();}}else super.onBackPressed();}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);if(tab==3)shell();else render();inject();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("day",day.toString());}
    @Override protected void onPause(){if(web!=null)web.onPause();CookieManager.getInstance().flush();super.onPause();}
    @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();}
    @Override protected void onDestroy(){epoch++;if(api!=null&&web!=null)api.cancel();io.shutdown();if(web!=null){body.removeView(web);web.stopLoading();web.destroy();web=null;}super.onDestroy();}
}
