package com.insomniac.expenseanalyser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class LaunchActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{

    static final int REQUEST_FINE_LOCATION = 1001;
    private PlacesAdapter mPlacesAdapter;
    private int mNeg = -1;
    private String mAmount = "";
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    @BindView(R.id.search_edit_view)
    EditText mSearchEditText;

    @BindView(R.id.button_search)
    Button mSearchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        ButterKnife.bind(this);

        final ListView mPlacesListView = (ListView) findViewById(R.id.list_places);
        mPlacesAdapter = new PlacesAdapter(this,new ArrayList<Place>());
        mPlacesListView.setAdapter(mPlacesAdapter);
        mPlacesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mPlacesAdapter.selectItem(i);

                //bande ko feedback milna chaiye
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(20);
                closeKeyboard();
            }
        });

        updateAmountView();

        if(mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
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
            case "neg":
                mNeg = -1;
            case "done":if(getAmount() == 0.0) {
                            Toast.makeText(getApplicationContext(), "No amount set", Toast.LENGTH_SHORT).show();
                            return;
                            }
                        if (mPlacesAdapter.getSelected() == null) {
                            mPlacesAdapter.selectItem(0);
                            if (mPlacesAdapter.getSelected() == null) {
                                Toast.makeText(getApplicationContext(), "No place selected", Toast.LENGTH_SHORT).show();}
                            else {
                                Toast.makeText(getApplicationContext(), "First place selected, click done again to confirm",Toast.LENGTH_SHORT).show();
                            }
                            return;
                        }

                        Realm realm = Realm.getDefaultInstance();
                        realm.beginTransaction();
                        realm.copyToRealmOrUpdate(mPlacesAdapter.getSelected());
                        realm.commitTransaction();

                        mAmount = "";
                        break;
            }

            updateAmountView();
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
    }

    public void closeKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if(inputMethodManager != null)
            inputMethodManager.hideSoftInputFromWindow(mSearchEditText.getWindowToken(),0);
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
}
