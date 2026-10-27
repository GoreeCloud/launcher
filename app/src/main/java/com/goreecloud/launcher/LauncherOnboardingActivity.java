package com.goreecloud.launcher;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/** Local-only first-run personalization. Always possible to skip. */
public final class LauncherOnboardingActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout body;
    private int step, homeRows=5, drawerRows=5, columns=4;
    private boolean labels=true, dock=true, swipe=true, wallpaper=true;
    private boolean swipeSearch=true, searchApps=true, searchWeb=true;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        GoreeVisuals.useLightSystemBarIcons(this);
        prefs=getSharedPreferences("launcher",MODE_PRIVATE);
        homeRows=prefs.getInt("home_rows",5);
        drawerRows=prefs.getInt("drawer_rows",5);
        columns=prefs.getInt("columns",4);
        labels=prefs.getBoolean("labels",true);
        dock=prefs.getBoolean("dock_enabled",true);
        swipe=prefs.getBoolean("swipe_up_drawer",true);
        swipeSearch=prefs.getBoolean("swipe_down_search",true);
        searchApps=prefs.getBoolean("search_apps",true);
        searchWeb=prefs.getBoolean("search_web",true);
        wallpaper=!"night".equals(prefs.getString("launcher_skin","wallpaper"));
        if(state!=null) step=state.getInt("step",0);
        showStep();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("step",step);
        super.onSaveInstanceState(state);
    }
    private void showStep() {
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF191B29);
        setContentView(scroll);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(dp(20),dp(44),dp(20),dp(20));
        scroll.addView(root);
        root.addView(label("GoreeCloud Launcher",26));
        root.addView(label("Setup "+(step+1)+" of 3",14));
        body=new LinearLayout(this);
        body.setOrientation(1);
        LinearLayout.LayoutParams bodyParams=new LinearLayout.LayoutParams(-1,0,1);
        root.addView(body,bodyParams);
        if(step==0) {
            heading("Your Home, your way");
            toggle("Use device wallpaper",wallpaper,v->wallpaper=v);
            toggle("Show five-app dock",dock,v->dock=v);
            toggle("Show app labels",labels,v->labels=v);
            body.addView(label("Preferred apps: Phone, Messages, Email, Browser, Camera.",14));
        } else if(step==1) {
            heading("Grid layout");
            slide("Home rows",homeRows,4,7,v->homeRows=v);
            slide("Drawer rows",drawerRows,4,7,v->drawerRows=v);
            slide("Grid columns",columns,3,6,v->columns=v);
        } else {
            heading("Gestures and editing");
            toggle("Swipe up to open apps",swipe,v->swipe=v);
            toggle("Swipe down for Universal Search",swipeSearch,v->swipeSearch=v);
            toggle("Search installed apps",searchApps,v->searchApps=v);
            toggle("Offer web search actions",searchWeb,v->searchWeb=v);
            body.addView(label("Long-press an empty Home space for Widgets, original GoreeCloud wallpapers and Launcher Settings. Hold then drag app icons to a Home page.",15));
            body.addView(label("Nothing is uploaded. You can change these options later.",13));
        }
        LinearLayout actions=new LinearLayout(this);
        root.addView(actions);
        Button back=button("Back",()->{step--;showStep();});
        back.setEnabled(step>0);
        actions.addView(back,new LinearLayout.LayoutParams(0,dp(60),1));
        actions.addView(button("Skip",this::complete),new LinearLayout.LayoutParams(0,dp(60),1));
        actions.addView(button(step==2?"Finish":"Next",()->{
            if(step<2){step++;showStep();}else complete();
        }),new LinearLayout.LayoutParams(0,dp(60),1));
    }
    private void heading(String title){body.addView(label(title,22));}
    private interface BoolChange{void change(boolean b);}
    private void toggle(String name,boolean initial,BoolChange change){
        CheckBox b=new CheckBox(this);b.setText(name);b.setTextSize(16);
        b.setTextColor(Color.WHITE);b.setChecked(initial);b.setMinHeight(dp(56));
        b.setOnCheckedChangeListener((v,state)->change.change(state));body.addView(b);
    }
    private interface IntChange{void change(int value);}
    private void slide(String name,int initial,int min,int max,IntChange change){
        TextView t=label(name+": "+initial,16);body.addView(t);
        SeekBar b=new SeekBar(this);b.setMax(max-min);b.setProgress(Math.max(0,Math.min(max-min,initial-min)));
        b.setContentDescription(name);
        b.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){
                t.setText(name+": "+(min+progress));if(fromUser)change.change(min+progress);
            }
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });body.addView(b);
    }
    private void complete(){
        prefs.edit().putBoolean("onboarding_completed_v1",true)
            .putInt("home_rows",homeRows).putInt("drawer_rows",drawerRows)
            .putInt("columns",columns).putBoolean("labels",labels)
            .putBoolean("dock_enabled",dock).putBoolean("swipe_up_drawer",swipe)
            .putBoolean("swipe_down_search",swipeSearch)
            .putBoolean("search_apps",searchApps).putBoolean("search_web",searchWeb)
            .putString("launcher_skin",wallpaper?"wallpaper":"night").apply();
        finish();
    }
    @Override public void onBackPressed(){
        if(step>0){step--;showStep();}else complete();
    }
    private TextView label(String text,int sp){
        TextView t=new TextView(this);t.setText(text);t.setTextColor(Color.WHITE);
        t.setTextSize(sp);t.setPadding(0,dp(12),0,dp(12));return t;
    }
    private Button button(String text,Runnable action){
        Button b=new Button(this);b.setAllCaps(false);b.setText(text);b.setOnClickListener(v->action.run());return b;
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
