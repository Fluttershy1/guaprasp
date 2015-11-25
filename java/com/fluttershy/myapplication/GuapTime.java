package com.fluttershy.myapplication;

import android.content.Context;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by admin on 16.11.2015.
 */
class GuapTimeBar{
    LessData less;
    long dateWait = 0, dl=1;
    int i;
    boolean seeSec = true;
    boolean end = false;
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm");
    public GuapTimeBar(LessData less, long date, int i, boolean end){
        this.less = less;
        this.dateWait = date;
        this.i = i;
        this.end = end;
        getDlStr();
    }
    public String getDlStr(){
        dl = dateWait-(new Date().getTime());
        return GuapTime.getTimerDl(dl/1000,seeSec);
    }
    public String getInfo(){
        return this.less.name;
    }

    public String getDateStr() {
        return dateFormat.format(new Date(dateWait));
    }
    public String getInfo2() {
        return this.less.build;
    }
    public String getEndStr() {
        return this.end?"до конца":"до начала";
    }
}
public class GuapTime {
    Context t;
    Date now;
    Base db;
    static String[] TimeName = new String[]{
            "До первой пары",
            "До конца 1 пары", "До 2 пары",
            "До конца 2 пары", "До 3 пары",
            "До конца 3 пары", "До 4 пары",
            "До конца 4 пары", "До 5 пары",
            "До конца 5 пары", "До 6 пары",
            "До конца 6 пары", "До 7 пары",
            "До конца 7 пары", "До 8 пары",
            "До конца 8 пары", "До первой пары"
    };
    static int min = 1000*60;
    /*
    1 пара (09:00-10:30)
    2 пара (10:40-12:10)
    3 пара (12:20-13:50)
    4 пара (14:10-15:40)
    5 пара (15:50-17:20)
    6 пара (17:30–19:00)
    7 пара (19:10–20:30)
    8 пара (20:40–22:00)*/
    static long[] TimeInt = new long[]{
            //Длительность пары, перерыв после пары
            0,
            90*min,10*min,//1
            90*min,10*min,//2
            90*min,20*min,//3
            90*min,10*min,//4
            90*min,10*min,//5
            90*min,10*min,//6
            80*min,10*min,//7
            80*min,min*60*11,//8
    };

    public GuapTime(Context context,Base b, long time){
        t = context;
        if (time==0) now = new Date();
        else now = new Date(time);
        this.db = b;
        //System.out.println("GuapTime "+now.toGMTString()+" : "+now.getDate()+"."+now.getMonth()+"."+now.getYear()+" - "+time);

    }
    public boolean isUp(long nn){
        Calendar cal = Calendar.getInstance(), cal1;
        if (nn!=0) cal.setTimeInMillis(nn);
        else cal.setTimeInMillis(now.getTime());
        boolean i1,i2,res;
        cal1 = Calendar.getInstance();
        cal1.clear();
        cal1.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal1.set(Calendar.MONTH, Calendar.SEPTEMBER);
        cal1.set(Calendar.DAY_OF_MONTH, 1);
        i1 = cal1.get(Calendar.WEEK_OF_YEAR)%2>0;

        i2 = cal.get(Calendar.WEEK_OF_YEAR)%2>0;

        res = (!i1&&!i2)||(i1&&i2);
        String revers = db.SettingGet(Base.ReversUp);
        if (cal.get(Calendar.DAY_OF_WEEK)==Calendar.SUNDAY) res = !res;
        if (revers.equals("1")) res = !res;
        return res;
    }

