package com.fluttershy.myapplication;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

class MyList extends AppCompatActivity
{
    int list_id, group_id;
    public final ArrayList<String> rows = new ArrayList<String>();
    public final ArrayList<String> rowsreal = new ArrayList<String>();
    public final ArrayList<String> ids = new ArrayList<String>();
    public ArrayAdapter<String> adapter;
    public ListView listView;

    public MyList(Context t, int i, int g)
    {
        list_id = i;
        group_id = g;
        adapter = new ArrayAdapter<String>(t, android.R.layout.simple_list_item_1, this.rows);
    }
    public void clear(){
        rows.clear();
        rowsreal.clear();
        ids.clear();
    }
}