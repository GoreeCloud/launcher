package com.goreecloud.launcher.fixture;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

/** Deliberately tiny permission-free app to test launcher discovery and cross-app launch. */
public final class FixtureActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView message = new TextView(this);
        message.setText("QA target running");
        message.setTextSize(27);
        message.setTypeface(null, Typeface.BOLD);
        message.setGravity(Gravity.CENTER);
        message.setTextColor(0xFF2C227C);
        message.setBackgroundColor(0xFFE8E6FF);
        message.setContentDescription("QA target running");
        setContentView(message);
    }
}