    public String PareTime(long time){
        Date NDate, PDate;
        long Ntime, Dtime, dl=0;

        if (time==0) NDate = new Date();
        else NDate = new Date(time);

        PDate = new Date(NDate.getYear(),NDate.getMonth(),NDate.getDate(),9,0); //9 часов этого дня
        Ntime = NDate.getTime();
        Dtime = PDate.getTime();
        int para=0;
        String out="";
        //System.out.println("TIMEMY: " + NDate.toGMTString() + " " + PDate.toGMTString());
        for (int i=0;i<TimeInt.length;i++){
            Dtime += TimeInt[i];
            if (Dtime>Ntime){
                para = i;
                out = TimeName[i];
                dl = (Dtime-Ntime)/1000;
                break;
            }
        }
        out = getTimerDl(dl, true)+" "+out;
        //System.out.println("TIMEMY: "+Ntime+" "+Dtime+" "+dl+" "+out);
        return out;

    }
    static public String getTimerDl(long dl,boolean seeSec){
        if (seeSec==false) {
            dl+=30;
        }
        String out;
        int h = (int)dl/3600;
        dl -= h*3600;
        int m = (int)dl/60;
        dl -= m*60;
        int s = (int)dl;
        NumberFormat nf2 = new DecimalFormat("00");
        out = nf2.format(h) +":"+nf2.format(m)+(seeSec?":"+nf2.format(s):"");
        return  out;
    }
    public int getLessTimeStart(int c){
        int ret=1000*60*60*9;
        if (c>8) return ret;
        for (int i=0; i<TimeInt.length&&i<=((c-1)*2); i++){
            ret += TimeInt[i];
        }
        return ret;
    }

    public GuapTimeBar getNearLess(ArrayList<LessData> list, long time){
        Calendar calNow = Calendar.getInstance(), calTemp, calAdd;
        if (time!=0) calNow.setTime(new Date(time));
        calAdd = Calendar.getInstance();

        calTemp = Calendar.getInstance();
        calTemp.clear();
        calTemp.set(Calendar.YEAR, calNow.get(Calendar.YEAR));
        calTemp.set(Calendar.MONTH, calNow.get(Calendar.MONTH));
        calTemp.set(Calendar.DAY_OF_MONTH, calNow.get(Calendar.DAY_OF_MONTH));
        calTemp.add(Calendar.DAY_OF_MONTH, -(calTemp.get(Calendar.DAY_OF_WEEK) - 1));

        //System.out.println("LOG:2 inCal " + getCalTime(calNow));
        //System.out.println("LOG:2 tpCal "+getCalTime(calTemp));

        LessData tempLess;
        boolean Up = this.isUp(calNow.getTimeInMillis()), br = false, end = false;
        //System.out.println("LOG:2 up "+Up);
        String[] Uplist = Up ? new String[]{RaspParse.symbUp,RaspParse.symbDown}:
                new String[]{RaspParse.symbDown,RaspParse.symbUp};
        int dayN = calNow.get(Calendar.DAY_OF_WEEK)-1,//0 - вск
                listSize = list.size()
                        ;
        long endTime = 0;
        String UplistS="";
        int ret = 0;
        for(int m = 0; m<Uplist.length;  m++){
            if (br) break;
            UplistS = Uplist[m];
            for (int i = 0; i < listSize ; i++){
                if (br) break;
                tempLess = list.get(i);

                if (tempLess.dayData.dayn>0&&
                        (tempLess.type.equals(UplistS)||tempLess.type.equals(""))&&
                        !(m==0&&tempLess.dayData.dayn<dayN)
                        ){

                    calAdd.clear();
                    calAdd = (Calendar) calTemp.clone();
                    calAdd.add(Calendar.DAY_OF_YEAR,tempLess.dayData.dayn);
                    calAdd.add(Calendar.MILLISECOND, this.getLessTimeStart(tempLess.dayData.number));
                    //System.out.println("LOG:2 "+getCalTime(calAdd)+" "+tempLess.dayData.dayn+"/"+tempLess.dayData.number+" "+tempLess.name);

                    if (calAdd.after(calNow)) {
                        ret = i;
                        br = true;
                        break;
                    }
                    else if (calAdd.getTimeInMillis()+TimeInt[2*tempLess.dayData.number-1]>calNow.getTimeInMillis()){
                        ret = i;
                        br = true;
                        end = true;
                        endTime = TimeInt[2*tempLess.dayData.number-1];
                        break;
                    }

                }
            }
            calTemp.add(Calendar.DAY_OF_YEAR, 7);//+неделя
            //System.out.println("LOG: new Start " + getCalTime(calTemp));
        }

        System.out.println("LOG: near "+ret+" "+getCalTime(calNow)+" less is "+list.get(ret).name);
        return new GuapTimeBar(list.get(ret),calAdd.getTimeInMillis()+(end?endTime:0),ret,end);
    }

    public String getCalTime(Calendar c){
        return c.get(Calendar.DAY_OF_MONTH)+"."+c.get(Calendar.MONTH)+"."+c.get(Calendar.YEAR)+" "
                +c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND);
    }

}
