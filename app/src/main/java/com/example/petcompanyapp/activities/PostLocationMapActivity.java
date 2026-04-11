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
import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;

import java.util.Locale;

public class PostLocationMapActivity extends AppCompatActivity {

    private static final String MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";

    private TextView textLocationSummary;
    private MapView mapView;
    private double latitude;
    private double longitude;
    private String locationReference;
    private String animalName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_post_location_map);

        textLocationSummary = findViewById(R.id.textLocationSummary);
        mapView = findViewById(R.id.mapPostLocation);
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

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this::configureMap);

        buttonOpenExternalMap.setOnClickListener(v -> openExternalMap());
    }

    private void configureMap(MapLibreMap mapLibreMap) {
        LatLng position = new LatLng(latitude, longitude);

        mapLibreMap.setStyle(new Style.Builder().fromUri(MAP_STYLE_URL), style -> {
            mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder()
                            .target(position)
                            .zoom(15.0)
                            .build()
            ));
            mapLibreMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(animalName)
                    .snippet(locationReference));
        });
    }

    private void openExternalMap() {
        Uri uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
