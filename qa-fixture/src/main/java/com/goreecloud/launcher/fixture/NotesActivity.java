package com.goreecloud.launcher.fixture;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

/** Second non-sensitive launcher QA target used to verify shortcut sorting. */
public final class NotesActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView view = new TextView(this);
        view.setText("QA notes running");
        view.setTextSize(24);
        view.setTextColor(0xFF2C227C);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(0xFFE8E6FF);
        setContentView(view);
    }
}
