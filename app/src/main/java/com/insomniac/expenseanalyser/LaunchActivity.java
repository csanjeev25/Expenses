package com.insomniac.expenseanalyser;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmModel;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class LaunchActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = LaunchActivity.class.getSimpleName();
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNT = 1003;
    public static final int REQUEST_FINE_LOCATION = 1004;

    private static final String BUTTON_TEXT = "Call Google SHEETS API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {SheetsScopes.SPREADSHEETS_READONLY};

    private PlacesAdapter mPlacesAdapter;
    private int mNeg = -1;
    private String mAmount = "";

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    GoogleAccountCredential mGoogleAccountCredential;

    @BindView(R.id.search_edit_view)
    CustomEditText mSearchEditText;

    @BindView(R.id.button_search)
    Button mSearchButton;

    @BindView(R.id.activity_launch)
    View vLaunchActivity;

    @BindView(R.id.tablet_layout)
    View vTabletLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        ButterKnife.bind(this);

        final ListView mPlacesListView = (ListView) findViewById(R.id.list_places);
        mPlacesAdapter = new PlacesAdapter(this,new ArrayList<Place>());
        mPlacesListView.setAdapter(mPlacesAdapter);

        updateAmountView();

        if(mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mGoogleAccountCredential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());

        final SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loactionUpdate();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        mSearchEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b)
                    vTabletLayout.setVisibility(View.GONE);
                else
                    vTabletLayout.setVisibility(View.VISIBLE);
            }
        });

        mSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                onSearchButtonClick(textView);
                return true;
            }
        });
    }

    public void onButtonPress(View v){
        Button b = (Button) v;

        String tag = (String) b.getTag();
        if(tag == null)
            tag = "";

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if(vibrator != null)
            vibrator.vibrate(20);

        switch(tag) {
            case "del":
                if (mAmount.length() > 0)
                    mAmount = mAmount.substring(0, mAmount.length() - 1);
                break;
            case "neg" :
                mNeg = -1;
            case "done" : storeTransaction();return;
            default : if(b.getText().toString().equals("0") || b.getText().toString().equals("00") || getAmount() == 0.0) return;
                if(mAmount.length() < 8)
                    mAmount = mAmount.concat(b.getText().toString());
                else Toast.makeText(getApplicationContext(), "Tum aur tumhare pitaji ke pass tho duniya bhar ka paisa hoga",
                        Toast.LENGTH_SHORT).show();break;
            }

            updateAmountView();
        }

        private void storeTransaction(){
            Place place = mPlacesAdapter.getSelected();

            if (place == null) {
                mPlacesAdapter.selectItem(0);
                if (mPlacesAdapter.getSelected() == null) {
                    Toast.makeText(getApplicationContext(), "No place selected",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "First place selected, click done again to confirm",
                            Toast.LENGTH_SHORT).show();
                }
                return;
            }

            Transaction transaction = new Transaction(getAmount(),place);
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(transaction);
            realm.commitTransaction();

            startSheetsSync();
            mPlacesAdapter.selectItem(-1);

            mAmount = "";
            updateAmountView();

        }

        private void startSheetsSync(){
            if(mGoogleAccountCredential.getSelectedAccountName() == null){
                chooseAccount();
                return;
            }

            SheetTask sheetTask = new SheetTask(this,mGoogleAccountCredential);
            sheetTask.execute();
        }

        @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNT)
        private void chooseAccount(){
            if(EasyPermissions.hasPermissions(this,Manifest.permission.GET_ACCOUNTS)){

                String accountName = getPreferences(MODE_PRIVATE).getString(PREF_ACCOUNT_NAME,null);
                if(accountName != null){
                    mGoogleAccountCredential.setSelectedAccountName(accountName);
                    startSheetsSync();
                }else {
                    startActivityForResult(mGoogleAccountCredential.newChooseAccountIntent(),REQUEST_ACCOUNT_PICKER);
                }
            }else {
                EasyPermissions.requestPermissions(this,"The contacts permission is needed to authenticate your spreadsheet account",REQUEST_PERMISSION_GET_ACCOUNT,Manifest.permission.GET_ACCOUNTS);

            }
        }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_ACCOUNT_PICKER : if(resultCode == RESULT_OK && data != null && data.getExtras() != null){
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if(accountName != null){
                    SharedPreferences settings = getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(PREF_ACCOUNT_NAME,accountName);
                    editor.apply();
                    mGoogleAccountCredential.setSelectedAccountName(accountName);
                    startSheetsSync();
                }
            }break;

            case REQUEST_AUTHORIZATION : if(resultCode == RESULT_OK) startSheetsSync(); break;
        }
    }

    public float getAmount(){
            if(mAmount.length() == 0)
                return 0;
            return Float.parseFloat(mAmount) / 100.0f * mNeg;
    }

    public void updateAmountView(){
            float amount = getAmount();
            TextView view = findViewById(R.id.amount_text_view);
            String value = String.format(Locale.US,"01.2f",amount);
            if(amount == 0.0f && mNeg < 0)
                value = "-".concat(value);
            view.setText(value);
    }

    public void updatedPlaceView(){
        mPlacesAdapter.clear();
        EditText tv = findViewById(R.id.search_edit_view);
        mPlacesAdapter.findNearbyPlaces(mLastLocation,tv.getText().toString());
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("MissingPermission")
    @AfterPermissionGranted(REQUEST_FINE_LOCATION)
    public void loactionUpdate(){
        if(!EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)){
            EasyPermissions.requestPermissions(this,"Location access is needed to find nearby places",REQUEST_FINE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION);
        }else{
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        updatedPlaceView();
    }

    public void onSearchButtonClick(View v){
        closeKeyboard();
        mPlacesAdapter.clear();
        mPlacesAdapter.findNearbyPlaces(mLastLocation,mSearchEditText.getText().toString());
    }

    public void closeKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if(inputMethodManager != null)
            inputMethodManager.hideSoftInputFromWindow(mSearchEditText.getWindowToken(),0);
        vLaunchActivity.requestFocus();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        loactionUpdate();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this);
    }

    public void onMenuButtonPress(View view) {
        startActivity(new Intent(this,DetailActivity.class));
    }

}
