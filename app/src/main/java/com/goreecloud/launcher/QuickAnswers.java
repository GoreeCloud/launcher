package com.goreecloud.launcher;
import android.app.Activity;
import android.content.*;
import android.os.Handler;
import android.os.Looper;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
/** Explicit text-only requests; no silent search-query uploads. */
public final class QuickAnswers {
  public interface Callback{void result(String text);}
  public static final String[] PROVIDERS={"GoreeCloud Search","OpenAI","Claude","Gemini"};
  private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
  private QuickAnswers(){}
  public static String code(String provider){
    switch(provider){
      case "OpenAI":return "openai";
      case "Claude":return "claude";
      case "Gemini":return "gemini";
      default:return "goreecloud";
    }
  }
  public static String selected(Context c){
    return c.getSharedPreferences("launcher",Context.MODE_PRIVATE)
      .getString("quick_provider","GoreeCloud Search");
  }
  public static void ask(Activity a,String question,Callback callback){
    final String provider=selected(a), id=code(provider);
    final String model=a.getSharedPreferences("launcher",Context.MODE_PRIVATE)
      .getString("model_"+id,"").trim();
    final String apiKey=ApiKeyVault.load(a,id);
    if("goreecloud".equals(id)){
      callback.result("GoreeCloud Search quick-answer API isn't configured yet.");
      return;
    }
    if(!model.matches("[a-zA-Z0-9._-]{2,100}") || apiKey.isEmpty()){
      callback.result("Configure the provider model and API key in Launcher Settings.");
      return;
    }
    if(question.trim().length()<2 || question.length()>2400){
      callback.result("Please enter a question under 2,400 characters.");
      return;
    }
    WORK.execute(()->{
      String result;
      try{result=fetch(id,model,apiKey,question.trim());}
      catch(Exception ignored){result="Request failed. Check your provider settings or connection.";}
      final String answer=result;
      new Handler(Looper.getMainLooper()).post(()->{
        if(!a.isFinishing()&&!a.isDestroyed())callback.result(answer);
      });
    });
  }
  private static String fetch(String id,String model,String key,String question)throws Exception{
    String endpoint;
    JSONObject body=new JSONObject();
    if("openai".equals(id)){
      endpoint="https://api.openai.com/v1/responses";
      body.put("model",model).put("input",question).put("store",false)
          .put("max_output_tokens",350);
    }else if("claude".equals(id)){
      endpoint="https://api.anthropic.com/v1/messages";
      body.put("model",model).put("max_tokens",350);
      body.put("messages",new JSONArray().put(
          new JSONObject().put("role","user").put("content",question)));
    }else{
      endpoint="https://generativelanguage.googleapis.com/v1beta/models/"
          +model+":generateContent";
      body.put("contents",new JSONArray().put(new JSONObject()
          .put("parts",new JSONArray().put(new JSONObject().put("text",question)))));
    }
    HttpURLConnection conn=(HttpURLConnection)new URL(endpoint).openConnection();
    conn.setRequestMethod("POST");conn.setConnectTimeout(12000);
    conn.setReadTimeout(20000);conn.setDoOutput(true);
    conn.setRequestProperty("Content-Type","application/json");
    if("openai".equals(id))conn.setRequestProperty("Authorization","Bearer "+key);
    if("claude".equals(id)){
      conn.setRequestProperty("x-api-key",key);
      conn.setRequestProperty("anthropic-version","2023-06-01");
    }
    if("gemini".equals(id))conn.setRequestProperty("x-goog-api-key",key);
    try {
      byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);
      try(OutputStream out=conn.getOutputStream()){out.write(bytes);}
      if(conn.getResponseCode()<200||conn.getResponseCode()>=300)
        return "Provider returned HTTP "+conn.getResponseCode()+". Check key and quota.";
      StringBuilder response=new StringBuilder();
      try(InputStreamReader reader=new InputStreamReader(conn.getInputStream(),StandardCharsets.UTF_8)){
        char[] buffer=new char[1024];int n;
        while((n=reader.read(buffer))>0&&response.length()<50000)
          response.append(buffer,0,n);
      }
      JSONObject json=new JSONObject(response.toString());
      StringBuilder result=new StringBuilder();
      JSONArray items="openai".equals(id)?json.optJSONArray("output")
          :"claude".equals(id)?json.optJSONArray("content"):null;
      if(items!=null)for(int i=0;i<items.length();i++){
        JSONObject block=items.optJSONObject(i);if(block==null)continue;
        if("claude".equals(id))result.append(block.optString("text",""));
        else{
          JSONArray contents=block.optJSONArray("content");
          if(contents!=null)for(int j=0;j<contents.length();j++)
            result.append(contents.getJSONObject(j).optString("text",""));
        }
      }
      if("gemini".equals(id)){
        JSONArray candidates=json.optJSONArray("candidates");
        if(candidates!=null&&candidates.length()>0){
          JSONArray parts=candidates.getJSONObject(0).getJSONObject("content").optJSONArray("parts");
          if(parts!=null)for(int j=0;j<parts.length();j++)
            result.append(parts.getJSONObject(j).optString("text",""));
        }
      }
      return result.length()==0?"Provider returned no answer.":
          result.substring(0,Math.min(6000,result.length()));
    }finally{conn.disconnect();}
  }
}
