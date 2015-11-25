package com.fluttershy.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by admin on 14.11.2015.
 */
public class Base extends SQLiteOpenHelper {

    static final String ReversUp = "ReversUp",
            ShowGroup = "ShowGroup",
            ShowTeacher = "ShowTeacher",
            ShowPlace = "ShowPlace",
            AutoOpen = "AutoOpen",
            ShowAutoMid = "ShowAutoMid";

    static final String DATABASE_NAME = "mydatabase.db";
    static final int DATABASE_VERSION = 32;
    static final String DATABASE_TABLE = "rasp";
    static final String DATABASE_TABLE2 = "listd";
    static final String DATABASE_TABLE3 = "settings";
    private final Context context;

    SQLiteDatabase sdb=null;

    @Override
    public void onCreate(SQLiteDatabase db) {

        sdb = db;
        sdb.execSQL("CREATE TABLE IF NOT EXISTS `" + DATABASE_TABLE2 + "` (\n" +
                "\t`_id` integer primary key autoincrement,\n" +
                "\t`list` TEXT '',\n" +
                "\t`load` INT(11) NULL DEFAULT ''\n" +
                ");");
        sdb.execSQL("CREATE TABLE IF NOT EXISTS `" + DATABASE_TABLE + "` (\n" +
                "\t`_id` integer primary key autoincrement,\n" +
                "\t`gname` VARCHAR(128) NULL DEFAULT NULL,\n" +
                "\t`rasp` TEXT '',\n" +
                "\t`star` INT(11) NULL DEFAULT '0',\n" +
                "\t`see` INT(11) NULL DEFAULT '0',\n" +
                "\t`load` INT(11) NULL DEFAULT '0'\n" +
            ");");
        sdb.execSQL("CREATE TABLE IF NOT EXISTS `" + DATABASE_TABLE3 + "` (\n" +
                "\t`name` VARCHAR(50) NOT NULL,\n" +
                "\t`value` TEXT NULL\n" +
                ")");
        setDefSettings();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        sdb = db;
        sdb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
        sdb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE2);
        sdb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE3);
        onCreate(sdb);
    }

    public void chConnect(){
        if (sdb == null || !sdb.isOpen()) {
            sdb = this.getWritableDatabase();
        }
        //System.out.println("MyNewDb "+sdb);
    }

    public Cursor findRaspByName(String name)
    {
        chConnect();
        Cursor cursor = sdb.query("rasp", new String[] {"gname","rasp","star","see","load"},
            "gname = ?", new String[] {name},
            null, null, null) ;
        return cursor;
    }


    public void SettingSet(String name, String value){
        chConnect();
        if (SettingGet(name)!=null){
            ContentValues values = new ContentValues();
            values.put("value", value);
            sdb.update(DATABASE_TABLE3, values, "name = ?", new String[]{name});
            return;
        }
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("value", value);
        sdb.insert(DATABASE_TABLE3, null, values);

    }
    public String SettingGet(String name){
        chConnect();
        String ret=null;
        //System.out.println("MyDb " + sdb);
        Cursor c = sdb.query(DATABASE_TABLE3,
                new String[]{"name", "value"}, "name = ?",
                new String[]{name}, null, null, null);
        if (c.getCount()>0)
        {
            c.moveToFirst();
            ret = c.getString(c.getColumnIndex("value"));
        }
        c.close();
        return ret;
    }
    public boolean isTrue(String s){
        boolean ret = false;
        //if (s==null) return ret;
        if (!s.equals("0")) ret = true;
        return  ret;
    }

    public void setDefSettings(){
        SettingSet(ReversUp,"0");
        SettingSet(ShowGroup,"1");
        SettingSet(ShowTeacher,"1");
        SettingSet(ShowPlace,"1");
        SettingSet(AutoOpen, "1");
        SettingSet(ShowAutoMid, "0");
    }

    Base(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        chConnect();
    }
}
