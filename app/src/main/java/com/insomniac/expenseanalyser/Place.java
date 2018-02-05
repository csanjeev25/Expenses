package com.insomniac.expenseanalyser;

import com.google.android.gms.maps.model.LatLng;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Objects;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Sanjeev on 2/3/2018.
 */
@SuppressWarnings("unused")
public class Place extends RealmObject{

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLongitude() {
        return longitude;
    }


    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }


    @PrimaryKey
    private String id = "";
    private String name = "";
    private Double latitude = 0.0;
    private Double longitude = 0.0;
    private String foursquare = "";

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    @Ignore
    private boolean local = false;

    public String getFoursquare() {
        return foursquare;
    }

    public void setFoursquare(String foursquare) {
        this.foursquare = foursquare;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    private String address = "";
    private String category = "";
    @Index
    private Date lastUsed;

    public Place(){
        lastUsed = new Date();
    }

            /**
          * Recalculates the primary key
          * The primary key is a MD5 hash of either the name or the foreign identifier
         */

    private void updateId(){
        String s = name;
        if(!foursquare.isEmpty())
            s = foursquare;

        try{
            MessageDigest messageDigest = java.security.MessageDigest.getInstance("MD5");
            messageDigest.update(s.getBytes());
            byte digest[] = messageDigest.digest();

            StringBuilder hexString = new StringBuilder();
            for(byte aMessageDigest : digest) {
                StringBuilder h = new StringBuilder(Integer.toString(0xff & aMessageDigest));

                while (h.length() < 2){
                    h.insert(0,"0");
                }
                hexString.append(h);
            }
            id = hexString.toString();
        }catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        lastUsed = new Date();
    }

    public void setLatLng(LatLng latLng){
        latitude = latLng.latitude;
        longitude = latLng.longitude;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(!(obj instanceof Place))
            return false;
        Place place = (Place) obj;
        return !place.getId().equals("") &&
                !this.getId().equals("") &&
                place.getId().equals(this.getId());
    }

    @Override
    public int hashCode(){
        return Objects.hash(id);
    }
}
