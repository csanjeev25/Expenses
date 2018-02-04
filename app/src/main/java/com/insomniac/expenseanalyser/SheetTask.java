package com.insomniac.expenseanalyser;

import android.os.AsyncTask;

import com.fasterxml.jackson.core.JsonFactory;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;

/**
 * Created by DELL on 2/4/2018.
 */

public class SheetTask extends AsyncTask<Void,Void,SheetTask> {

    private onTaskCompleted mOnTaskCompleted;
    private Exception mLastError = null;
    private com.google.api.services.sheets.v4.Sheets mService = null;
    private GoogleAccountCredential mGoogleAccountCredential;

    public interface onTaskCompleted{
        void onTaskCompleted(SheetTask sheetTask);
    }

    public void setOnTaskCompleted(onTaskCompleted taskCompleted){
        mOnTaskCompleted = taskCompleted;
    }

    public SheetTask(GoogleAccountCredential googleAccountCredential){
        mGoogleAccountCredential = googleAccountCredential;
    }

    @Override
    protected SheetTask doInBackground(Void... voids) {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JacksonFactory jacksonFactory = JacksonFactory.getDefaultInstance();
        mService = new Sheets.Builder(httpTransport,jacksonFactory,mGoogleAccountCredential)
                .setApplicationName("ExpenseAnalyser")
                .build();
        return null;
    }

    @Override
    protected void onPostExecute(SheetTask sheetTask) {
        if(mOnTaskCompleted != null)
            mOnTaskCompleted.onTaskCompleted(sheetTask);
    }
}
