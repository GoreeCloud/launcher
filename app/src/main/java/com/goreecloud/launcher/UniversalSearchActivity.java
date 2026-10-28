package com.goreecloud.launcher;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Local, privacy-gated search; web searches launch only on explicit request. */
public final class UniversalSearchActivity extends Activity {
    private SharedPreferences prefs;
    private EditText search;
    private LinearLayout results;
    private final List<ResolveInfo> apps=new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("launcher",MODE_PRIVATE);
        getWindow().setStatusBarColor(0xFF191B29);
        getWindow().setNavigationBarColor(0xFF191B29);

        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(0xFF191B29);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(20),dp(44),dp(20),dp(24));
        setContentView(scroll);
        GoreeVisuals.useLightSystemBarIcons(this);
        LinearLayout page=new LinearLayout(this);
        page.setOrientation(1);scroll.addView(page);
        TextView header=label("⌕   Universal Search",25,true);
        page.addView(header);
        TextView description=label("On-device results first • you control every source",13,false);
        description.setTextColor(0xFFC2CBE4);page.addView(description);
        LinearLayout inputRow=new LinearLayout(this);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);page.addView(inputRow);
        search=new EditText(this);search.setSingleLine(true);
        search.setTextColor(0xFF1C2734);search.setHintTextColor(0xFF596678);
        search.setHint("Search apps, contacts, files…");
        search.setPadding(dp(15),0,dp(9),0);
        search.setBackground(GoreeVisuals.card(getResources(),0xFFF3F5FB,18));
        inputRow.addView(search,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView options=label("⚙",24,true);
        options.setContentDescription("Universal Search settings");
        options.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        inputRow.addView(options,new LinearLayout.LayoutParams(dp(45),dp(54)));
        results=new LinearLayout(this);results.setOrientation(1);
        page.addView(results);
        Intent discover=new Intent(Intent.ACTION_MAIN);
        discover.addCategory(Intent.CATEGORY_LAUNCHER);
        apps.addAll(getPackageManager().queryIntentActivities(discover,0));
        apps.sort(Comparator.comparing(a->String.valueOf(a.loadLabel(getPackageManager())).toLowerCase(Locale.ROOT)));
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){ render(); }
            public void afterTextChanged(Editable e){}
        });
        render();
        // Swipe-down Search is intended to be a one-gesture workflow.
        // Put the caret in the search field without requiring a second tap.
        search.requestFocus();
        search.postDelayed(() -> {
            if (isFinishing() || isDestroyed() || !search.hasFocus()) return;
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(
                    search, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }, 250);
    }
    @Override public void onResume(){super.onResume();if(results!=null)render();}
    private void section(String title){TextView t=label(title,17,true);t.setPadding(0,dp(19),0,dp(9));results.addView(t);}
    private void item(String text,Runnable action) {
        TextView t=label(text,15,false);t.setMinHeight(dp(48));
        t.setPadding(dp(14),dp(10),dp(14),dp(10));
        t.setBackground(GoreeVisuals.card(getResources(),0xFF2C3447,15));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(5);
        results.addView(t,lp);t.setOnClickListener(v->action.run());
    }
    private void render() {
        results.removeAllViews();
        String q=search.getText().toString().trim();
        if(q.length()<2) {
            section("Start typing");
            item("Search only sources you enable in Launcher Settings",()->startActivity(
                    new Intent(this,SettingsActivity.class)));
            return;
        }
        int count=0;
        if(prefs.getBoolean("search_apps",true)) {
            section("Apps");
            for(ResolveInfo app:apps) {
                if(app.activityInfo==null)continue;
                String title=String.valueOf(app.loadLabel(getPackageManager()));
                if(!title.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))continue;
                ComponentName component=new ComponentName(app.activityInfo.packageName,app.activityInfo.name);
                item(title,()->LauncherAppLock.guard(this,component.getPackageName(),
                    ()->{try{startActivity(Intent.makeMainActivity(component));}
                        catch(Exception ignored){notice();}}));
                if(++count>=14)break;
            }
        }
        if(prefs.getBoolean("search_contacts",false)) {
            section("Contacts");
            if(checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED) {
                contacts(q);
            } else item("Enable Contacts permission in Launcher Settings",()->
                    startActivity(new Intent(this,SettingsActivity.class)));
        }
        if(prefs.getBoolean("search_calls",false)) {
            section("Calls");
            if(checkSelfPermission(Manifest.permission.READ_CALL_LOG)==PackageManager.PERMISSION_GRANTED)calls(q);
            else item("Call history requires eligible Android dialer permission",()->
                    startActivity(new Intent(this,SettingsActivity.class)));
        }
        if(prefs.getBoolean("search_files",false)) {
            section("Files");
            String chosen=prefs.getString("search_files_tree_uri","");
            if(chosen.isEmpty())item("Choose a folder in Launcher Settings",()->
                startActivity(new Intent(this,SettingsActivity.class)));
            else files(q,Uri.parse(chosen));
        }
        section("Quick answers");
        String provider=QuickAnswers.selected(this);
        TextView answer=label("",15,false);
        item("Ask "+provider+" for a quick answer",()->{
            answer.setText("Requesting answer from "+provider+"…");
            QuickAnswers.ask(this,q,text -> showQuickAnswer(answer,provider,text));
        });
        results.addView(answer);
        if(prefs.getBoolean("search_web",true)) {
            section("Web");
            item("Search the web for “"+q+"”",()->web(q));
        }
    }
    /** Tappable, verified first-party result URLs; no automatic web requests. */
    private void showQuickAnswer(TextView answer, String provider, String text) {
        if (!"GoreeCloud Search".equals(provider)) {
            answer.setText(text);
            return;
        }
        List<GoreeCloudSearchContract.SourcedLink> links =
                GoreeCloudSearchContract.sourcedResultLinks(text);
        if (links.isEmpty()) {
            answer.setText(text);
            return;
        }
        SpannableString rich = new SpannableString(text);
        for (GoreeCloudSearchContract.SourcedLink link : links) {
            final String target = link.url;
            rich.setSpan(new ClickableSpan() {
                @Override public void onClick(View view) {
                    // Recheck at tap time; only HTTP(S) links from a formatted
                    // first-party result may leave the Launcher.
                    if (!GoreeCloudSearchContract.safeResultUrl(target)) {
                        notice();
                        return;
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(target));
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    open(intent);
                }
                @Override public void updateDrawState(TextPaint paint) {
                    paint.setColor(0xFFB9E5FF);
                    paint.setUnderlineText(true);
                }
            }, link.start, link.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        answer.setLinksClickable(true);
        answer.setMovementMethod(LinkMovementMethod.getInstance());
        answer.setText(rich, TextView.BufferType.SPANNABLE);
    }

    private void contacts(String q){
        Uri uri=ContactsContract.Contacts.CONTENT_URI;
        String[] columns={ContactsContract.Contacts._ID,ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY};
        try(Cursor c=getContentResolver().query(uri,columns,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY+" LIKE ?",new String[]{"%"+q+"%"},
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY+" ASC")) {
            int n=0;
            while(c!=null && c.moveToNext() && n++<12) {
                Uri target=ContactsContract.Contacts.getLookupUri(c.getLong(0),c.getString(1));
                item(c.getString(2),()->open(new Intent(Intent.ACTION_VIEW,target)));
            }
        }catch(Exception ex){item("Contacts unavailable",()->{});}
    }
    private void calls(String q) {
        try(Cursor c=getContentResolver().query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.NUMBER},""+CallLog.Calls.NUMBER+" LIKE ?",
                new String[]{"%"+q+"%"},CallLog.Calls.DATE+" DESC")) {
            int n=0;
            while(c!=null && c.moveToNext() && n++<12) {
                String number=c.getString(0);
                item("Call  "+number,()->open(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+number))));
            }
        } catch(Exception ex){item("Call history unavailable",()->{});}
    }
    private void files(String q,Uri tree) {
        try {
            Uri folder=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
            Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(folder));
            int n=0;
            try(Cursor c=getContentResolver().query(children,new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)) {
                while(c!=null&&c.moveToNext()&&n<30) {
                    String name=c.getString(1);
                    if(name==null||!name.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))continue;
                    Uri document=DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(0));
                    String mime=c.getString(2);n++;
                    item(name,()->{
                        Intent i=new Intent(Intent.ACTION_VIEW,document);
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        i.setDataAndType(document,mime);
                        open(i);
                    });
                }
            }
        }catch(Exception ex){item("Folder access unavailable. Reselect it in Settings.",()->
            startActivity(new Intent(this,SettingsActivity.class)));}
    }
    private void web(String query){
        Intent i=new Intent(Intent.ACTION_WEB_SEARCH);
        i.putExtra("query",query);
        if(i.resolveActivity(getPackageManager())==null)
            i=new Intent(Intent.ACTION_VIEW,new Uri.Builder().scheme("https")
                .authority("www.google.com").path("search").appendQueryParameter("q",query).build());
        open(i);
    }
    private void open(Intent i){try{startActivity(i);}catch(Exception ex){notice();}}
    private void notice(){Toast.makeText(this,"No app available for this action",Toast.LENGTH_SHORT).show();}
    private TextView label(String s,int size,boolean bold){
        TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(0xFFF5F7FF);
        if(bold)v.setTypeface(null,android.graphics.Typeface.BOLD);
        v.setPadding(dp(4),dp(10),dp(4),dp(10));return v;
    }
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
}
