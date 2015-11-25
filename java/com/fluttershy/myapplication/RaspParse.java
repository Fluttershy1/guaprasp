package com.fluttershy.myapplication;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ListMas{
    ArrayList<ListMasItem> list = new ArrayList<ListMasItem>();
    public void addItem(String gname, String gid, String gtype){
        list.add(new ListMasItem(gname,gid,gtype));
    }
}
class ListMasItem{
    String gname, gid, gtype;
    public ListMasItem(String gname, String gid, String gtype){
        this.gname = gname;
        this.gid = gid;
        this.gtype = gtype;
    }
}
class DayData{
    String client,title,para,time;
    int dayn = -1, number=0;
    public DayData(){
        client = title = para = time = "";
        dayn = -1;
    }
    public DayData clone(){
        DayData d = new DayData();
        d.client = client;
        d.title = title;
        d.para = para;
        d.time = time;
        d.dayn = dayn;
        d.number = number;
        return d;
    }
}
class LessData{
    String type,less,name,build,teacher,group;
    DayData dayData;
    public LessData(DayData day)
    {
        type = less = name = build = teacher = group = "";
        dayData = day;
    }
}
public class RaspParse  extends AppCompatActivity {
    String result="";
    Context t;
    ArrayList <LessData> list = null;
    ArrayList <ListMas> listM = null;
    Elements es1;
    Document html;
    Base db;
    static String symbDown="▼",symbUp="▲";
    static String[] typeS = {"g","p","b","r"};

    public RaspParse(Context tt,Base db){
        t = tt;
        this.db = db;
    }
    public String getList(String data)
    {

        html = Jsoup.parse(data);
        es1 = html.select("select");
        result="";
        int im = es1.size();
        for (int i=0;i<im;i++)
        {
            result += es1.get(i).outerHtml()+"\r\n";
        }
        return result;
    }
    public String getBody(String data)
    {
        html = Jsoup.parse(data);
        es1 = html.select(".result");
        result = "";
        if (es1.size()>0)
        {
            result = es1.get(0).outerHtml();
        }
        return result;
    }
    public void getRaspData(String data)
    {
        Matcher m, m0 ,m1;
        //<h\d>.*?<\/h\d>|<div[^>]*>.*?<\/div><\/div>
        Pattern p1 = Pattern.compile("<h\\d>(.*?)<\\/h\\d>|<div class=\"study\">([^\\\0]*?<div>[^\\\0]*?<\\/div>[^\\\0]*?)<\\/div>"),
                p2 = Pattern.compile("<em>\\s*\\W\\s*([^<]*)"),
                p3 = Pattern.compile("<b>([^<]*)"),
                p4 = Pattern.compile("<a[^>]*>^[^-]*([^\\\0]]*?)<\\/a>"),
                p5 = Pattern.compile("<span[^>]*>([^\\\0]*?)<\\/span>"),
                p6 = Pattern.compile("<a[^>]*>([^\\\0]]*?)<\\/a>");
        list = new ArrayList<LessData>();
        getTime("st Parse ");
        m0 = p1.matcher(data);
        getTime("ed Parse");
        String s1,s2,e0,e1,e2;
        DayData tempDay = new DayData();
        LessData tempLess = new LessData(null);
        for (int i=0;m0.find();i++)
        {

            e0 = m0.group(0);
            e1 = m0.group(1);
           if (e1!=null)
            {
                //getTime("find ddat a"+ e1);
                if (e0.contains("h2")){
                    tempDay.client = e1;
                }
                //День недели
                else if (e0.contains("h3")){
                    tempDay.title = e1;
                    tempDay.dayn = getDayByName(tempDay.title);
                }
                //Номер пары
                else if (e0.contains("h4")){
                    tempDay.para = e1;
                    tempDay.time = e1.replaceAll("^.*?\\(|\\).*?$","");
                    s2 = e1.replaceAll("\\D*?\\s.*", "");
                    tempDay.number = Integer.valueOf(s2.equals("")?"0":s2);
                }
            }
            else
            {
                e1 = m0.group(2);
                tempLess = new LessData(tempDay.clone());
                m1 = p5.matcher(e1);
                while(m1.find()) {
                    e2 = m1.group();
                    s1 = delTag(e2);
                    //getTime("less d "+e2);
                    if (e2.contains("preps"))//преподаватель
                    {
                       tempLess.teacher = s1.replaceAll("^.*?:\\s*?|\\s*\\-\\s*.*?$","");
                    } else if (e2.contains("groups"))//Группы
                    {
                        tempLess.group = s1.replaceAll("^.*?:\\s*?","");

                    }
                    else {//Содержимое пары
                        {
                            s2="";
                            if(e2.indexOf('▲')>=0) s2=symbUp;
                            if(e2.indexOf('▼')>=0) s2=symbDown;
                            tempLess.type = s2;
                        }
                        tempLess.name = e2.replaceAll("<(em|b)[^>]*?>.*?<\\/(em|b)>|<span[^>]*?>|<\\/span>", "").replaceAll("^\\s*\\W\\s*|\\s*$", "");
                        {
                            m = p2.matcher(e1);
                            tempLess.build = "";
                            if (m.find()){
                                tempLess.build = m.group(1);
                            }
                        }
                        {
                            m = p3.matcher(e1);
                            tempLess.less = "";
                            if (m.find()){
                                tempLess.less = m.group(1);
                            }
                        }

                    }
                }
                list.add(tempLess);
            }
        }
    }
    public String delTag(String s){return  s.replaceAll("<[^>]*>","");}
    public String getRaspId(String gname){
        ListMas lm;
        ListMasItem lmi;
        String gtype="",gid="";
        if (listM==null) getListMas();

        int c1, c2;
        c1 = listM.size();
        for (int i1 = 0; i1 < c1 ; i1++){
            lm = listM.get(i1);
            c2 = lm.list.size();
            for (int i2 = 0; i2 < c2 ; i2++){
                lmi = lm.list.get(i2);
                if (lmi.gname.equals(gname)){
                    gtype = lmi.gtype;
                    gid = lmi.gid;
                }
            }
        }
        System.out.println("LOG: "+gname+" "+gid+" "+gtype);
        return typeS[Integer.parseInt(gtype)]+"="+gid;
    }

