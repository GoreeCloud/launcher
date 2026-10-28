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
  private static final java.util.concurrent.atomic.AtomicBoolean BUSY=new java.util.concurrent.atomic.AtomicBoolean();
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
    final boolean goreecloud = "goreecloud".equals(id);
    final String apiKey=goreecloud ? "" : ApiKeyVault.load(a,id);
    if(!goreecloud && (!model.matches("[a-zA-Z0-9._-]{2,100}") || apiKey.isEmpty())){
      callback.result("Configure the provider model and API key in Launcher Settings.");
      return;
    }
    if(goreecloud ? !GoreeCloudSearchContract.validQuery(question)
            : question.trim().length()<2 || question.length()>2400){
      callback.result(goreecloud
          ? "GoreeCloud Search requires a printable question of 2–500 characters."
          : "Please enter a question under 2,400 characters.");
      return;
    }
    if(!BUSY.compareAndSet(false,true)){
      callback.result("A quick-answer request is already running.");return;
    }
    WORK.execute(()->{
      String result;
      try{result=goreecloud ? fetchGoreeCloud(question.trim())
              : fetch(id,model,apiKey,question.trim());}
      catch(Exception ignored){result="Request failed. Check your provider settings or connection.";}
      finally{BUSY.set(false);}
      final String answer=result;
      new Handler(Looper.getMainLooper()).post(()->{
        if(!a.isFinishing()&&!a.isDestroyed())callback.result(answer);
      });
    });
  }
  private static String fetchGoreeCloud(String question) throws Exception {
    JSONObject payload = new JSONObject().put("query",question).put("category","general")
      .put("range","").put("page",1).put("language","all").put("safesearch",1);
    HttpURLConnection c=(HttpURLConnection)new URL(GoreeCloudSearchContract.ENDPOINT).openConnection();
    c.setConnectTimeout(12000);c.setReadTimeout(18000);
    c.setRequestMethod("POST");c.setDoOutput(true);c.setInstanceFollowRedirects(false);
    c.setRequestProperty("Content-Type","application/json");
    c.setRequestProperty("Accept","application/json");
    try {
      try(OutputStream out=c.getOutputStream()){
        out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
      }
      int status=c.getResponseCode();
      if(status==403)return "GoreeCloud Search requires authorized NetBird access.";
      if(status==429)return "GoreeCloud Search rate limit reached. Try again later.";
      if(status<200||status>=300)return "GoreeCloud Search returned HTTP "+status+".";
      StringBuilder raw=new StringBuilder();
      try(InputStreamReader in=new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8)){
        char[] buf=new char[2048];int n;
        while((n=in.read(buf))>0){
          if(raw.length()+n>512000)return "GoreeCloud Search response exceeded size limit.";
          raw.append(buf,0,n);
        }
      }
      JSONArray results=new JSONObject(raw.toString()).optJSONArray("results");
      if(results==null||results.length()==0)return "No matching GoreeCloud Search results.";
      StringBuilder answer=new StringBuilder(GoreeCloudSearchContract.RESULTS_HEADING);
      int shown=0;
      for(int i=0;i<results.length()&&shown<3;i++){
        JSONObject item=results.optJSONObject(i);if(item==null)continue;
        String url=item.optString("url","");
        if(!GoreeCloudSearchContract.safeResultUrl(url))continue;
        shown++;
        answer.append("\n\n").append(shown).append(". ")
          .append(GoreeCloudSearchContract.safePreviewText(item.optString("title","Result"),140))
          .append("\n")
          .append(GoreeCloudSearchContract.safePreviewText(item.optString("description",""),240))
          .append("\n").append(url);
      }
      return shown==0?"No matching safe GoreeCloud Search results.":answer.toString();
    }finally{c.disconnect();}
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
    conn.setInstanceFollowRedirects(false);
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
