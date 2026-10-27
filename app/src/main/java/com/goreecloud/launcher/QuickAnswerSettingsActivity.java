package com.goreecloud.launcher;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
/** User-controlled external provider configuration for Universal Search. */
public final class QuickAnswerSettingsActivity extends Activity {
  private SharedPreferences settings;
  private LinearLayout content;
  private String selected;
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);

    settings=getSharedPreferences("launcher",MODE_PRIVATE);
    selected=QuickAnswers.selected(this);
    render();
  }
  private TextView line(String label,int size){
    TextView t=new TextView(this);t.setText(label);
    t.setTextSize(size);t.setTextColor(0xFFF1F7FA);
    t.setPadding(12,12,12,12);return t;
  }
  private Button button(String label,Runnable action){
    Button b=new Button(this);b.setText(label);b.setAllCaps(false);
    b.setOnClickListener(v->action.run());return b;
  }
  private void render(){
    ScrollView scroll=new ScrollView(this);
    scroll.setBackgroundColor(0xFF192737);
    setContentView(scroll);
        GoreeVisuals.useLightSystemBarIcons(this);
    content=new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(20,48,20,36);
    scroll.addView(content);
    content.addView(line("‹   Quick answers",24));
    content.addView(line("Choose a provider for explicit quick-answer requests.",16));
    Spinner chooser=new Spinner(this);
    ArrayAdapter<String> choices=new ArrayAdapter<>(this,
        android.R.layout.simple_spinner_dropdown_item,QuickAnswers.PROVIDERS);
    chooser.setAdapter(choices);
    int index=java.util.Arrays.asList(QuickAnswers.PROVIDERS).indexOf(selected);
    chooser.setSelection(Math.max(0,index));
    content.addView(chooser);
    chooser.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
      public void onNothingSelected(android.widget.AdapterView<?> parent){}
      public void onItemSelected(android.widget.AdapterView<?> parent,
          android.view.View view,int position,long id){
        String next=QuickAnswers.PROVIDERS[position];
        if(!next.equals(selected)){
          selected=next;
          settings.edit().putString("quick_provider",next).apply();
          render();
        }
      }
    });
    if("GoreeCloud Search".equals(selected)){
      content.addView(line("GoreeCloud Search quick answers are unavailable until a supported service API is published. No endpoint or credential has been assumed.",15));
      return;
    }
    String id=QuickAnswers.code(selected);
    content.addView(line("Model ID (from your provider account)",16));
    EditText model=new EditText(this);
    model.setSingleLine(true);
    model.setTextColor(0xFFF2FBFF);
    model.setText(settings.getString("model_"+id,""));
    model.setHint("Enter model identifier");
    content.addView(model);
    content.addView(button("Save model",()->{
      String value=model.getText().toString().trim();
      if(!value.matches("[a-zA-Z0-9._-]{2,100}")){
        Toast.makeText(this,"Enter a valid model ID",Toast.LENGTH_SHORT).show();
        return;
      }
      settings.edit().putString("model_"+id,value).apply();
      Toast.makeText(this,"Model saved",Toast.LENGTH_SHORT).show();
    }));
    content.addView(line(ApiKeyVault.has(this,id)?
        "API key: saved securely on this device":"API key: not configured",15));
    EditText secret=new EditText(this);
    secret.setSingleLine(true);
    secret.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
    secret.setHint("Paste provider API key");
    secret.setTextColor(0xFFF2FBFF);
    content.addView(secret);
    content.addView(button("Save API key",()->{
      String key=secret.getText().toString().trim();
      if(!key.isEmpty()&&ApiKeyVault.save(this,id,key)){
        secret.setText("");
        Toast.makeText(this,"API key encrypted",Toast.LENGTH_SHORT).show();
        render();
      }else Toast.makeText(this,"Could not save key",Toast.LENGTH_SHORT).show();
    }));
    content.addView(button("Delete saved key",()->{
      ApiKeyVault.clear(this,id);
      secret.setText("");
      render();
    }));
    content.addView(line("Security: API requests use your key and may incur provider charges. "
      +"A compromised device can expose client-side credentials; for production use a trusted "
      +"server-side proxy and restricted provider keys. Queries go only to the provider you select, "
      +"and only when you tap Quick answer.",14));
  }
}
