package com.fluttershy.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    String contentText = null;
    String groupmasurl = "http://rasp.guap.ru/";
    RaspParse raspParse;
    Handler handler1;
    GuapTime GT;

    MyList[] myList = new MyList[6];

    Base db;
    SQLiteDatabase sdb;
    RaspParse rp;

    AdapterView.OnItemClickListener myFunc;
    private ArrayList<SwipeRefreshLayout> mSwipeRefreshLayout = new ArrayList<SwipeRefreshLayout>();

    TabHost tabs;

    int tooOld = 1000*60*60*24*2, isShow = 0;//2 суток
    boolean ListsSet = false, canClick = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getTime("Создание главной страницы");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        db = new Base(this);
        sdb = db.sdb;

        if (checkAutoOpen()) isShow=-1;
        getTime("Конец создание главной страницы");
    }

    public void startRefresher(int id){
        SwipeRefreshLayout lw= (SwipeRefreshLayout) findViewById(id);
        lw.setOnRefreshListener(this);
        lw.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN);
        lw.setProgressBackgroundColorSchemeColor(Color.rgb(230, 230, 230));
        mSwipeRefreshLayout.add(lw);

    }
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            //bloop(isNetworkConnected(this) ? "Интернет есть" : "Интернета нет");
            startActivity(new Intent(getApplicationContext(), Settings.class));
            return true;
        }
        else if (id == R.id.action_clearbase)
        {
            db.onUpgrade(sdb, 0, 1);
            db.onCreate(sdb);
            getLists(false);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume(){
        getTime("Resume main "+isShow);
        super.onResume();
        //Если запуск после построения всего
        if (isShow==1){
            showSavedList(false);
        }
        //Если первичные настройки
        else if (isShow==0){
            rp = new RaspParse(this,db);
            tabsInit();
            getLists(false);
            startRefresher(R.id.swipe_container1);
            startRefresher(R.id.swipe_container2);
            startRefresher(R.id.swipe_container3);
            startRefresher(R.id.swipe_container4);
            startRefresher(R.id.swipe_container5);
            startTimer();
            isShow++;
        }
        //Если переход в другое активити
        else if(isShow==-1){
            isShow++;
        }
    }
    public void startTimer(){
        GT = new GuapTime(this,db,0);
        handler1 = new Handler() {
            public void handleMessage(Message msg) {
                setTitle((String) msg.obj);
            }
        };
        new Thread() {
            public void run(){
                while (true){
                    Message msg = new Message();
                    msg.obj = GT.PareTime(0);
                    handler1.sendMessage(msg);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
    public boolean checkAutoOpen(){
        String gname = getIntent().getStringExtra("gname");;
        getTime("Начало проверки перехода "+gname);
        if (db.isTrue(db.SettingGet(Base.AutoOpen))||gname!=null){
            if (gname==null) {
                Cursor c = sdb.query("rasp", new String[]{"gname"}, null, null, null, null, "see desc", "1");
                if (c.getCount()<=0) return false;
                c.moveToFirst();
                gname = c.getString(c.getColumnIndex("gname"));
            }

            Intent intent = new Intent(getApplicationContext(),SeeRasp.class);
            intent.putExtra("gname", gname);
            getTime("Переход на просмотр");
            startActivity(intent);
            return true;
        }
        return  false;
    }

    public void getLists(boolean refr){
        canClick = false;
        System.out.println("LOG: start getList");
        ListsSet = true;
        Cursor cursor = sdb.query("listd", new String[]{"list", "load"}, null, null, null, null, null) ;

        if (cursor.getCount()>0&&!refr)
        {
            cursor.moveToFirst();
            contentText = cursor.getString(cursor.getColumnIndex("list"));
            System.out.println("Загружены данные списков");
            showList();
        } else
        {
            loadData();
        }
    }
    public void loadData(){
        if (isNetworkConnected(this))
        {
            bloop("Загрузка...");
            getTime("Начало загрузки");
            new ProgressTask().execute();
        }
        else
        {
            bloop("Нет связи с интернетом");
        }
    }
    public void showList(){
        myFunc = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!canClick) {
                    bloop("Wait");
                    return;
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findViewById(R.id.editText).getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

                MyList ml = search(parent.getId());
                Intent intent = new Intent(getApplicationContext(),SeeRasp.class);
                intent.putExtra("gname",ml.rowsreal.get(position));
                startActivity(intent);
            }
        };


        showSavedList(true);
        showAllList();
        SrchListInit();
        canClick = true;
    }

    public void SrchListInit() {
        TextWatcher inputTW = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                SrchList(s.toString());
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){

            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        int i=5;
        ((EditText) findViewById(R.id.editText)).addTextChangedListener(inputTW);
        myList[i] = new MyList(this,R.id.listView6, i);
        myList[i].listView = (ListView) findViewById(myList[i].list_id);
        myList[i].listView.setAdapter(myList[i].adapter);
        myList[i].listView.setOnItemClickListener(myFunc);
    }
    public void SrchList(String s){
        //bloop(s);
        int i = 5;
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
        getTime("Начало построения списков");
        int ids[] = {R.id.listView1,R.id.listView2,R.id.listView3,R.id.listView4};

        ListMas lm;
        ListMasItem lmi;
        if (rp.listM==null) rp.getListMas();

        int c1, c2;
        c1 = rp.listM.size();
        for (int i = 0; i < c1 ; i++){
            myList[i] = new MyList(this,ids[i], i);
            myList[i].listView = (ListView) findViewById(myList[i].list_id);
            myList[i].listView.setAdapter(myList[i].adapter);
            myList[i].listView.setOnItemClickListener(myFunc);

            lm = rp.listM.get(i);
            c2 = lm.list.size();
            for (int i2 = 0; i2 < c2 ; i2++){
                lmi = lm.list.get(i2);

                myList[i].ids.      add(lmi.gid);
                myList[i].rows.     add(lmi.gname);
                myList[i].rowsreal. add(lmi.gname);
            }
            myList[i].adapter.notifyDataSetChanged();
        }
        getTime("Конец построения списков");
    }
    public void showSavedList(boolean chTab){
        getTime("Начало построения сохранённых");
        int i=4;

        myList[i] = new MyList(this,R.id.listView5, i);
        myList[i].listView = (ListView) findViewById(myList[i].list_id);
        myList[i].listView.setAdapter(myList[i].adapter);
        myList[i].listView.setOnItemClickListener(myFunc);
        Cursor c = sdb.query("rasp", new String[]{"gname", "star"}, null, null, null, null, "star desc,see desc");
        //System.out.println("LOG: Start show savel list");
        int count = c.getCount();
        if (count>0)
        {
            int star;
            String gname;
            for (int q=0;q<count;q++){
                //System.out.println("LOG: new item");
                c.moveToPosition(q);
                star = c.getInt(c.getColumnIndex("star"));
                gname = c.getString(c.getColumnIndex("gname"));
                myList[i].ids.add("");
                myList[i].rows.add((star==1?"✰ ":"")+gname);
                myList[i].rowsreal.add(gname);
            }
            if (chTab)
            {
                tabs.setCurrentTab(5);
                /*if (db.isTrue(db.SettingGet(Base.AutoOpen)))
                    myList[i].listView.performItemClick(myList[i].listView.getAdapter().
                        getView(0, null, null),0,myList[i].listView.getAdapter().
                        getItemId(0));*/
            }
        }
        getTime("Конец построения сохранённых");
    }

    public void addText(String s)
    {
        TextView label;
        label = new TextView(this);
        label.setText(s);
        label.setTextSize(14);
        //myLay.addView(label);
    }

    public void bloop(String t)
    {
        Toast toast = Toast.makeText(this,t, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void dataisloaded()
    {
        getTime("Конец загрузки");
        raspParse = new RaspParse(this,db);
        contentText = raspParse.getList(contentText);

        if (contentText.equals("")){
            bloop("Ошибка загрузки");
            return;
        }
        sdb.delete("listd",null,null);

        Long time1 = new Date().getTime();
        ContentValues values = new ContentValues();
        values.put("list", contentText);
        values.put("load", time1);
        sdb.insert("listd", null, values);
        System.out.println("Сохранены данные списков");
        getLists(false);

    }

    public MyList search(int id)
    {
        int i=0, r=0;
        for (;i<myList.length;i++) {
            if (myList[i]!=null) if (myList[i].list_id==id) {
                r=i;
                break;
            }
        }
        return myList[r];
    }

    public void tabsInit()
    {
        int ids[] = {R.id.tab1,R.id.tab2,R.id.tab3,R.id.tab4,R.id.tab6,R.id.tab5};
        int ico[] = {R.mipmap.tab_ico_0,
                R.mipmap.tab_ico_1,
                R.mipmap.tab_ico_2,
                R.mipmap.tab_ico_3,
                R.mipmap.tab_ico_6,
                R.mipmap.tab_ico_4};
        String names[] = {"Группа","Учитель","Здание","Кабинет","Поиск","Сохр"};
        tabs = (TabHost) findViewById(R.id.tabhost);
        tabs.setup();
        TabHost.TabSpec spec;
        Resources res = getResources();
        for(int i = 0; i<6;i++) {

            spec = tabs.newTabSpec("tag"+i);
            spec.setIndicator("",res.getDrawable(ico[i]));
            spec.setContent(ids[i]);
            tabs.addTab(spec);
        }
        tabs.setCurrentTab(0);
    }

    public boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cm.getActiveNetworkInfo();
        return nInfo != null && nInfo.isConnected();

    }

    @Override
    public void onRefresh() {
        getTime("Обновление страницы");
        getLists(true);
        int c = mSwipeRefreshLayout.size();
        for (int i=0; i<c;i++ ) mSwipeRefreshLayout.get(i).setRefreshing(false);

    }

    class ProgressTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... path) {
            String content;
            try{ content = getContent(groupmasurl);}
            catch (IOException ex){content = ex.getMessage();}
            return content;
        }
        protected void onProgressUpdate(Void... items) {
        }
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
