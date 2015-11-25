package com.fluttershy.myapplication;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

/**
 * The configuration screen for the {@link GuapTimeRasp4x1 GuapTimeRasp4x1} AppWidget.
 */
public class GuapTimeRasp4x1ConfigureActivity extends Activity {

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private static final String PREFS_NAME = "com.fluttershy.myapplication.GuapTimeRasp4x1";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    MyList[] myList = new MyList[5];
    RaspParse rp;
    Base db;

    public GuapTimeRasp4x1ConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        LOG("Conf Create");
        super.onCreate(icicle);

        setResult(RESULT_CANCELED);

        setContentView(R.layout.guap_time_rasp4x1_configure);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
        if (db==null) db = new Base(this);
        if (rp==null) rp = new RaspParse(this,db);
        showAllList();
        SrchListInit();
    }

    static void saveTitlePref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
        prefs.commit();
    }

    static String loadTitlePref(Context context, int appWidgetId) {
        //LOG("Conf Load Pref");
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (titleValue != null) {
            return titleValue;
        } else {
            return context.getString(R.string.time_guap_rasp4x1_defgroup);
        }
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.commit();
    }
    static public void LOG(String s){
        System.out.println("LOG:3 " + s);
    }

    public void SrchListInit() {
        AdapterView.OnItemClickListener myFunc = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findViewById(R.id.editText).getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

                final Context context = GuapTimeRasp4x1ConfigureActivity.this;

                MyList ml = myList[4];
                String gname = ml.rowsreal.get(position);
                LOG("My string "+gname);
                saveTitlePref(context, mAppWidgetId, gname);
                //AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                //GuapTimeRasp4x1.updateAppWidget(context, appWidgetManager, mAppWidgetId);
                GuapTimeRasp4x1.repeat(context,true);
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);

                //GuapTimeRasp.callLater(context);
                finish();
            }
        };
        TextWatcher inputTW = new TextWatcher() {
            public void afterTextChanged(Editable s) {SrchList(s.toString());}
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        };
        int i=4;
        ((EditText) findViewById(R.id.editText)).addTextChangedListener(inputTW);
        myList[i] = new MyList(this,R.id.listView, i);
        myList[i].listView = (ListView) findViewById(myList[i].list_id);
        myList[i].listView.setAdapter(myList[i].adapter);
        myList[i].listView.setOnItemClickListener(myFunc);
        SrchList("");
    }
    public void SrchList(String s){
        int i = 4;
        myList[i].clear();

        int c1, c2;
        ListMas lm;
        ListMasItem lmi;
        c1 = rp.listM.size();
        for (int i1 = 0; i1 < c1 ; i1++){
            lm = rp.listM.get(i1);
            c2 = lm.list.size();
            for (int i2 = 0; i2 < c2 ; i2++){
                lmi = lm.list.get(i2);
                if (lmi.gname.toLowerCase().contains(s.toLowerCase())) {
                    myList[i].ids.add(lmi.gid);
                    myList[i].rows.add(lmi.gname);
                    myList[i].rowsreal.add(lmi.gname);
                }
            }
        }
        myList[i].adapter.notifyDataSetChanged();
    }

    public void showAllList(){
        ListMas lm;
        ListMasItem lmi;
        if (rp.listM==null) rp.getListMas();

        int c1, c2;
        c1 = rp.listM.size();
        for (int i = 0; i < c1 ; i++){
            myList[i] = new MyList(this,0, i);

            lm = rp.listM.get(i);
            c2 = lm.list.size();
            for (int i2 = 0; i2 < c2 ; i2++){
                lmi = lm.list.get(i2);

                myList[i].ids.      add(lmi.gid);
                myList[i].rows.     add(lmi.gname);
                myList[i].rowsreal. add(lmi.gname);
            }
        }
    }
}

