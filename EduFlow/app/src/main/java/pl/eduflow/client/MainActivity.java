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
import android.text.InputType;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String DEFAULT_HOME="https://eduvulcan.pl/";
    private static final int IMPORT_CSS=42, PICK_FILE=43;
    private WebView web;
    private LinearLayout root, top, bottom;
    private TextView brand, host, reload;
    private ProgressBar progress;
    private SharedPreferences prefs;
    private String css, injector, navigator;
    private boolean demo=false, pageFailed=false;
    private ValueCallback<Uri[]> fileCallback;
    private int bg, surface, ink, muted, accent, border;
    private int active=0;
    private final String[] keys={"home","grades","timetable","messages"};
    private final String[] labels={"Start","Oceny","Plan","Wiadomości","Więcej"};
    private final IconView[] icons=new IconView[5];
    private final TextView[] navLabels=new TextView[5];

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("eduflow",MODE_PRIVATE);
        try {css=asset("mobile.css");injector=asset("inject.js");navigator=asset("navigate.js");}
        catch(Exception e){new AlertDialog.Builder(this).setMessage("Brak zasobów aplikacji.").setPositiveButton("Zamknij",(d,w)->finish()).setCancelable(false).show();return;}
        configureColors();
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg);
        setContentView(root);
        if(Build.VERSION.SDK_INT>=30) getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets a=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(a.left,a.top,a.right,a.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
        top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(16),dp(10),dp(12),dp(10));
        TextView logo=text("e",27,accent);logo.setGravity(Gravity.CENTER);logo.setTypeface(null,Typeface.BOLD);logo.setBackground(shape(surface,14));
        top.addView(logo,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout title=new LinearLayout(this);title.setOrientation(LinearLayout.VERTICAL);title.setPadding(dp(12),0,0,0);
        brand=text("EduFlow",20,ink);brand.setTypeface(null,Typeface.BOLD);host=text("Nieoficjalny klient",11,muted);host.setSingleLine(true);
        title.addView(brand);title.addView(host);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        reload=text("↻",28,accent);reload.setGravity(Gravity.CENTER);reload.setContentDescription("Odśwież stronę");reload.setOnClickListener(v->web.reload());
        top.addView(reload,new LinearLayout.LayoutParams(dp(48),dp(48)));root.addView(top);
        top.setOnLongClickListener(v->{showAddress();return true;});
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));root.addView(progress,new LinearLayout.LayoutParams(-1,dp(2)));
        web=new WebView(this);web.setBackgroundColor(bg);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER);bottom.setPadding(dp(6),dp(6),dp(6),dp(6));root.addView(bottom);
        for(int i=0;i<5;i++){
            final int index=i;
            LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setPadding(0,dp(6),0,dp(6));
            icons[i]=new IconView(this,i,muted);navLabels[i]=text(labels[i],10,muted);navLabels[i].setGravity(Gravity.CENTER);navLabels[i].setPadding(0,dp(4),0,0);
            item.addView(icons[i],new LinearLayout.LayoutParams(dp(24),dp(24)));item.addView(navLabels[i]);bottom.addView(item,new LinearLayout.LayoutParams(0,dp(62),1));
            item.setContentDescription(labels[i]);item.setOnClickListener(v->{if(index==4)showSettings();else navigate(index);});
            if(i<4)item.setOnLongClickListener(v->{saveShortcut(keys[index]);return true;});
        }
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setSafeBrowsingEnabled(true);
        s.setSupportZoom(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);s.setUseWideViewPort(true);s.setLoadWithOverviewMode(true);
        s.setSupportMultipleWindows(true);s.setJavaScriptCanOpenWindowsAutomatically(false);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,prefs.getBoolean("thirdPartyCookies",false));
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){if(!r.isForMainFrame())return false;return route(r.getUrl());}
            @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap icon){Uri u=Uri.parse(url);if(!trusted(u)&&!(demo&&"preview.invalid".equals(u.getHost()))&&!"about:blank".equals(url)){v.stopLoading();pageFailed=true;progress.setVisibility(View.GONE);openExternal(u);return;}pageFailed=false;progress.setVisibility(View.VISIBLE);host.setText(demo?"Podgląd • dane demonstracyjne":u.getHost());}
            @Override public void onPageCommitVisible(WebView v,String url){inject();}
            @Override public void onPageFinished(WebView v,String url){progress.setVisibility(View.GONE);if(!pageFailed)inject();}
            @Override public void onReceivedSslError(WebView v,SslErrorHandler handler,SslError error){handler.cancel();pageFailed=true;toast("Błąd certyfikatu HTTPS — połączenie zablokowane.");}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()){pageFailed=true;progress.setVisibility(View.GONE);toast("Nie udało się wczytać strony. Sprawdź internet i użyj ↻.");}}
            @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse e){if(r.isForMainFrame()){pageFailed=true;toast("Serwer zwrócił HTTP "+e.getStatusCode()+". Możesz odświeżyć stronę.");}}
            @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail d){root.removeView(web);web.destroy();toast("Proces strony został zamknięty. Uruchamiam ponownie aplikację.");recreate();return true;}
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView v,int value){progress.setProgress(value);}
            @Override public boolean onCreateWindow(WebView v,boolean dialog,boolean userGesture,Message result){
                if(!userGesture)return false;
                final WebView popup=new WebView(MainActivity.this);
                popup.setWebViewClient(new WebViewClient(){
                    @Override public boolean shouldOverrideUrlLoading(WebView p,WebResourceRequest r){if(r.isForMainFrame()){Uri u=r.getUrl();if(trusted(u)){demo=false;web.loadUrl(u.toString());}else openExternal(u);p.post(p::destroy);return true;}return false;}
                });
                ((WebView.WebViewTransport)result.obj).setWebView(popup);result.sendToTarget();new Handler(Looper.getMainLooper()).postDelayed(()->{try{popup.destroy();}catch(Exception ignored){}},10000);return true;
            }
            @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> callback,FileChooserParams params){
                if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;
                try{startActivityForResult(params.createIntent(),PICK_FILE);}catch(Exception e){fileCallback.onReceiveValue(null);fileCallback=null;toast("Brak aplikacji do wyboru plików.");}return true;
            }
        });
        web.setDownloadListener((url,ua,disposition,mime,size)->download(url,ua,disposition,mime));
        paintShell();
        if(state!=null){demo=state.getBoolean("demo",false);if(demo)showDemo();else if(web.restoreState(state)==null)loadHome();}
        else if(!prefs.getBoolean("introduced",false)){
            new AlertDialog.Builder(this).setTitle("EduFlow • wersja 0.1")
                .setMessage("Nieoficjalny klient WebView z mobilnym CSS. Logujesz się wyłącznie na stronie VULCAN. Aplikacja nie ma własnego serwera ani formularza hasła.\n\nZgodność wyglądu po logowaniu wymaga testu na Twoim koncie. Korzystaj zgodnie z regulaminem usługi. Nie jest to aplikacja firmy VULCAN.")
                .setPositiveButton("Otwórz eduVULCAN",(d,w)->{prefs.edit().putBoolean("introduced",true).apply();loadHome();})
                .setNeutralButton("Zobacz podgląd",(d,w)->{prefs.edit().putBoolean("introduced",true).apply();showDemo();}).setCancelable(false).show();
        }else loadHome();
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int sp,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(sp);t.setTextColor(color);return t;}
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private String asset(String name)throws IOException{try(InputStream in=getAssets().open(name)){return new String(readLimited(in,2*1024*1024),StandardCharsets.UTF_8);}}
    private byte[] readLimited(InputStream in,int limit)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;while(out.size()<limit&&(count=in.read(buffer,0,Math.min(buffer.length,limit-out.size())))!=-1)out.write(buffer,0,count);return out.toByteArray();}
    private boolean dark(){String theme=prefs.getString("theme","dark");return "dark".equals(theme)||("system".equals(theme)&&(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);}
    private void configureColors(){boolean d=dark();bg=Color.parseColor(d?"#0E1420":"#F3F6FC");surface=Color.parseColor(d?"#182131":"#FFFFFF");ink=Color.parseColor(d?"#EDF3FF":"#17233A");muted=Color.parseColor(d?"#A9B8D0":"#4D607F");accent=Color.parseColor(d?"#86ADFF":"#285EC7");border=Color.parseColor(d?"#30405A":"#D6DFED");}
    private void paintShell(){configureColors();root.setBackgroundColor(bg);top.setBackgroundColor(bg);bottom.setBackgroundColor(surface);web.setBackgroundColor(bg);brand.setTextColor(ink);host.setTextColor(muted);reload.setTextColor(accent);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));for(int i=0;i<5;i++){icons[i].tint(i==active?accent:muted);navLabels[i].setTextColor(i==active?accent:muted);}getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(surface);if(Build.VERSION.SDK_INT>=30){WindowInsetsController c=getWindow().getInsetsController();if(c!=null)c.setSystemBarsAppearance(dark()?0:WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);}}
    private boolean trusted(Uri uri){if(uri==null||!"https".equalsIgnoreCase(uri.getScheme())||uri.getUserInfo()!=null||(uri.getPort()!=-1&&uri.getPort()!=443))return false;String h=uri.getHost();if(h==null)return false;h=h.toLowerCase(Locale.ROOT);for(String domain:new String[]{"eduvulcan.pl","vulcan.net.pl","vulcan.edu.pl"})if(h.equals(domain)||h.endsWith("."+domain))return true;return false;}
    private boolean route(Uri uri){if(trusted(uri)){demo=false;return false;}openExternal(uri);return true;}
    private void openExternal(Uri uri){String scheme=uri.getScheme();if(!"https".equalsIgnoreCase(scheme)&&!"http".equalsIgnoreCase(scheme)&&!"mailto".equalsIgnoreCase(scheme)&&!"tel".equalsIgnoreCase(scheme)){toast("Ten typ odnośnika nie jest obsługiwany.");return;}new AlertDialog.Builder(this).setTitle("Otworzyć poza EduFlow?").setMessage(uri.toString()+"\n\nSesja aplikacji nie jest przenoszona do przeglądarki.").setNegativeButton("Anuluj",null).setPositiveButton("Otwórz",(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception e){toast("Brak aplikacji obsługującej odnośnik.");}}).show();}
    private void inject(){
        if(web==null)return;
        Uri uri=Uri.parse(web.getUrl()==null?"":web.getUrl());if(!(trusted(uri)||(demo&&"preview.invalid".equals(uri.getHost()))))return;
        try{JSONObject c=new JSONObject();c.put("enabled",prefs.getBoolean("enabled",true));c.put("theme",dark()?"dark":"light");c.put("compact",prefs.getBoolean("compact",false));c.put("css",css);c.put("customCss",prefs.getString("customCss",""));web.evaluateJavascript(injector.replace("__CONFIG__",c.toString()),null);}catch(Exception e){toast("Nie udało się nałożyć CSS.");}
    }
    private void loadHome(){demo=false;active=0;paintShell();String home=prefs.getString("url_home",DEFAULT_HOME);web.loadUrl(trusted(Uri.parse(home))?home:DEFAULT_HOME);}
    private void showDemo(){try{demo=true;active=0;paintShell();web.loadDataWithBaseURL("https://preview.invalid/",asset("preview.html"),"text/html","UTF-8",null);}catch(Exception e){toast("Nie udało się otworzyć podglądu.");}}
    private void navigate(int index){
        if(index==0){loadHome();return;}
        if(demo){toast("To podgląd. Wybierz Start, aby zalogować się do eduVULCAN.");return;}
        active=index;paintShell();String key=keys[index],saved=prefs.getString("url_"+key,"");
        if(trusted(Uri.parse(saved))){web.loadUrl(saved);return;}
        if(!trusted(Uri.parse(web.getUrl()==null?"":web.getUrl()))){toast("Najpierw zaloguj się na stronie eduVULCAN.");return;}
        web.evaluateJavascript(navigator.replace("__SECTION__",JSONObject.quote(key)),value->{
            try{JSONObject result=new JSONObject(value);if(result.optBoolean("found"))return;}catch(Exception ignored){}
            new AlertDialog.Builder(this).setTitle("Przypisz zakładkę "+labels[index]).setMessage("Nie znaleziono widocznego przycisku tej sekcji. Otwórz ją w oryginalnym menu dziennika, a następnie przytrzymaj dolną zakładkę, aby zapisać bieżący adres.\n\nJeśli adres nie zmienia się w tej sekcji, przypisanie URL nie pomoże — potrzebne będzie dopasowanie selektorów.").setPositiveButton("Rozumiem",null).show();
        });
    }
    private void navigateAttendance(){if(demo){toast("Frekwencja jest dostępna po zalogowaniu.");return;}String saved=prefs.getString("url_attendance","");if(trusted(Uri.parse(saved))){web.loadUrl(saved);return;}if(!trusted(Uri.parse(web.getUrl()==null?"":web.getUrl())))return;web.evaluateJavascript(navigator.replace("__SECTION__",JSONObject.quote("attendance")),v->{if(!v.contains("\"found\":true"))toast("Otwórz frekwencję w menu strony i przypisz adres w ustawieniach.");});}
    private void showSettings(){String[] options={"Motyw: "+prefs.getString("theme","dark"),"Mobilny CSS: "+(prefs.getBoolean("enabled",true)?"włączony":"wyłączony"),"Kompaktowy układ: "+(prefs.getBoolean("compact",false)?"tak":"nie"),"Edytuj własny CSS","Importuj plik CSS","Adres startowy","Przypisz / usuń zakładki","Otwórz frekwencję","Odśwież","Podgląd wyglądu (demo)","Cookies między domenami: "+(prefs.getBoolean("thirdPartyCookies",false)?"włączone":"wyłączone"),"Wyczyść sesję i dane stron","Informacje i ograniczenia"};
        new AlertDialog.Builder(this).setTitle("Ustawienia EduFlow").setItems(options,(d,w)->{
            switch(w){
                case 0:new AlertDialog.Builder(this).setTitle("Motyw").setItems(new String[]{"Ciemny","Jasny","Systemowy"},(a,n)->{prefs.edit().putString("theme",new String[]{"dark","light","system"}[n]).apply();paintShell();inject();}).show();break;
                case 1:prefs.edit().putBoolean("enabled",!prefs.getBoolean("enabled",true)).apply();inject();break;
                case 2:prefs.edit().putBoolean("compact",!prefs.getBoolean("compact",false)).apply();inject();break;
                case 3:editCss();break;
                case 4:try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,IMPORT_CSS);}catch(Exception e){toast("Brak aplikacji do wyboru plików.");}break;
                case 5:editHome();break;
                case 6:shortcutMenu();break;
                case 7:navigateAttendance();break;
                case 8:web.reload();break;
                case 9:showDemo();break;
                case 10:boolean enabled=!prefs.getBoolean("thirdPartyCookies",false);new AlertDialog.Builder(this).setTitle("Cookies między domenami").setMessage("Włączaj tylko, jeśli wymaga tego logowanie osadzone między domenami VULCAN. Ustawienie może zwiększyć możliwość śledzenia w witrynie.\n\n"+(enabled?"Włączyć?":"Wyłączyć?")).setNegativeButton("Anuluj",null).setPositiveButton("Zastosuj",(a,n)->{prefs.edit().putBoolean("thirdPartyCookies",enabled).apply();CookieManager.getInstance().setAcceptThirdPartyCookies(web,enabled);web.reload();}).show();break;
                case 11:clearSession();break;
                case 12:new AlertDialog.Builder(this).setTitle("EduFlow 0.1 • prototyp").setMessage("Nieoficjalny klient, bez powiązania z VULCAN.\n\nDane pochodzą z prawdziwej strony. Podgląd ma wyłącznie dane demonstracyjne.\n\nNie ma własnego API, synchronizacji w tle, push ani danych offline. CSS nie odblokowuje funkcji ani płatnych usług. Zmiany strony mogą wymagać korekty selektorów.\n\nHasła nie są odczytywane ani zapisywane przez kod aplikacji. Sesję obsługuje WebView. Wersja debug umożliwia lokalne debugowanie WebView; nie udostępniaj telefonu niezaufanej osobie.\n\nWłasny CSS może ładować zewnętrzne zasoby — importuj tylko zaufane pliki.").setPositiveButton("OK",null).show();break;
            }
        }).show();
    }
    private EditText editor(String value,boolean multi){EditText e=new EditText(this);e.setText(value);e.setTextColor(ink);e.setPadding(dp(16),dp(12),dp(16),dp(12));e.setBackgroundColor(surface);e.setSingleLine(!multi);if(multi){e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);e.setTypeface(Typeface.MONOSPACE);e.setTextSize(12);e.setMinLines(8);e.setMaxLines(18);e.setGravity(Gravity.TOP);}else e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);return e;}
    private void editCss(){EditText e=editor(prefs.getString("customCss",""),true);AlertDialog d=new AlertDialog.Builder(this).setTitle("Twój CSS • max 120 KB").setView(e).setNegativeButton("Anuluj",null).setNeutralButton("Usuń CSS",(a,w)->{prefs.edit().remove("customCss").apply();inject();}).setPositiveButton("Zapisz",null).create();d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String value=e.getText().toString();if(value.getBytes(StandardCharsets.UTF_8).length>122880){toast("CSS jest zbyt duży.");return;}prefs.edit().putString("customCss",value).apply();inject();d.dismiss();}));d.show();}
    private void editHome(){EditText e=editor(prefs.getString("url_home",DEFAULT_HOME),false);AlertDialog d=new AlertDialog.Builder(this).setTitle("Adres startowy HTTPS").setMessage("Dozwolone wyłącznie domeny eduVULCAN i VULCAN.").setView(e).setNegativeButton("Anuluj",null).setNeutralButton("Domyślny",(a,w)->{prefs.edit().remove("url_home").apply();loadHome();}).setPositiveButton("Zapisz",null).create();d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String u=e.getText().toString().trim();if(!trusted(Uri.parse(u))){toast("Podaj adres HTTPS w domenie VULCAN.");return;}prefs.edit().putString("url_home",u).apply();d.dismiss();loadHome();}));d.show();}
    private void shortcutMenu(){new AlertDialog.Builder(this).setTitle("Przypisz bieżący adres do…").setItems(new String[]{"Start","Oceny","Plan","Wiadomości","Frekwencja","Usuń wszystkie przypisania"},(d,w)->{if(w==5){new AlertDialog.Builder(this).setTitle("Usunąć przypisania?").setNegativeButton("Anuluj",null).setPositiveButton("Usuń",(a,n)->{SharedPreferences.Editor e=prefs.edit();for(String key:new String[]{"home","grades","timetable","messages","attendance"})e.remove("url_"+key);e.apply();toast("Usunięto przypisania.");}).show();}else saveShortcut(new String[]{"home","grades","timetable","messages","attendance"}[w]);}).show();}
    private void saveShortcut(String key){String url=web.getUrl();if(demo||url==null||!trusted(Uri.parse(url))){toast("Otwórz najpierw właściwą sekcję eduVULCAN.");return;}String name=key.equals("attendance")?"Frekwencja":labels[java.util.Arrays.asList(keys).indexOf(key)];new AlertDialog.Builder(this).setTitle("Przypisać: "+name+"?").setMessage(url+"\n\nNie zapisuj adresów logowania z jednorazowymi tokenami.").setNegativeButton("Anuluj",null).setPositiveButton("Zapisz",(d,w)->{prefs.edit().putString("url_"+key,url).apply();toast("Zakładka przypisana.");}).show();}
    private void clearSession(){new AlertDialog.Builder(this).setTitle("Wyczyścić sesję?").setMessage("Usunie cookies, dane witryn, pamięć podręczną i historię WebView. Ustawienia oraz własny CSS zostaną zachowane. Nie usuwa już pobranych plików.").setNegativeButton("Anuluj",null).setPositiveButton("Wyczyść",(d,w)->{web.stopLoading();demo=false;web.loadUrl("about:blank");CookieManager.getInstance().removeAllCookies(ok->{CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();web.clearCache(true);web.clearFormData();web.clearHistory();loadHome();toast("Usunięto lokalne dane sesji.");});}).show();}
    private void showAddress(){new AlertDialog.Builder(this).setTitle("Bieżący adres").setMessage(web.getUrl()).setNegativeButton("Zamknij",null).setPositiveButton("Kopiuj",(d,w)->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Adres",web.getUrl()));toast("Skopiowano adres.");}).show();}
    private void download(String url,String ua,String disposition,String mime){Uri uri=Uri.parse(url);if(!trusted(uri)){toast("Pobieranie jest dostępne tylko dla adresów HTTPS VULCAN; blob: nie jest obsługiwany.");return;}String name=URLUtil.guessFileName(url,disposition,mime).replaceAll("[\\\\/:*?\"<>|]","_");new AlertDialog.Builder(this).setTitle("Pobrać plik?").setMessage(name+"\n\nZ: "+uri.getHost()).setNegativeButton("Anuluj",null).setPositiveButton("Pobierz",(d,w)->{try{DownloadManager.Request r=new DownloadManager.Request(uri);r.setTitle(name);if(mime!=null)r.setMimeType(mime);String cookie=CookieManager.getInstance().getCookie(url);if(cookie!=null&&!cookie.isEmpty())r.addRequestHeader("Cookie",cookie);if(ua!=null)r.addRequestHeader("User-Agent",ua);r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,name);((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(r);toast("Zlecono pobieranie — status w powiadomieniach systemu.");}catch(Exception e){toast("Nie udało się zlecić pobierania.");}}).show();}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==PICK_FILE){if(fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));fileCallback=null;}return;}if(req==IMPORT_CSS&&result==RESULT_OK&&data!=null&&data.getData()!=null){try(InputStream in=getContentResolver().openInputStream(data.getData())){if(in==null)throw new IOException();byte[] bytes=readLimited(in,122881);if(bytes.length>122880){toast("Plik CSS jest większy niż 120 KB.");return;}String value=new String(bytes,StandardCharsets.UTF_8);new AlertDialog.Builder(this).setTitle("Zastąpić własny CSS?").setMessage("Import obejmuje "+bytes.length+" bajtów. Importuj tylko zaufane CSS; plik może zawierać odnośniki do zewnętrznych zasobów.").setNegativeButton("Anuluj",null).setPositiveButton("Importuj",(d,w)->{prefs.edit().putString("customCss",value).apply();inject();toast("Zaimportowano CSS.");}).show();}catch(Exception e){toast("Nie udało się odczytać pliku.");}}}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);paintShell();inject();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putBoolean("demo",demo);if(web!=null&&!demo)web.saveState(out);}
    @Override protected void onPause(){if(web!=null)web.onPause();CookieManager.getInstance().flush();super.onPause();}
    @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();}
    @Override protected void onDestroy(){if(fileCallback!=null){fileCallback.onReceiveValue(null);fileCallback=null;}if(web!=null){root.removeView(web);web.stopLoading();web.destroy();web=null;}super.onDestroy();}
}
