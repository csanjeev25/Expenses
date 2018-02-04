package com.insomniac.expenseanalyser;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Sanjeev on 2/4/2018.
 */
@SuppressWarnings("unused")
public class Transaction extends RealmObject{

    @PrimaryKey
    private String mTransactionID;
    private Date mDate;
    private float mAmount = 0.0f;
    private Place mPlace;

    public Transaction(){
        mTransactionID = UUID.randomUUID().toString();
        mDate = new Date();
    }

    public String getTransactionID() {
        return mTransactionID;
    }

    public void setTransactionID(String transactionID) {
        mTransactionID = transactionID;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public float getAmount() {
        return mAmount;
    }

    public void setAmount(float amount) {
        mAmount = amount;
    }

    public Place getPlace() {
        return mPlace;
    }

    public void setPlace(Place place) {
        mPlace = place;
    }

    public Transaction(float amount, Place place){
        mTransactionID = UUID.randomUUID().toString();
        mAmount = amount;
        mPlace = place;
        mDate = new Date();
    }

}
