package com.insomniac.expenseanalyser;

import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.webkit.WebView;
import android.widget.TextView;

import io.realm.Realm;


/**
 * Created by Sanjeev on 2/5/2018.
 */

public class DetailActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.about_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        WebView webView = (WebView) findViewById(R.id.webview);
        webView.loadUrl("file:///about.html");

        TextView textView;

        textView = findViewById(R.id.textAboutVersion);
        textView.setText(BuildConfig.VERSION_NAME);

        textView = findViewById(R.id.textAboutDocId);
        textView.setText(PreferenceManager.getDefaultSharedPreferences(this).getString(SheetTask.PREF_SHEET_ID, "<none>"));

        textView = findViewById(R.id.textAboutNumberPlaces);
        Realm realm = Realm.getDefaultInstance();
        textView.setText(String.valueOf(realm.where(Place.class).count()));
        realm.close();

        Location ll = getIntent().getParcelableExtra("location");
        textView = findViewById(R.id.textAboutLocation);
        textView.setText(ll.getLatitude() + "," + ll.getLongitude() + " (±" + ll.getAccuracy() + "m)");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
