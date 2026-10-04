package pl.eduflow.client;
import android.app.Activity;
import android.net.Uri;
import android.os.*;
import android.webkit.WebView;
import org.json.*;
import java.util.UUID;

final class SessionJob {
    interface Callback {void success(JSONObject data);void failure(String code);}
    private final Activity activity;
    private final WebView web;
    private final String host;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String job,page;
    private long started;
    private int generation=0;
    private boolean running=false;
    SessionJob(Activity a,WebView w,String h){activity=a;web=w;host=h;}
    static String base(String url,String host){
        if(url==null)return null;Uri u=Uri.parse(url);
        if(!"https".equalsIgnoreCase(u.getScheme())||!host.equalsIgnoreCase(u.getHost())||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))return null;
        String path=u.getEncodedPath();if(path==null)return null;String[] p=path.split("/");
        if(p.length<2||!p[1].matches("[a-zA-Z0-9_-]+")||p[1].equalsIgnoreCase("Account")||p[1].equalsIgnoreCase("logowanie"))return null;
        return "https://"+host+"/"+p[1];
    }
    boolean running(){return running;}
    void cancel(){generation++;running=false;handler.removeCallbacksAndMessages(null);if(job!=null){String q=JSONObject.quote(job);try{web.evaluateJavascript("(function(){const x=window["+q+"];if(x&&x.cancel)x.cancel();delete window["+q+"];})()",null);}catch(RuntimeException ignored){}job=null;}}
    void request(String script,JSONObject config,Callback callback){
        cancel();String b=base(web.getUrl(),host);if(b==null){callback.failure("WRONG_PORTAL");return;}
        final int token=generation;running=true;page=web.getUrl();started=SystemClock.elapsedRealtime();job="ef_"+UUID.randomUUID().toString().replace("-","");
        try{config.put("base",b);config.put("job",job);web.evaluateJavascript(script.replace("__CONFIG__",config.toString()),v->{if(token==generation)poll(token,callback);});}
        catch(Exception e){cancel();callback.failure("CONFIG_ERROR");}
    }
    private void poll(int token,Callback callback){
        if(token!=generation||activity.isFinishing()||activity.isDestroyed())return;
        if(!page.equals(web.getUrl())){cancel();callback.failure("PAGE_CHANGED");return;}
        if(SystemClock.elapsedRealtime()-started>40000){cancel();callback.failure("TIMEOUT");return;}
        String q=JSONObject.quote(job);
        web.evaluateJavascript("JSON.stringify(window["+q+"]?window["+q+"].result:{status:'error',error:'PAGE_CHANGED'})",v->{
            if(token!=generation)return;
            try{
                if(v==null||v.length()>4*1024*1024)throw new IllegalArgumentException();
                Object text=new JSONTokener(v).nextValue();if(!(text instanceof String))throw new IllegalArgumentException();
                JSONObject result=new JSONObject((String)text);
                if(result.optString("status").equals("pending")){handler.postDelayed(()->poll(token,callback),250);return;}
                cancel();if(result.optString("status").equals("done")&&result.opt("data") instanceof JSONObject)callback.success(result.getJSONObject("data"));else callback.failure(result.optString("error","SCHEMA_RESULT"));
            }catch(Exception e){cancel();callback.failure("SCHEMA_RESULT");}
        });
    }
}
