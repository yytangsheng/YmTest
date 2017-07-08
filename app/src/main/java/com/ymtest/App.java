package com.ymtest;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;


/**
 * Created by huangyihui on 2017/6/23.
 *
 * 1. 用户1写的代码
 *
 *  切换了下  我属于tag下的
 *  sadf
 */
public class App extends Application{

    static {
        //使用夜间模式
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }



}
