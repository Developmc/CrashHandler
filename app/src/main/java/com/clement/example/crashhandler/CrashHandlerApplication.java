package com.clement.example.crashhandler;

import android.app.Application;

/**
 * Created by clement on 16/7/29.
 */

public class CrashHandlerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(this,true);
    }
}
