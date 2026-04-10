package com.example.petcompanyapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcompanyapp.R;
import com.example.petcompanyapp.utils.IntentKeys;
import com.example.petcompanyapp.utils.LocationUtils;
import com.example.petcompanyapp.utils.MapConfigUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

public class PostLocationMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView textLocationSummary;
    private double latitude;
    private double longitude;
    private String locationReference;
    private String animalName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_location_map);

        textLocationSummary = findViewById(R.id.textLocationSummary);
        Button buttonOpenExternalMap = findViewById(R.id.buttonOpenExternalMap);
        latitude = getIntent().getDoubleExtra(IntentKeys.EXTRA_LATITUDE, -23.550520d);
        longitude = getIntent().getDoubleExtra(IntentKeys.EXTRA_LONGITUDE, -46.633308d);
        locationReference = getIntent().getStringExtra(IntentKeys.EXTRA_LOCATION_REFERENCE);
        animalName = getIntent().getStringExtra(IntentKeys.EXTRA_USER_NAME);
        if (locationReference == null || locationReference.trim().isEmpty()) {
            locationReference = LocationUtils.resolveLocationLabel(this, latitude, longitude);
        }

        textLocationSummary.setText(getString(
                R.string.feed_post_location_detail,
                locationReference,
                String.format(Locale.US, "%.5f", latitude),
                String.format(Locale.US, "%.5f", longitude)
        ));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapPostLocation);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        buttonOpenExternalMap.setOnClickListener(v -> openExternalMap());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(this);
        LatLng position = new LatLng(latitude, longitude);

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f));
        googleMap.addMarker(new MarkerOptions()
                .position(position)
                .title(animalName)
                .snippet(locationReference)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        if (!MapConfigUtils.hasConfiguredMapsKey(this)) {
            textLocationSummary.append("\n" + getString(R.string.error_maps_key_missing));
        }
    }

    private void openExternalMap() {
        Uri uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
}
