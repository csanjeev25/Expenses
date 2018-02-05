package com.insomniac.expenseanalyser;

import android.os.AsyncTask;

/**
 * Created by Sanjeev on 2/5/2018.
 */

abstract public class AsynHandlerTask<Params,Progess> extends AsyncTask<Params,Progess,AsynHandlerTask> {

    public interface OnTaskCompleted{
        void onTaskCompleted(AsynHandlerTask asynHandlerTask);
    }

    private OnTaskCompleted mListener;

    public void setListener(OnTaskCompleted listener){
        mListener = listener;
    }

    @Override
    protected void onPostExecute(AsynHandlerTask asynHandlerTask) {
        if(mListener != null)
            mListener.onTaskCompleted(asynHandlerTask);
    }
}
