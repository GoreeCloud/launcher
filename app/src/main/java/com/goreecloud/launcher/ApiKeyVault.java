package com.goreecloud.launcher;
import android.content.*;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
/** Keys protected by device Android Keystore AES-GCM. Never included in backups. */
public final class ApiKeyVault {
  private static final String ALIAS="goree_search_provider_v1";
  private ApiKeyVault(){}
  private static SharedPreferences storage(Context c){
    return c.getSharedPreferences("goree_provider_secrets_private",Context.MODE_PRIVATE);
  }
  private static SecretKey key()throws Exception {
    KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
    KeyStore.Entry entry=store.getEntry(ALIAS,null);
    if(entry instanceof KeyStore.SecretKeyEntry)
      return ((KeyStore.SecretKeyEntry)entry).getSecretKey();
    KeyGenerator generator=KeyGenerator.getInstance(
       KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
    generator.init(new KeyGenParameterSpec.Builder(ALIAS,
       KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
       .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
       .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
       .setRandomizedEncryptionRequired(true).build());
    return generator.generateKey();
  }
  public static boolean has(Context c,String provider){
    return storage(c).contains("k_"+provider);
  }
  public static boolean save(Context c,String provider,String value){
    try{
      if(value==null||value.trim().isEmpty())return false;
      Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE,key());
      byte[] raw=cipher.doFinal(value.trim().getBytes("UTF-8"));
      String encoded=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)
        +"."+Base64.encodeToString(raw,Base64.NO_WRAP);
      return storage(c).edit().putString("k_"+provider,encoded).commit();
    }catch(Exception ignored){return false;}
  }
  public static String load(Context c,String provider){
    try{
      String[] parts=storage(c).getString("k_"+provider,"").split("\\.");
      if(parts.length!=2)return "";
      Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE,key(),
        new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
      return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),"UTF-8");
    }catch(Exception ignored){return "";}
  }
  public static void clear(Context c,String provider){
    storage(c).edit().remove("k_"+provider).apply();
  }
}
