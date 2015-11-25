package com.fluttershy.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class SeeRasp extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener  {

    String gname,respurl;
    String contentText=null;
    RaspParse raspParse;
    LinearLayout myLay;
    ScrollView myLayPar;
    ActionBar actionBar;
    GuapTimeBar actionBarObj;
    Base db;
    SQLiteDatabase sdb;
    GuapTime gt;
    Handler handler1;
    String userChangeMod=null;
    View actionBarV;
    int isShow = 0;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getTime("Создание страницы расписания");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_rasp);

        myLay = (LinearLayout) findViewById(R.id.MySeeLay);
        myLayPar = (ScrollView) findViewById(R.id.MySeeLayPar);


        db = new Base(this);
        sdb = db.sdb;
        gt = new GuapTime(this,db,0);
    }

    @Override
    public void onRefresh() {
        getRaspData(true);
        mSwipeRefreshLayout.setRefreshing(false);
    }
    public void onResume(){
        getTime("onResume seeRasp " + isShow);
        super.onResume();
        if (isShow==1) {
            getTime("Смена мода обновлению");
            ShowRasp(null);
            actionBarObj=null;
        }
        else if(isShow==0){
            raspParse = new RaspParse(this,db);
            handler1 = new Handler() {
                public void handleMessage(Message msg) {
                    GuapTimeBar gtb = (GuapTimeBar) msg.obj;
                    ((TextView) actionBarV.findViewById(R.id.time_guap_bar_timer)).setText(gtb.getDlStr());
                    ((TextView) actionBarV.findViewById(R.id.time_guap_bar_info)).setText(gtb.getInfo());
                    ((TextView) actionBarV.findViewById(R.id.time_guap_bar_date)).setText(gtb.getDateStr());
                    ((TextView) actionBarV.findViewById(R.id.time_guap_bar_info2)).setText(gtb.getInfo2());
                    ((TextView) actionBarV.findViewById(R.id.time_guap_bar_end)).setText(gtb.getEndStr());
                }
            };
            gname = getIntent().getStringExtra("gname");
            System.out.println("LOG: open rasp " + gname);
            startRefresher(R.id.swipe_container);
            startChooseRadio();
            getRaspData(false);
            actionBarInit();
            isShow++;
        }
    }
    private void actionBarInit(){
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setIcon(R.mipmap.tab_ico_6);

        LayoutInflater inflator = (LayoutInflater) this .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.time_guap_bar, null);

        actionBar.setCustomView(v);
        actionBarV = actionBar.getCustomView();

        new Thread() {
            public void run(){
                while (true){
                    //System.out.println("MyNewTh "+TimeView);
                    if (raspParse.list!=null)
                    {
                        Message msg = new Message();
                        if (actionBarObj==null)
                            actionBarObj = gt.getNearLess(raspParse.list,0);
                        if (actionBarObj.dl<=0)
                            actionBarObj = gt.getNearLess(raspParse.list,0);
                        msg.obj = actionBarObj;
                        handler1.sendMessage(msg);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    public void startRefresher(int id){
        mSwipeRefreshLayout= (SwipeRefreshLayout) findViewById(id);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.rgb(230, 230, 230));

    }
    public void startChooseRadio(){
        boolean isUp = gt.isUp(0);
        boolean isMid = db.isTrue(db.SettingGet(Base.ShowAutoMid));
        ((RadioButton) findViewById(isMid?R.id.SeeChooseBoth:
                        (isUp?R.id.SeeChooseUp:R.id.SeeChooseDown)
        )).setChecked(true);
        RadioGroup radiogroup = (RadioGroup) findViewById(R.id.chooseMod);
        radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                String mod = "";
                switch (checkedId) {
                    case R.id.SeeChooseUp:
                        mod = RaspParse.symbUp;
                        break;
                    case R.id.SeeChooseDown:
                        mod = RaspParse.symbDown;
                        break;
                }
                if (userChangeMod != null) if (userChangeMod.equals(mod)) return;
                userChangeMod = mod;
                getTime("Смена мода по радио");
                ShowRasp(mod);
            }
        });
    }

    public void dataisloaded() {
        //raspParse = new RaspParse(this,contentText);
        getTime("Конец загрузки");
        //Обрезаем входящие данные
        contentText = raspParse.getBody(contentText);
        //System.out.println("LOG: " + contentText);
        if (contentText.equals("")) {
            bloop("Ошибка загрузки");
            return;
        }
        Long time1 = new Date().getTime();
        sdb.delete("rasp","gname = ?",new String[]{gname});
        ContentValues values = new ContentValues();
        values.put("gname", gname);
        values.put("rasp", contentText);
        values.put("load", time1);
        sdb.insert("rasp", null, values);
        System.out.println("Сохранены данные" + gname);
        getRaspData(false);
    }

    public void getRaspData(boolean refr)
    {
        getTime("Начало проверки данных");
        Cursor cursor = db.findRaspByName(gname);
        if (cursor.getCount()>0&&!refr)
        {
            Long time1 = new Date().getTime();
            ContentValues values = new ContentValues();
            values.put("see", time1);
            sdb.update("rasp", values, "gname = ?", new String[]{gname});
            cursor.moveToFirst();
            contentText = cursor.getString(cursor.getColumnIndex("rasp"));
            cursor.close();
            //addText(contentText);
            getTime("Смена мода протоколу");
            ShowRasp(null);
            System.out.println("Загружены данные " + gname);
        }
        else
        {
            loadData();
        }
    }
    public void loadData(){
        if (isNetworkConnected(this))
        {
            bloop("Загрузка...");
            respurl = "http://rasp.guap.ru/?"+raspParse.getRaspId(gname);
            getTime("Начало загрузки");
            System.out.println("LOG: loading page "+respurl);
            new ProgressTask().execute();
        }
        else
        {
            bloop("Нет связи с интернетом");
        }
    }
    public boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cm.getActiveNetworkInfo();
        return nInfo != null && nInfo.isConnected();

    }
    //@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void ShowRasp(String mod)
    {

        //(new Date().getTime())-1000*60*60*24*2
        boolean isUp = gt.isUp(0);
        if (mod==null){
            if (userChangeMod==null) {
                boolean isMid = db.isTrue(db.SettingGet(Base.ShowAutoMid));
                if (isMid) mod = "";
                else mod = isUp?RaspParse.symbUp:RaspParse.symbDown;
            }
            else mod = userChangeMod;
        }
        findViewById(R.id.seeRaspLayout).setBackgroundResource(isUp ? R.color.weekUp : R.color.weekDown);
        myLay.removeAllViews();
        getTime("Начало построения массива данных");
        raspParse.getRaspData(contentText);
        getTime("Конец построения массива данных");
        if (false) {
            int min = 1000 * 60;
            int h = 1000 * 60 * 60;
            int day = 1000 * 60 * 60 * 24;
            int[] mas = new int[]{
                    -2 * day + 0 * h + 0 * min,
                    -1 * day + 0 * h + 0 * min,
                    -0 * day + 0 * h + 0 * min,
                    +1 * day + 0 * h + 0 * min,
            };
            for (int m : mas) {
                GuapTimeBar bar = gt.getNearLess(raspParse.list, (new Date().getTime()) + m);
                addText("v5 " + bar.getDlStr() + " " + bar.getEndStr() + " " + bar.less.name + " ", R.style.raspData, null);
            }
        }
        int count = raspParse.list.size();
        String client = null,title = null,para = null, temp="";
        LessData tempLess;
        View tempV;
        TextView tempT;
        LinearLayout lay;
        for (int i=0;i<count;i++) {
            tempLess = raspParse.list.get(i);
            if (tempLess.type.equals("")||tempLess.type.equals(mod)||mod.equals("")) {
                if (!tempLess.dayData.client.equals(client)) {
                    client = tempLess.dayData.client;
                    title = para = null;
                    addView(client,R.layout.rasp_h2,R.id.rasp_h2_text);
                }
                if (!tempLess.dayData.title.equals(title)) {
                    title = tempLess.dayData.title;
                    para = null;
                    addView(title,R.layout.rasp_h3,R.id.rasp_h3_text);
                }
                if (!tempLess.dayData.para.equals(para)) {
                    para = tempLess.dayData.para;
                    temp = tempLess.dayData.number+" пара ("+tempLess.dayData.time+")";
                    addView(tempLess.dayData.number>0?temp:tempLess.dayData.time,R.layout.rasp_h4,R.id.rasp_h4_text);
                }
                tempV = LayoutInflater.from(this).inflate(R.layout.rasp_less, null);
                lay = (LinearLayout) tempV.findViewById(R.id.rasp_less_lay);
                (tempT = (TextView) tempV.findViewById(R.id.rasp_less_up)).setText((!tempLess.type.equals("") ? tempLess.type + " " : " "));
                tempT.setTextColor(tempLess.type.equals(RaspParse.symbUp) ? Color.RED : Color.BLUE);
                ((TextView) tempV.findViewById(R.id.rasp_less_type)).setText((!tempLess.less.equals("") ? tempLess.less + " " : ""));
                ((TextView) tempV.findViewById(R.id.rasp_less_name)).setText(tempLess.name);

                if (!tempLess.teacher.equals("") && db.isTrue(db.SettingGet(Base.ShowTeacher)))
                    addText("Преподаватель: " + tempLess.teacher, R.style.raspData, lay);
                if (!tempLess.build.equals("") && db.isTrue(db.SettingGet(Base.ShowPlace)))
                    addText(tempLess.build, R.style.raspData, lay);
                if (!tempLess.group.equals("") && db.isTrue(db.SettingGet(Base.ShowGroup)))
                    addText("Группы: " + tempLess.group, R.style.raspData, lay);

                myLay.addView(tempV);
            }

        }
        getTime("Конец построения сетки расписания");


    }
    public String get_ok(int num, String z1, String z2,String z3) {
        num = num%100;

        if (num>=20) num = num%10;
        if (num == 0) return z1;
        if (num == 1) return z2;
        if (num>=2&&num<=4) return z3;
        if (num>=5) return z1;
        return z1;
    }
    public void addText(String s, int style, LinearLayout lay)
    {
        if (lay==null) lay = myLay;
        TextView label;
        label = new TextView(this,null);
        label.setText(s);
        label.setTextAppearance(this, style);
        lay.addView(label);
    }
    public void addView(String s, int id_lay, int id_text){

        View tempV = LayoutInflater.from(this).inflate(id_lay, null);
        ((TextView) tempV.findViewById(id_text)).setText(s);
        myLay.addView(tempV);
    }
    public void bloop(String t)
    {
        Toast toast = Toast.makeText(this, t, Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_see_rasp, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(getApplicationContext(), Settings.class));
            return true;
        }
        if (id == R.id.action_addstar) {
            Cursor cursor = db.findRaspByName(gname);
            if (cursor.getCount()>0) {
                cursor.moveToFirst();
                int b = cursor.getInt(cursor.getColumnIndex("star"));
                b = b!=0?0:1;
                ContentValues values = new ContentValues();
                values.put("star", b);
                sdb.update("rasp", values, "gname = ?", new String[]{gname});
                bloop(b==1?"Добавлено в избранное":"Удалено из избранного");
            }
            else{
                bloop("Ошибка");
            }
        }

        return super.onOptionsItemSelected(item);
    }

    class ProgressTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... path) {

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
            contentText = content;
            dataisloaded();
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
                String line=null;
                while ((line=reader.readLine()) != null) {
                    buf.append(line + "\n");
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


    public long maintime = 0;
    public void getTime(String s){
        long d = new Date().getTime();
        if (maintime==0) maintime = d;
        System.out.println("TIME: "+((d-maintime))+"ms - "+s);
    }
}
