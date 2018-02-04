package com.insomniac.expenseanalyser;

import java.util.Date;

/**
 * Created by Sanjeev on 2/4/2018.
 */

public class Transaction {

    private Date mDate;
    private float mAmount;

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

    public String getPlace() {
        return mPlace;
    }

    public void setPlace(String place) {
        mPlace = place;
    }

    public String getPlaceId() {
        return mPlaceId;
    }

    public void setPlaceId(String placeId) {
        mPlaceId = placeId;
    }

    private String mPlace;
    private String mPlaceId;

    public Transaction() {
        mDate = new Date();
    }

    public Transaction(float amount){
        mAmount = amount;
    }

    public Transaction(float amount,String place){
        mAmount = amount;
        mPlace = place;
    }

    public Transaction(float amount,String place,String placeId){
        mAmount = amount;
        mPlace = place;
        mPlaceId = placeId;
    }
}
