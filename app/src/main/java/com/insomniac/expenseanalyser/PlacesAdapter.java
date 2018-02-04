package com.insomniac.expenseanalyser;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Sanjeev on 2/3/2018.
 */

public class PlacesAdapter extends ArrayAdapter {

    private Context mContext;
    private int mSelected = -1;
    private final ArrayList<Place> mPlaceArrayList;

    PlacesAdapter(@NonNull Context context, ArrayList<Place> placeArrayList) {
        super(context, -1, placeArrayList);
        mContext = context;
        mPlaceArrayList = placeArrayList;
    }

    public void selectItem(int position) {
        mSelected = position;
        notifyDataSetChanged();
    }

    @Nullable
    public Place getSelected() {
        try {
            return mPlaceArrayList.get(mSelected);
        } catch (ArrayIndexOutOfBoundsException e) {
            mSelected = -1;
            return null;
        }
    }

    public void findNearbyPlaces(@Nullable Location location, String filter) {
        if (location == null) {
            Toast.makeText(mContext, "No Location Avail", Toast.LENGTH_SHORT).show();
            return;
        }

        loadLocalPlaces(location);

        final PlaceDetectionClient placeDetectionClient = Places.getPlaceDetectionClient(mContext, null);

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final Task<PlaceLikelihoodBufferResponse> placeLikelihoodBufferResponseTask = placeDetectionClient.getCurrentPlace(null);
        placeLikelihoodBufferResponseTask.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                PlaceLikelihoodBufferResponse likelihoods = placeLikelihoodBufferResponseTask.getResult();
                for(PlaceLikelihood placeLikelihood : likelihoods){
                    Place place = new Place();
                    place.setName(placeLikelihood.getPlace().getName().toString());
                    place.setLatLng(placeLikelihood.getPlace().getLatLng());
                    place.setInfo(placeLikelihood.getPlace().getAddress().toString());
                    place.setForeign(placeLikelihood.getPlace().getId());

                    if(mPlaceArrayList.contains(place)){
                        Log.d("me","place already there");
                    }else
                        add(place);
                }
                likelihoods.release();
            }
        });
    }

    public void loadLocalPlaces(Location location){
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        int radius = 500;
        if(location.hasAccuracy()){
            radius = 250;
        }
        LatLngBounds latLngBounds = toBounds(latLng,radius);

        Realm realm = Realm.getDefaultInstance();
        RealmQuery<Place> placeRealmQuery = realm.where(Place.class);
        placeRealmQuery.between("lat",latLngBounds.southwest.latitude,latLngBounds.northeast.latitude);
        placeRealmQuery.between("lon",latLngBounds.southwest.longitude,latLngBounds.northeast.longitude);

        RealmResults<Place> placeRealmResults = placeRealmQuery.findAllSorted("lastused", Sort.DESCENDING);
        for(Place result : placeRealmResults)
            add(result);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View rowView = LayoutInflater.from(mContext).inflate(R.layout.places_row,parent,false);

        Place item = mPlaceArrayList.get(position);
        assert item != null;

        TextView textView = rowView.findViewById(R.id.firstLine);
        textView.setText(item.getName());

        TextView textView1 = rowView.findViewById(R.id.radio_description_text_view);
        textView1.setText(item.getInfo());

        RadioButton radioButton = rowView.findViewById(R.id.select_radio_button);
        radioButton.setChecked(mSelected == position);

        return rowView;
    }

    @Override
    public void clear() {
        super.clear();
        mSelected = -1;
    }

    @NonNull
    public LatLngBounds toBounds(LatLng centre,int radius){
        double distanceFromCentreToCorner = radius*Math.sqrt(2.0);
        LatLng southWeatCorner = SphericalUtil.computeOffset(centre,distanceFromCentreToCorner,255.0);
        LatLng northEastCorner = SphericalUtil.computeOffset(centre,distanceFromCentreToCorner,45.0);

        return new LatLngBounds(southWeatCorner,northEastCorner);
    }
}
