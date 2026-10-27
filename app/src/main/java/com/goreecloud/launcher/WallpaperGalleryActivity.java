package com.goreecloud.launcher;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class WallpaperGalleryActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF171B29);
        GoreeVisuals.useLightSystemBarIcons(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF171B29);
        scroll.setPadding(dp(18),dp(45),dp(18),dp(32));
        setContentView(scroll);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(1);scroll.addView(body);
        TextView back = label("‹   GoreeCloud Wallpapers",23);
        back.setOnClickListener(v->finish());body.addView(back);
        body.addView(label("Original GoreeCloud collection • offline",14));
        TextView device=label("▧   Choose a device wallpaper",16);
        device.setBackground(GoreeVisuals.card(getResources(),0xFF2C3C4A,20));
        device.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_SET_WALLPAPER)));
        body.addView(device);
        for(int index=0;index<GoreeWallpapers.count();index++) {
            final int theme=index;
            LinearLayout card=new LinearLayout(this);
            card.setOrientation(1);
            card.setPadding(dp(8),dp(8),dp(8),dp(8));
            card.setBackground(GoreeVisuals.card(getResources(),0xFF273647,20));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.setMargins(0,dp(16),0,0);body.addView(card,lp);
            ImageView preview=new ImageView(this);
            preview.setImageBitmap(GoreeWallpapers.render(theme,240,360));
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(preview,new LinearLayout.LayoutParams(-1,dp(215)));
            card.addView(label(GoreeWallpapers.NAMES[theme]+"   •   Apply",17));
            card.setContentDescription("Preview "+GoreeWallpapers.NAMES[theme]);
            card.setOnClickListener(v->confirm(theme));
        }
    }
    private void confirm(int theme) {
        new AlertDialog.Builder(this).setTitle(GoreeWallpapers.NAMES[theme])
            .setMessage("Apply this original design to the device Home wallpaper?")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Apply wallpaper",(dialog,which)->{
                new Thread(()->{
                    boolean ok=true;
                    try {GoreeWallpapers.apply(this,theme);}
                    catch(Exception ex){ok=false;}
                    final boolean success=ok;
                    runOnUiThread(()->{
                        Toast.makeText(this,success?"Wallpaper applied":"Wallpaper application failed",Toast.LENGTH_LONG).show();
                        if(success)finish();
                    });
                },"goree-wallpaper").start();
            }).show();
    }
    private TextView label(String s,int sp) {
        TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);
        t.setTextColor(0xFFF8FAFF);t.setPadding(dp(12),dp(12),dp(12),dp(12));return t;
    }
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
}
