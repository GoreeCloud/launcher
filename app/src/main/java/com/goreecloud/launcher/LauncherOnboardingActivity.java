package com.goreecloud.launcher;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.View;
import android.view.WindowInsetsController;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.view.WindowInsets;
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
        scroll.setBackgroundColor(0xFFF2F6F8);
        FrameLayout page = new FrameLayout(this);
        page.addView(scroll, new FrameLayout.LayoutParams(-1,-1));
        setContentView(page);
        // Onboarding uses a light GC Canvas; system-bar icons must be dark.
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController bars = getWindow().getInsetsController();
            if (bars != null) {
                int flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                bars.setSystemBarsAppearance(flags, flags);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(dp(20),dp(44),dp(20),dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        TextView brand = label("GoreeCloud Launcher",25);
        brand.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(brand);
        TextView intro = label("Your space. Your choice.",14);
        intro.setTextColor(0xFF546778);
        root.addView(intro);

        LinearLayout progress = new LinearLayout(this);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1,dp(5));
        progressParams.topMargin=dp(17);
        root.addView(progress,progressParams);
        for (int i=0;i<3;i++) {
            View segment = new View(this);
            segment.setBackground(GoreeVisuals.card(getResources(),
                i<=step ? 0xFF126979 : 0xFFD8E3E7,6));
            LinearLayout.LayoutParams segmentParams = new LinearLayout.LayoutParams(0,dp(5),1);
            if (i>0) segmentParams.leftMargin=dp(8);
            progress.addView(segment,segmentParams);
        }

        body=new LinearLayout(this);
        body.setOrientation(1);
        body.setPadding(dp(20),dp(18),dp(20),dp(20));
        body.setBackground(GoreeVisuals.card(getResources(),Color.WHITE,24));
        LinearLayout.LayoutParams bodyParams=new LinearLayout.LayoutParams(-1,-2);
        bodyParams.topMargin=dp(18);
        root.addView(body,bodyParams);
        TextView stepLabel = label("Setup "+(step+1)+" of 3",12);
        stepLabel.setTextColor(0xFF126979);
        stepLabel.setLetterSpacing(0.1f);
        body.addView(stepLabel);
        if(step==0) {
            heading("Your Home, your way");
            toggle("Use device wallpaper",wallpaper,v->wallpaper=v);
            toggle("Show five-app dock",dock,v->dock=v);
            toggle("Show app labels",labels,v->labels=v);
            TextView note=label("Starter dock: Phone, Messages, Email, Browser and Camera. Change these anytime.",13);
            note.setTextColor(0xFF546778);
            body.addView(note);
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
            TextView note=label("Your setup choices stay on device. Selecting a web search later sends your query to that provider.",13);
            note.setTextColor(0xFF546778);
            body.addView(note);
        }
        LinearLayout actions=new LinearLayout(this);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1,-2);
        actionsParams.topMargin=dp(16);
        root.addView(actions,actionsParams);
        Button back=button("Back",()->{step--;showStep();});
        back.setEnabled(step>0);
        actions.addView(back,new LinearLayout.LayoutParams(0,dp(52),1));
        Button skip=button("Skip",this::complete);
        LinearLayout.LayoutParams skipParams=new LinearLayout.LayoutParams(0,dp(52),1);
        skipParams.leftMargin=dp(8);
        actions.addView(skip,skipParams);
        Button next=button(step==2?"Finish":"Next",()->{
            if(step<2){step++;showStep();}else complete();
        });
        next.setBackground(GoreeVisuals.card(getResources(),0xFF126979,16));
        next.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams nextParams=new LinearLayout.LayoutParams(0,dp(52),1);
        nextParams.leftMargin=dp(8);
        actions.addView(next,nextParams);
        TextView footer=label("Adjust your choices later in Launcher Settings.",12);
        footer.setGravity(Gravity.CENTER);
        footer.setTextColor(0xFF546778);
        root.addView(footer);
        // At 200% text size the third step cannot fit vertically. Pin the
        // navigation row above Android's gesture area while allowing the
        // entire setup content to scroll underneath it.
        if (getResources().getConfiguration().fontScale >= 1.6f) {
            root.removeView(actions);
            root.setPadding(dp(20),dp(44),dp(20),dp(24));
            LinearLayout actionSurface = new LinearLayout(this);
            actionSurface.setOrientation(LinearLayout.VERTICAL);
            actionSurface.setPadding(dp(12),dp(10),dp(12),dp(10));
            actionSurface.setBackground(GoreeVisuals.card(getResources(),Color.WHITE,20));
            actionSurface.addView(actions,new LinearLayout.LayoutParams(-1,-2));
            FrameLayout.LayoutParams floating = new FrameLayout.LayoutParams(
                    -1,-2,Gravity.BOTTOM);
            floating.setMargins(dp(12),0,dp(12),dp(16));
            page.addView(actionSurface,floating);
            // The pinned footer must occupy its own layout space rather than
            // covering scrollable help text. Recalculate when font size,
            // navigation insets or screen orientation changes its bounds.
            actionSurface.addOnLayoutChangeListener(
                (view,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
                    if (page.getHeight() <= 0) return;
                    FrameLayout.LayoutParams viewport =
                        (FrameLayout.LayoutParams) scroll.getLayoutParams();
                    int reserved = Math.max(0, page.getHeight() - top + dp(8));
                    if (viewport.bottomMargin != reserved) {
                        viewport.bottomMargin = reserved;
                        scroll.setLayoutParams(viewport);
                    }
                });
            actionSurface.setOnApplyWindowInsetsListener((view,insets)->{
                int bottom = Build.VERSION.SDK_INT >= 30
                    ? insets.getInsets(WindowInsets.Type.navigationBars()).bottom
                    : insets.getSystemWindowInsetBottom();
                FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)view.getLayoutParams();
                int required=dp(12)+bottom;
                if(lp.bottomMargin!=required){
                    lp.bottomMargin=required;
                    view.setLayoutParams(lp);
                }
                return insets;
            });
        }
    }
    private void heading(String title){
        TextView heading=label(title,22);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        body.addView(heading);
    }
    private interface BoolChange{void change(boolean b);}
    private void toggle(String name,boolean initial,BoolChange change){
        CheckBox b=new CheckBox(this);b.setText(name);b.setTextSize(16);
        b.setTextColor(0xFF182738);b.setChecked(initial);b.setMinHeight(dp(52));
        b.setButtonTintList(ColorStateList.valueOf(0xFF126979));
        b.setOnCheckedChangeListener((v,state)->change.change(state));body.addView(b);
    }
    private interface IntChange{void change(int value);}
    private void slide(String name,int initial,int min,int max,IntChange change){
        TextView t=label(name+": "+initial,16);body.addView(t);
        SeekBar b=new SeekBar(this);b.setMax(max-min);b.setProgress(Math.max(0,Math.min(max-min,initial-min)));
        b.setContentDescription(name);
        b.setProgressTintList(ColorStateList.valueOf(0xFF126979));
        b.setThumbTintList(ColorStateList.valueOf(0xFF126979));
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
        TextView t=new TextView(this);t.setText(text);t.setTextColor(0xFF182738);
        t.setTextSize(sp);t.setPadding(0,dp(6),0,dp(6));return t;
    }
    private Button button(String text,Runnable action){
        Button b=new Button(this);
        b.setAllCaps(false);b.setText(text);b.setTextSize(15);
        b.setTextColor(0xFF126979);
        b.setBackground(GoreeVisuals.card(getResources(),0xFFE7F1F2,16));
        b.setOnClickListener(v->action.run());return b;
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
