package com.treynix.helloworld;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;


public class HelloWorldApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

}
