package com.goreecloud.launcher;
import android.app.*;
import android.content.*;
import android.text.InputType;
import android.util.Base64;
import android.widget.*;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
/** Launcher-only PIN gate; no OS-wide interception. */
public final class LauncherAppLock {
  private static final int ROUNDS=150000;
  private LauncherAppLock() {}
  private static SharedPreferences prefs(Context c){
    return c.getSharedPreferences("goree_launcher_pin_private",Context.MODE_PRIVATE);
  }
  public static boolean configured(Context c){
    return prefs(c).contains("pin_salt") && prefs(c).contains("pin_hash");
  }
  public static boolean locked(Context c,String pkg){
    return configured(c)&&prefs(c).getStringSet("locked",Collections.emptySet()).contains(pkg);
  }
  public static int count(Context c){
    return prefs(c).getStringSet("locked",Collections.emptySet()).size();
  }
  private static byte[] hash(String pin,byte[] salt)throws Exception{
    PBEKeySpec spec=new PBEKeySpec(pin.toCharArray(),salt,ROUNDS,256);
    try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
      .generateSecret(spec).getEncoded();}
    finally{spec.clearPassword();}
  }
  private static boolean matches(Context c,String pin){
    try{
      SharedPreferences p=prefs(c);
      byte[] salt=Base64.decode(p.getString("pin_salt",""),Base64.NO_WRAP);
      byte[] expected=Base64.decode(p.getString("pin_hash",""),Base64.NO_WRAP);
      byte[] candidate=hash(pin,salt);
      boolean ok=MessageDigest.isEqual(expected,candidate);
      Arrays.fill(candidate,(byte)0);
      return ok;
    }catch(Exception e){return false;}
  }
  public static boolean validPin(String pin){
    return pin!=null&&pin.matches("[0-9]{4,6}");
  }
  private static boolean save(Context c,String pin){
    if(!validPin(pin))return false;
    try{
      byte[] salt=new byte[24];new SecureRandom().nextBytes(salt);
      byte[] result=hash(pin,salt);
      boolean ok=prefs(c).edit()
        .putString("pin_salt",Base64.encodeToString(salt,Base64.NO_WRAP))
        .putString("pin_hash",Base64.encodeToString(result,Base64.NO_WRAP))
        .putInt("failures",0).putLong("cooldown",0).commit();
      Arrays.fill(result,(byte)0);return ok;
    }catch(Exception e){return false;}
  }
  private static EditText entry(Activity a,String hint){
    EditText v=new EditText(a);
    v.setHint(hint);v.setSingleLine(true);
    v.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    v.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(6)});
    return v;
  }
  private static void toast(Activity a,String msg){
    Toast.makeText(a,msg,Toast.LENGTH_SHORT).show();
  }
  public static void require(Activity a,String reason,Runnable onVerified){
    if(!configured(a)){setup(a,onVerified);return;}
    SharedPreferences p=prefs(a);
    if(p.getLong("cooldown",0)>System.currentTimeMillis()){
      toast(a,"Too many attempts. Try again later.");return;
    }
    EditText pin=entry(a,"4–6 digit PIN");
    AlertDialog dlg=new AlertDialog.Builder(a)
      .setTitle(reason).setView(pin)
      .setNegativeButton("Cancel",null).setPositiveButton("Unlock",null).create();
    dlg.setOnShowListener(v->dlg.getButton(AlertDialog.BUTTON_POSITIVE)
      .setOnClickListener(button->{
        String attempt=pin.getText().toString();pin.setText("");
        if(matches(a,attempt)){
          p.edit().putInt("failures",0).putLong("cooldown",0).apply();
          dlg.dismiss();onVerified.run();return;
        }
        int n=p.getInt("failures",0)+1;
        long until=n>=5?System.currentTimeMillis()+60000L*Math.min(30,n-4):0;
        p.edit().putInt("failures",n).putLong("cooldown",until).apply();
        toast(a,until>0?"Temporarily locked":"Incorrect PIN");
        if(until>0)dlg.dismiss();
      }));
    dlg.show();
  }
  public static void setup(Activity a,Runnable done){
    if(configured(a)) require(a,"Verify current PIN",()->create(a,done));
    else create(a,done);
  }
  private static void create(Activity a,Runnable done){
    EditText first=entry(a,"New PIN");
    EditText second=entry(a,"Confirm PIN");
    LinearLayout fields=new LinearLayout(a);
    fields.setOrientation(LinearLayout.VERTICAL);
    fields.setPadding(24,8,24,0);
    fields.addView(first);fields.addView(second);
    AlertDialog dlg=new AlertDialog.Builder(a).setTitle("App Lock PIN")
      .setMessage("Launcher-only protection; Android can still open apps outside GoreeCloud.")
      .setView(fields).setNegativeButton("Cancel",null)
      .setPositiveButton("Save PIN",null).create();
    dlg.setOnShowListener(v->dlg.getButton(AlertDialog.BUTTON_POSITIVE)
      .setOnClickListener(button->{
        String aPin=first.getText().toString(),bPin=second.getText().toString();
        if(!aPin.matches("[0-9]{4,6}")||!aPin.equals(bPin)){
          toast(a,"Enter matching 4–6 digit PINs");return;
        }
        if(save(a,aPin)){first.setText("");second.setText("");dlg.dismiss();done.run();}
        else toast(a,"Could not save PIN");
      }));
    dlg.show();
  }
  public static void setLocked(Activity a,String pkg,boolean lock,Runnable after){
    require(a,lock?"Confirm PIN to lock":"Confirm PIN to unlock",()->{
      SharedPreferences p=prefs(a);
      Set<String> next=new HashSet<>(p.getStringSet("locked",Collections.emptySet()));
      if(lock)next.add(pkg);else next.remove(pkg);
      if(p.edit().putStringSet("locked",next).commit()){
        toast(a,lock?"App locked":"App unlocked");after.run();
      }
    });
  }
  public static void guard(Activity a,String pkg,Runnable start){
    if(locked(a,pkg))require(a,"Unlock app",start);
    else start.run();
  }
}
