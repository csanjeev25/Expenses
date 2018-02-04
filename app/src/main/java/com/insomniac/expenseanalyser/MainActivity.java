package com.insomniac.expenseanalyser;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{

    GoogleAccountCredential mGoogleAccountCredential;
    private TextView mOutputTextView;
    private Button mCallApiButton;
    ProgressDialog mProgressDialog;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNT = 1003;

    private static final String BUTTON_TEXT = "Call Google SHEETS API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {SheetsScopes.SPREADSHEETS_READONLY};

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        LinearLayout activityLayout = new LinearLayout(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        activityLayout.setLayoutParams(layoutParams);
        activityLayout.setOrientation(LinearLayout.VERTICAL);
        activityLayout.setPadding(16,16,16,16);

        ViewGroup.LayoutParams viewGroupLayoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mCallApiButton = new Button(this);
        mCallApiButton.setText(BUTTON_TEXT);
        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallApiButton.setEnabled(false);
                mOutputTextView.setText("");
                getResultsFromApi();
                mCallApiButton.setEnabled(true);
            }
        });
        activityLayout.addView(mCallApiButton);

        mOutputTextView = new TextView(this);
        mOutputTextView.setLayoutParams(viewGroupLayoutParams);
        mOutputTextView.setPadding(16,16,16,16);
        mOutputTextView.setVerticalScrollBarEnabled(true);
        mOutputTextView.setMovementMethod(new ScrollingMovementMethod());
        mOutputTextView.setText("Click the \'" + BUTTON_TEXT + "\' button to test the API.");
        activityLayout.addView(mOutputTextView);

        setContentView(activityLayout);

        mGoogleAccountCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
    }

    public void getResultsFromApi(){
        if(!isGooglePlayServicesAvailable())
            acquireGooglePlayServices();
        else if(mGoogleAccountCredential.getSelectedAccountName() == null)
            chooseAccount();
        else if(!isDeviceOnline())
            mOutputTextView.setText("No network connection available");
        else
            new MakeRequestTask(mGoogleAccountCredential).execute();
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNT)
    private void chooseAccount(){
        if(EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)){
            String accountName = getPreferences(MODE_PRIVATE).getString(PREF_ACCOUNT_NAME,null);
            if(accountName != null){
                mGoogleAccountCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            }else{
                startActivityForResult(mGoogleAccountCredential.newChooseAccountIntent(),REQUEST_ACCOUNT_PICKER);
            }
        }else{
            EasyPermissions.requestPermissions(this,"This app needs to access your Google Accounts (via Contacts)",REQUEST_PERMISSION_GET_ACCOUNT,Manifest.permission.GET_ACCOUNTS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_GOOGLE_PLAY_SERVICES : if(resultCode != RESULT_OK){
                mOutputTextView.setText("This app requires Google Play Services. Please Install Google Play Sevices on your device");
            }else
                getResultsFromApi();
            break;

            case REQUEST_ACCOUNT_PICKER : if(resultCode == RESULT_OK && data != null && data.getExtras() != null){
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if(accountName != null){
                    SharedPreferences settings = getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(PREF_ACCOUNT_NAME,accountName);
                    editor.apply();
                    mGoogleAccountCredential.setSelectedAccountName(accountName);
                    getResultsFromApi();
                }
            }
            break;

            case REQUEST_AUTHORIZATION : if(requestCode != RESULT_OK)
                getResultsFromApi();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this);
    }

    private boolean isDeviceOnline(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isGooglePlayServicesAvailable(){
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }


    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    private void acquireGooglePlayServices(){
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if(googleApiAvailability.isUserResolvableError(connectionStatusCode))
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
    }

    private void showGooglePlayServicesAvailabilityErrorDialog(int connectStatusCode){
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = googleApiAvailability.getErrorDialog(
                MainActivity.this,connectStatusCode,REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private class MakeRequestTask extends AsyncTask<Void,Void,List<String>>{
        private Sheets mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential googleAccountCredential){
            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JacksonFactory jacksonFactory = JacksonFactory.getDefaultInstance();
            mService = new Sheets.Builder(
                    httpTransport,jacksonFactory,googleAccountCredential)
                    .setApplicationName("ExpenseAnalyser")
                    .build();
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            try{
                return getDataFromApi();
            }catch (Exception e){
                cancel(true);
                return null;
            }
        }

        private List<String> getDataFromApi() throws IOException {
            String spreadSheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";
            String range = "Class Data!A2:E";
            List<String> results = new ArrayList<String>();
            ValueRange response = this.mService.spreadsheets().values()
                    .get(spreadSheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            if (values != null) {
                results.add("Name, Major");
                for (List row : values) {
                    results.add(row.get(0) + ", " + row.get(4));
                }
            }
            return results;
        }

        @Override
        protected void onPreExecute() {
            mOutputTextView.setText("");
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgressDialog.hide();
            if (output == null || output.size() == 0) {
                mOutputTextView.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Google Sheets API:");
                mOutputTextView.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            mProgressDialog.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputTextView.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputTextView.setText("Request cancelled.");
            }
        }

    }
}
