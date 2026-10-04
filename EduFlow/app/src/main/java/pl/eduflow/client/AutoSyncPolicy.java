package pl.eduflow.client;

final class AutoSyncPolicy {
    private static final long INTERVAL=300000L;
    private boolean foreground=false,enabled=true,blocked=false,running=false;
    private long due=0;
    private int failures=0;
    void resume(long now){foreground=true;if(!blocked)due=Math.min(due,now);}
    void pause(){foreground=false;running=false;}
    void enable(boolean value,long now){enabled=value;if(value&&!blocked)due=now;}
    boolean due(long now){return foreground&&enabled&&!blocked&&!running&&now>=due;}
    boolean begin(long now,boolean manual){
        if(!foreground||running||(!manual&&!due(now)))return false;
        running=true;return true;
    }
    void success(long now){running=false;blocked=false;failures=0;due=now+INTERVAL;}
    void failure(long now,boolean authentication){
        running=false;
        if(authentication){blocked=true;return;}
        failures=Math.min(failures+1,5);due=now+Math.min(900000L,30000L*(1L<<failures));
    }
    void authenticated(long now){blocked=false;failures=0;due=now;}
    boolean running(){return running;}
}
