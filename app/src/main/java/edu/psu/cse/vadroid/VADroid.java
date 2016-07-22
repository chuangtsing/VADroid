package edu.psu.cse.vadroid;

import android.app.Application;
import android.content.Context;


public class VADroid extends Application {
    private static VADroid instance;

    public static VADroid getInstance() {
        return instance;
    }

    public static Context getContext(){
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}