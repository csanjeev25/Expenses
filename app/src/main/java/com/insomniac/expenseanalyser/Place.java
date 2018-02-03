package com.insomniac.expenseanalyser;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Sanjeev on 2/3/2018.
 */
@SuppressWarnings("unused")
public class Place extends RealmObject{

    @PrimaryKey
    private String id = "";
    private String name = "";
    private String foreign = "";
    private Double latitude = 0.0;
    private Double longitude = 0.0;
    @Index
    private Date lastUsed;
    private String info = "";

    public Place(){
        lastUsed = new Date();
    }

    private void updateId(){
        String s = name;
        if(!foreign.isEmpty())
            s = foreign;
        
    }
}