    public void getListMas(){

        if (listM!=null) listM.clear();
        listM = new ArrayList<ListMas>();
        String contentText;
        Cursor cursor = db.sdb.query("listd", new String[]{"list", "load"}, null, null, null, null, null) ;

        if (cursor.getCount()<=0) return;

        cursor.moveToFirst();
        contentText = cursor.getString(cursor.getColumnIndex("list"));

        Pattern p1 = Pattern.compile("<select[^>]*>[^\0]*?<\\/select[^>]*>");    //Список
        Pattern p2 = Pattern.compile("<option[^>]*>[^\0]*?<\\/option[^>]*>");    //Элемент списка
        Pattern p3 = Pattern.compile("(?:value=\\\")\\d*[^=\\\"]");              //id
        Matcher m1 = p1.matcher(contentText), m2 ,m3;
        String select, option, temp;
        ListMas lm;
        int i=0;
        while(m1.find()){
            lm = new ListMas();
            select = m1.group();
            m2 = p2.matcher(select);
            while(m2.find()) {
                option = m2.group();
                if (!option.replaceAll("<[^>]*>","").equals("- нет -"))
                {
                    m3 = p3.matcher(option);
                    m3.find();
                    lm.addItem(
                        option.replaceAll("<[^>]*>", ""),
                        m3.group().replaceAll("value=\"", ""),
                        i+""
                    );
                }

            }
            listM.add(lm);
            i++;
        }
    }

    public long maintime = 0;
    public void getTime(String s){
        if (false) {
            long d = new Date().getTime();
            if (maintime == 0) maintime = d;
            System.out.println("TIME:2 " + ((d - maintime)) + "ms - " + s);
            maintime = d;
        }
    }


    public int getDayByName(String s){
        String[] names = new String[]{"Воскресенье","Понедельник","Вторник","Среда","Четверг","Пятница","Суббота"};
        for (int i=0; i<names.length; i++){
            if (s.equals(names[i])) return i;
        }
        return -1;
    }



}
