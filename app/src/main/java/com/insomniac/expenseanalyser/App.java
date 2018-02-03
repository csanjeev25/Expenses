package com.insomniac.expenseanalyser;

import android.app.Application;

import io.realm.Realm;

/**
 * Created by Sanjeev on 2/3/2018.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(getApplicationContext());
    }
}
