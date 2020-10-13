package com.example.mapproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.OpeningHours;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements LocationListener {

    private GoogleMap mMap;
    private static final int PERMISSION_CODE = 101;
    String[] permissions_all={Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION};
    LocationManager locationManager;
    boolean isGpsLocation;
    Location mLocation;
    boolean isNetworklocation;
    ProgressDialog progressDialog;
    EditText editText;
    TextView infoName;
    String placeName, placePhone;
    Uri placeWebsite;
    Double placeRating;
    LatLng start = null;
    LatLng end = null;
    Location current = new Location("Current");
    Location destination  = new Location("Destination");
    PlacesClient placesClient;
    List<Place.Field> fieldList= Arrays.asList(Place.Field.NAME,
            Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.PHONE_NUMBER, Place.Field.WEBSITE_URI,
             Place.Field.RATING);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        infoName = findViewById(R.id.info_name);
        editText= findViewById(R.id.edit_text);

        initializePlaces();
        editText.setFocusable(false);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,
                     fieldList).build(MapsActivity.this);
             startActivityForResult(intent, 100);
            }
        });

        progressDialog=new ProgressDialog(MapsActivity.this);
        progressDialog.setMessage("Fetching location...");

        SupportMapFragment mapFragment=(SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                mMap=googleMap;
                getLocation();

            }
        });
    }

    private void initializePlaces(){
        Places.initialize(getApplicationContext(),"AIzaSyDv90cTp1k7Ym1LcWUSe91-8lyR3Wi5x3M");
        placesClient = Places.createClient(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100 && resultCode == RESULT_OK){
            final Place place = Autocomplete.getPlaceFromIntent(data);

            placeName = place.getName();
            placePhone = place.getPhoneNumber();
            placeWebsite = place.getWebsiteUri();
            placeRating = place.getRating();

            end = place.getLatLng();
            current.setLatitude(start.latitude);
            current.setLongitude(start.longitude);
            destination.setLatitude(end.latitude);
            destination.setLongitude(end.latitude);
            double distance = current.distanceTo(destination);
            distance = distance/1609.344;
            distance = Math.round(distance);

            editText.setText(placeName + ", " + place.getAddress());

                    infoName.setText(placeName + ": " + distance + " miles away\n\n");

                    if(placeRating!=null)
                    infoName.append("Rating: " + placeRating + "/5\n\n");
                    if(placePhone!=null)
                        infoName.append("Phone Number: " + placePhone + "\n\n");
                    if(placeWebsite!=null)
                        infoName.append("Website: " + placeWebsite + "\n\n");

            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(end).title(placeName));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(end));
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(end, 10f);
            mMap.animateCamera(update);
        }
        else
            if(resultCode == AutocompleteActivity.RESULT_ERROR){
                Status status = Autocomplete.getStatusFromIntent(data);
                Toast.makeText(getApplicationContext(),status.getStatusMessage(),
                        Toast.LENGTH_SHORT).show();
            }
    }

    private void getLocation() {
        progressDialog.show();
        if(Build.VERSION.SDK_INT>=23){
            if(checkPermission()){
                getDeviceLocation();
            }
            else{
                requestPermission();
            }
        }
        else{
            getDeviceLocation();
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MapsActivity.this,permissions_all,PERMISSION_CODE);
    }

    private boolean checkPermission() {
        for(int i=0;i<permissions_all.length;i++){
            int result= ContextCompat.checkSelfPermission(MapsActivity.this,permissions_all[i]);
            if(result== PackageManager.PERMISSION_GRANTED){
                continue;
            }
            else {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_CODE:
                if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    getFinalLocation();
                }
                else{
                    Toast.makeText(this, "Permission Failed", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void showSettingForLocation() {
        AlertDialog.Builder al=new AlertDialog.Builder(MapsActivity.this);
        al.setTitle("Location Not Enabled!");
        al.setMessage("Enable Location ?");
        al.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });
        al.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        al.show();
    }
    private void getDeviceLocation() {
        Log.d("TAG", "GOT DEVICE LOCATION");
        locationManager=(LocationManager)getSystemService(Service.LOCATION_SERVICE);
        isGpsLocation=locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworklocation=locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(!isGpsLocation && !isNetworklocation){
            showSettingForLocation();
            getLastLocation();
        }
        else{
            getFinalLocation();
        }
    }

    private void getLastLocation() {
        if(locationManager!=null) {
            try {
                Criteria criteria = new Criteria();
                String provider = locationManager.getBestProvider(criteria,false);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void getFinalLocation() {

        try{
            if(isGpsLocation){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000*60,10,MapsActivity.this);
                if(locationManager!=null){
                    mLocation=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if(mLocation!=null){
                        updateUi(mLocation);
                    }
                }
            }
            else if(isNetworklocation){
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000*60*1,10,MapsActivity.this);
                if(locationManager!=null){
                    mLocation=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if(mLocation!=null){
                        updateUi(mLocation);
                    }
                }
            }
            else{
                Toast.makeText(this, "Can't Get Location", Toast.LENGTH_SHORT).show();
            }

        }catch (SecurityException e){
            Toast.makeText(this, "Can't Get Location", Toast.LENGTH_SHORT).show();
        }

    }

    private void updateUi(Location loc) {
        if(loc.getLatitude()==0 && loc.getLongitude()==0){
            getDeviceLocation();
        }
        else{
            progressDialog.dismiss();

            infoName.setText("Current location: " + start);
            start = new LatLng(loc.getLatitude(),loc.getLongitude());

            mMap.addMarker(new MarkerOptions().position(start).title("Current Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(start));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(start,15f));

        }

    }

    @Override
    public void onLocationChanged(Location location) {
        updateUi(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

}
