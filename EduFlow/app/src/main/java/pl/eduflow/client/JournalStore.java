package pl.eduflow.client;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

final class JournalStore {
    private static final Object LOCK=new Object();
    private static final String ALIAS="eduflow.journal.v1";
    private static final int LIMIT=4*1024*1024;
    private final AtomicFile file;
    JournalStore(Context context){file=new AtomicFile(new File(context.getFilesDir(),"journal-cache.bin"));}
    private SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(store.containsAlias(ALIAS))return (SecretKey)store.getKey(ALIAS,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    JSONObject load() throws Exception { synchronized(LOCK){
        if(!file.getBaseFile().exists())return new JSONObject().put("profiles",new JSONArray());
        byte[] data;
        try(InputStream in=file.openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1){out.write(buffer,0,n);if(out.size()>LIMIT)throw new IOException("CACHE_TOO_LARGE");}data=out.toByteArray();
        }
        if(data.length<30||data[0]!=1||data[1]!=12)throw new IOException("CACHE_FORMAT");
        byte[] iv=java.util.Arrays.copyOfRange(data,2,14);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));
        JSONObject result=new JSONObject(new String(cipher.doFinal(data,14,data.length-14),StandardCharsets.UTF_8));
        if(!(result.opt("profiles") instanceof JSONArray))throw new IOException("CACHE_FORMAT");
        return result;
    }}
    void save(JSONObject profile) throws Exception { synchronized(LOCK){
        JSONObject data=load();JSONArray old=data.getJSONArray("profiles"),next=new JSONArray();
        next.put(profile);
        for(int i=0;i<old.length()&&next.length()<16;i++){JSONObject p=old.getJSONObject(i);if(!p.optString("id").equals(profile.getString("id")))next.put(p);}
        data.put("profiles",next);
        byte[] plain=data.toString().getBytes(StandardCharsets.UTF_8);if(plain.length>LIMIT-100)throw new IOException("CACHE_TOO_LARGE");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        byte[] encrypted=cipher.doFinal(plain),iv=cipher.getIV();if(iv.length!=12)throw new IOException("CACHE_IV");
        FileOutputStream out=null;
        try{out=file.startWrite();out.write(1);out.write(iv.length);out.write(iv);out.write(encrypted);file.finishWrite(out);}
        catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
    }}
    void clear(){synchronized(LOCK){file.delete();}}
}
