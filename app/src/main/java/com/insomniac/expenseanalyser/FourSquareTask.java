package com.insomniac.expenseanalyser;

import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Sanjeev on 2/5/2018.
 */

public class FourSquareTask extends AsynHandlerTask<Void,Void>{

    static final String Client_ID = "IS5AI20N3F5CO23ZQ1MLEMDG4IWQL0NGA02LCWTPN3ACJRSP" ;
    static final String Client_Secret = "R0CLHEBFRMVOAEGRAGP3XPRMDMBF3TZACKEYP2ETSJDNHGMM";

    private Exception mLastError;
    private URL mURL;
    private ArrayList<Place> mPlaces;

    public FourSquareTask(LatLng latLng,String filter) {

        mPlaces = new ArrayList<>();
        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority("api.foursquare.com")
                .appendPath("v2")
                .appendPath("venues")
                .appendPath("search")
                .appendQueryParameter("client_id", Client_ID)
                .appendQueryParameter("client_secret", Client_Secret)
                .appendQueryParameter("v", "20170801")
                .appendQueryParameter("ll", latLng.latitude + "," + latLng.longitude)
                .appendQueryParameter("query", filter)
                .appendQueryParameter("limit", "25")
                .appendQueryParameter("radius", "1250")
                .build();

        try {
            mURL = new URL(uri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected AsynHandlerTask doInBackground(Void... voids) {
        try {
            String data = fetchData();
            parseData(data);
        }catch (Exception e){
            mLastError = e;
            cancel(true);
        }

        return this;
    }

    private String fetchData() throws IOException{
        StringBuilder stringBuilder = new StringBuilder();
        HttpURLConnection httpURLConnection = (HttpURLConnection)mURL.openConnection();

        try{
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));

            String line;
            while((line = bufferedReader.readLine()) != null){
                stringBuilder.append(line).append("\n");
            }
            bufferedReader.close();
            return stringBuilder.toString();
        }finally {
            httpURLConnection.disconnect();
        }
    }

    private void parseData(String data) throws JSONException{

        JSONObject jsonObject = (JSONObject) new JSONTokener(data).nextValue();

        JSONArray venues = jsonObject.getJSONObject("response").getJSONArray("venues");
        for(int i = 0;i < venues.length();i++){
            Place place = new Place();
            JSONObject venue = venues.getJSONObject(i);
            JSONObject loc = venue.getJSONObject("location");
            JSONArray cat = venue.getJSONArray("categories");

            place.setFoursquare(venue.getString("id"));
            place.setName(venue.getString("name"));
            if(loc.has("address")){
                place.setAddress(loc.getString("address"));
            }
            if(cat.length() > 0 && cat.getJSONObject(0).has("shortName")){
                place.setCategory(cat.getJSONObject(0).getString("shortName"));
            }
            place.setLatitude(loc.getDouble("lat"));
            place.setLongitude(loc.getDouble("lng"));

            mPlaces.add(place);
        }
    }

    public ArrayList<Place> getPlaces(){
        return mPlaces;
    }

    @Override
    protected void onCancelled() {
        if(mLastError != null)
            mLastError.printStackTrace();
    }
}
