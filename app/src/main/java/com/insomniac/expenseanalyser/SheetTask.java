package com.insomniac.expenseanalyser;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.UpdateSpreadsheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Sanjeev on 2/4/2018.
 */

public class SheetTask extends AsynHandlerTask<Void,Void> {

    private final String TAG = SheetTask.class.getSimpleName();
    public static final String PREF_SHEET_ID = "sheetID";
    private static final String SHEET_TITLE = "ExpenseAnalyser";
    private WeakReference<LaunchActivity> mContextRef;


    private Exception mLastError = null;
    private com.google.api.services.sheets.v4.Sheets mSheets = null;
    private GoogleAccountCredential mGoogleAccountCredential;



    public SheetTask(LaunchActivity launchActivity,GoogleAccountCredential googleAccountCredential){
        mContextRef = new WeakReference<LaunchActivity>(launchActivity);
        mGoogleAccountCredential = googleAccountCredential;

        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JacksonFactory jacksonFactory = JacksonFactory.getDefaultInstance();
        mSheets = new Sheets.Builder(httpTransport,jacksonFactory,mGoogleAccountCredential)
                .setApplicationName("ExpenseAnalyser")
                .build();
    }

    @Override
    protected SheetTask doInBackground(Void... voids) {

        try{
            String docID = getOrCreateDocument();
            transferTransaction(docID);

        }catch (IOException e){
            mLastError = e;
            cancel(true);
        }
        return this;
    }

    private void transferTransaction(String docID){
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        RealmResults<Transaction> transactions = realm.where(Transaction.class).findAllSorted("mDate", Sort.ASCENDING);
        List<List<Object>> values = new ArrayList<>();
        for(Transaction transaction : transactions){
            List<Object> row = new ArrayList<>();
            row.add(transaction.getTransactionID());
            row.add(transaction.getAmount());
            row.add(android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss", transaction.getDate()));
            row.add(transaction.getPlace().getName());
            row.add(transaction.getPlace().getAddress());
            row.add(transaction.getPlace().getCategory());
            row.add(transaction.getPlace().getFoursquare());
            row.add(transaction.getPlace().getLatitude());
            row.add(transaction.getPlace().getLongitude());
            values.add(row);
        }

        transactions.deleteAllFromRealm();
        realm.commitTransaction();
        try{
            append(docID,values);
        }catch (IOException e){
            
        }

        realm.close();
    }

    private List<List<Object>> getHeaders(){
        List<List<Object>> values = new ArrayList<>();
        List<Object> row = new ArrayList<>();

        row.add("TX ID");
        row.add("Amount");
        row.add("Date");
        row.add("Place");
        row.add("Address");
        row.add("Category");
        row.add("FourSquareId");
        row.add("Latitude");
        row.add("Longitude");
        values.add(row);

        return values;
    }

    private void append(String docId,List<List<Object>> vales) throws IOException{

        String range = "A1:B1";

        ValueRange requestBody = new ValueRange();
        requestBody.setValues(vales);

        Sheets.Spreadsheets.Values.Append request = mSheets.spreadsheets().values().append(docId,range,requestBody).setValueInputOption("USER_ENTERED");

        AppendValuesResponse response = request.execute();
        System.out.println(response);
    }

    private String getOrCreateDocument() throws IOException {

        Context context = mContextRef.get();
        if(context == null) throw new IOException("no context available");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String docID = sharedPreferences.getString(PREF_SHEET_ID,null);

        if (docID != null) {
            try {
                mSheets.spreadsheets().get(docID).execute();
                return docID;
            } catch (IOException e) {
                Log.e(TAG, "Known doc is not accessible, we forget about it");
                throw e;
            }
        }

        Spreadsheet spreadsheet = mSheets.spreadsheets().create(new Spreadsheet()).execute();

        List<Request> requests = new ArrayList<>();
        requests.add(new Request()
                .setUpdateSpreadsheetProperties(new UpdateSpreadsheetPropertiesRequest()
                        .setProperties(new SpreadsheetProperties()
                                .setTitle(SHEET_TITLE)
                        )
                        .setFields("title")
                )
        );

        requests.add(new Request()
                .setUpdateSpreadsheetProperties(new UpdateSpreadsheetPropertiesRequest()
                        .setProperties(new SpreadsheetProperties()
                                .setLocale("en_US")
                        )
                        .setFields("locale")
                )
        );

        BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest().setRequests(requests);
        mSheets.spreadsheets().batchUpdate(spreadsheet.getSpreadsheetId(),batchUpdateSpreadsheetRequest).execute();

        append(spreadsheet.getSpreadsheetId(),getHeaders());

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_SHEET_ID,spreadsheet.getSpreadsheetId());
        editor.apply();

        return spreadsheet.getSpreadsheetId();
    }



    @Override
    protected void onCancelled() {
        if(mLastError != null) {
            if (mLastError instanceof UserRecoverableAuthIOException) {
                LaunchActivity launchActivity = mContextRef.get();
                if(launchActivity != null){
                    launchActivity.startActivityForResult(((UserRecoverableAuthIOException)mLastError).getIntent(),LaunchActivity.REQUEST_AUTHORIZATION);
                }
            }
        }

        Log.e(TAG, "task was cancelled", mLastError);
            }
        }

