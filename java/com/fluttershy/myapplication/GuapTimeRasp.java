package com.fluttershy.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

class PingPref {

    private static final String PREF = "pref";
    private static final String pre_ = "wid";
    private static final String _data = "_data";

    private SharedPreferences mSettings;


    PingPref(Context context) {
        mSettings = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void setData(String num, String url){
        SharedPreferences.Editor editor = mSettings.edit();
        String key = pre_ + num;
        editor.putString(key + _data, url);
        editor.commit();
    }

    public String getData(String num) {
        String key = pre_ + num;
        String url = mSettings.getString(key + _data, "");
        return url;
    }
}
public class GuapTimeRasp extends AppWidgetProvider {
    final public static String GUAP_TIME_RASP = "com.fluttershy.guaptimerasp.GUAP_TIME_RASP";


    final public static String dataSep=";;;";
    static boolean seeSec=false;
    String respurl="";

    Base db;
    RaspParse RP;
    String gname="";
    Context context=null;
    PingPref pingPref=null;
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        LOG("Act on Update " + appWidgetIds.length);
        if (appWidgetIds.length>0) {
            DrawWidgets(context, appWidgetManager, appWidgetIds);
        }
        repeat(context,true);
        repeat(context,false);
    }
    static public void repeat(Context context, boolean now){
        LOG("Start alarm sec:" + seeSec);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(GUAP_TIME_RASP), PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        if (!now){
            c.set(Calendar.SECOND, seeSec?1:0);
            c.set(Calendar.MILLISECOND, 0);
            c.add(Calendar.MINUTE, seeSec?0:1);
        }
        //System.currentTimeMillis()
        long updateFreq = 1000*(seeSec?1:60);
        alarmManager.cancel(PendingIntent.getBroadcast(context, 0, new Intent(GUAP_TIME_RASP), 0));
        alarmManager.setRepeating(AlarmManager.RTC, c.getTimeInMillis(), updateFreq, pendingIntent);


    }
    public void repeatStop(Context context){
        if (pingPref==null) pingPref = new PingPref(context);
        pingPref.setData("rep", "false");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(GUAP_TIME_RASP), 0);
        alarmManager.cancel(pendingIntent);
        LOG("Stop alarm");
    }
    public void DrawWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
        //LOG("Act on Draw " + appWidgetIds.length);
        final int N = appWidgetIds.length;
        if (N<=0) {
            repeatStop(context);
            return;
        }
        for (int appWidgetId : appWidgetIds) {
            DrawWidget(context, appWidgetManager, appWidgetId);
        }
    }
    public void DrawWidget(Context context, AppWidgetManager appWidgetManager,int appWidgetId){
        LOG("Update Vid");
        String data;
        String[] datam;
        if (pingPref==null) pingPref = new PingPref(context);
        gname = GuapTimeRasp4x1ConfigureActivity.loadTitlePref(context, appWidgetId);
        if (gname.equals("")) {
            LOG("No name");
            //callLater(context);
            return;
        }
        data = pingPref.getData(gname);
        if (data.equals("")){
            data = updateData(context,gname);
        }
        datam = data.split(dataSep);
        long now = new Date().getTime();
        if (datam[0].equals("")) datam[0]="0";
        if (now>=Long.parseLong(datam[0])||datam.length<5){
            data = updateData(context,gname);
            datam = data.split(dataSep);
        }
        if (data.equals("")) {
            LOG("Данные не загружены для "+gname);
            if (db==null) db = new Base(context);
            if (RP==null) RP = new RaspParse(context,db);
            respurl = "http://rasp.guap.ru/?"+RP.getRaspId(gname);
            this.context = context;
            new ProgressTask().execute();
            return;
        }
        String dl = GuapTime.getTimerDl((Long.parseLong(datam[0]) - now)/1000,seeSec);

        RemoteViews views = new RemoteViews(context.getPackageName(), getLayout());
        views.setTextViewText(R.id.time_guap_rasp4x1_timer, dl);
        views.setTextViewText(R.id.time_guap_rasp4x1_info,  datam[1]);
        views.setTextViewText(R.id.time_guap_rasp4x1_date,  datam[2]);
        views.setTextViewText(R.id.time_guap_rasp4x1_info2, datam[3]);
        views.setTextViewText(R.id.time_guap_rasp4x1_end, datam[4]);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("gname", gname);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent,0);

        views.setOnClickPendingIntent(R.id.main_layout, pendingIntent);


        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
        if (N<=0) {
            repeatStop(context);
            return;
        }
        for (int appWidgetId : appWidgetIds) {
            GuapTimeRasp4x1ConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent){
        final String action = intent.getAction();
        //LOG("RECEIVE "+action);
        if (GUAP_TIME_RASP.equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, getClassName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            DrawWidgets(context, appWidgetManager, appWidgetIds);
        }
        super.onReceive(context, intent);

    }
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,int appWidgetId) {

    }
    static public String updateData(Context context, String gname){

        //LOG("Update info "+gname);
        PingPref pingPref;
        Base db;
        GuapTime GT;
        RaspParse RP;
        GuapTimeBar GTB;

        db = new Base(context);
        GT = new GuapTime(context, db, 0);
        pingPref = new PingPref(context);

        String data = "";

        if (!gname.equals("")) {
            RP = new RaspParse(context, db);
            Cursor c = db.findRaspByName(gname);
            if (c.getCount() <= 0) {
                GuapTimeRasp.LOG("No data for " + gname);
                return "";
            }
            c.moveToFirst();
            String rasp = c.getString(c.getColumnIndex("rasp"));
            RP.getRaspData(rasp);
            GTB = GT.getNearLess(RP.list,0);
            data =  GTB.dateWait + dataSep +
                    GTB.getInfo() + dataSep +
                    GTB.getDateStr() + dataSep +
                    GTB.getInfo2() + dataSep +
                    GTB.getEndStr();
            pingPref.setData(gname,data);
        }

        return data;
    }

    class ProgressTask extends AsyncTask<String, Void, String> {
        private String name;
        @Override
        protected String doInBackground(String... path) {
            this.name = gname;
            LOG("Loading data for "+name);
            String content;
            try{
                content = getContent(respurl);
            }
            catch (IOException ex){
                content = ex.getMessage();
            }

            return content;
        }
        @Override
        protected void onProgressUpdate(Void... items) {
        }
        @Override
        protected void onPostExecute(String content) {
            content = RP.getBody(content);
            if (content.equals("")) {
                LOG("Ошибка загрузки");
                return;
            }
            Long time1 = new Date().getTime();
            db.sdb.delete("rasp", "gname = ?", new String[]{this.name});
            ContentValues values = new ContentValues();
            values.put("gname", this.name);
            values.put("rasp", content);
            values.put("load", time1);
            db.sdb.insert("rasp", null, values);
            LOG("Сохранены данные");
            repeat(context,true);
            repeat(context,false);
            //onReceive(context,new Intent(GUAP_TIME_RASP));
        }

        private String getContent(String path) throws IOException {
            BufferedReader reader=null;
            try {
                URL url=new URL(path);
                HttpURLConnection c=(HttpURLConnection)url.openConnection();
                c.setRequestMethod("GET");
                c.setReadTimeout(10000);
                c.connect();
                reader= new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder buf=new StringBuilder();
                String line;
                while ((line=reader.readLine()) != null) {
                    buf.append(line).append("\n");
                }
                return(buf.toString());
            }
            finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
    }

    static public void LOG(String s){
        System.out.println("LOG:3 " + s);
    }
    static Class getClassName() {return GuapTimeRasp4x1.class;}
    static int getLayout(){return R.layout.guap_time_rasp4x1;}

    public void onEnabled(Context context) {}
    public void onDisabled(Context context) {}

}
