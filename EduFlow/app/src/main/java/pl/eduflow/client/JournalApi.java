package pl.eduflow.client;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.webkit.WebView;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.UUID;

final class JournalApi {
    interface Callback { void success(JSONObject data); void failure(String code); }
    private final Activity activity;
    private final WebView web;
    private final String script;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String job=null;
    private long started;
    private String pageUrl;
    private int generation=0;
    JournalApi(Activity activity,WebView web,String script){this.activity=activity;this.web=web;this.script=script;}
    static String portalBase(String url){
        if(url==null)return null;
        Uri u=Uri.parse(url);
        if(!"https".equalsIgnoreCase(u.getScheme())||!"uczen.eduvulcan.pl".equalsIgnoreCase(u.getHost())||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))return null;
        String path=u.getEncodedPath();if(path==null)return null;
        String[] parts=path.split("/");
        if(parts.length<2||!parts[1].matches("[a-zA-Z0-9_-]+")||parts[1].equalsIgnoreCase("Account")||parts[1].equalsIgnoreCase("logowanie"))return null;
        return "https://uczen.eduvulcan.pl/"+parts[1];
    }
    void cancel(){
        generation++;handler.removeCallbacksAndMessages(null);
        if(job!=null){String q=JSONObject.quote(job);try{web.evaluateJavascript("(function(){const s=window["+q+"];if(s&&s.cancel)s.cancel();delete window["+q+"];})()",null);}catch(RuntimeException ignored){}job=null;}
    }
    void request(JSONObject config,Callback callback){
        cancel();final int token=generation;
        String base=portalBase(web.getUrl());
        if(base==null){callback.failure("WRONG_PORTAL");return;}
        pageUrl=web.getUrl();started=SystemClock.elapsedRealtime();job="ef_"+UUID.randomUUID().toString().replace("-","");
        try{config.put("base",base);config.put("job",job);}catch(Exception e){callback.failure("CONFIG_ERROR");return;}
        web.evaluateJavascript(script.replace("__CONFIG__",config.toString()),v->{if(token==generation)poll(token,callback);});
    }
    private void poll(int token,Callback callback){
        if(token!=generation||activity.isFinishing()||activity.isDestroyed())return;
        if(!pageUrl.equals(web.getUrl())){cancel();callback.failure("PAGE_CHANGED");return;}
        if(SystemClock.elapsedRealtime()-started>40000){cancel();callback.failure("TIMEOUT");return;}
        String q=JSONObject.quote(job);
        web.evaluateJavascript("JSON.stringify(window["+q+"]?window["+q+"].result:{status:'error',error:'PAGE_CHANGED'})",value->{
            if(token!=generation)return;
            if(value==null||value.length()>3*1024*1024){cancel();callback.failure("PAYLOAD_TOO_LARGE");return;}
            try{
                Object parsed=new JSONTokener(value).nextValue();
                if(!(parsed instanceof String))throw new IllegalArgumentException();
                JSONObject result=new JSONObject((String)parsed);
                String status=result.optString("status");
                if(status.equals("pending")){handler.postDelayed(()->poll(token,callback),250);return;}
                cancel();
                if(status.equals("done")&&result.opt("data") instanceof JSONObject)callback.success(result.getJSONObject("data"));
                else callback.failure(result.optString("error","SCHEMA_RESULT"));
            }catch(Exception e){cancel();callback.failure("SCHEMA_RESULT");}
        });
    }
}
