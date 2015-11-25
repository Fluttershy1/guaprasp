package com.fluttershy.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;

public class Settings extends AppCompatActivity {

    static final int[] CheckIds = {
            R.id.settingsReversUp,
            R.id.settingsShowGroup,
            R.id.settingsShowTeacher,
            R.id.settingsShowPlace,
            R.id.settingsShowAutoMid,
            R.id.settingsAutoOpen
    };
    static final String[] CheckName = {
            Base.ReversUp,
            Base.ShowGroup,
            Base.ShowTeacher,
            Base.ShowPlace,
            Base.ShowAutoMid,
            Base.AutoOpen
    };
    Base b;
    CheckBox cb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        b = new Base(this);
        updateSeeInfo();
    }

    public void updateSeeInfo(){
        CheckBox cb;
        String temp;
        for (int i=0;i<CheckIds.length;i++){
            cb = (CheckBox) findViewById(CheckIds[i]);
            temp = b.SettingGet(CheckName[i]);
            //System.out.println("MyTempIs "+CheckName[i]+" = "+temp+ " "+(temp.equals("0")));
            cb.setChecked(b.isTrue(temp));
        }
    }
    public void clickCheckbox(View v){
        String name=null,value=null;
        int id = v.getId();
        for(int i = 0; i < CheckIds.length; i++){
            if (id==CheckIds[i]){
                name = CheckName[i];
                value = CheckCheckBox(CheckIds[i])?"1":"0";
                break;
            }
        }
        if (name!=null)
        {
            b.SettingSet(name,value);

        }
        updateSeeInfo();
    }
    public boolean CheckCheckBox(int id){
        cb = (CheckBox) findViewById(id);
        return cb.isChecked();
    }

}
