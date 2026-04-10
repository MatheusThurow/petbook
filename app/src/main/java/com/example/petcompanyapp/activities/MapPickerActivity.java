package com.example.petcompanyapp.activities;

import android.content.Intent;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private TextView textSelectedLocation;
    private LatLng selectedLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        textSelectedLocation = findViewById(R.id.textSelectedLocation);
        Button buttonConfirmLocation = findViewById(R.id.buttonConfirmLocation);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapPickerFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (!MapConfigUtils.hasConfiguredMapsKey(this)) {
            textSelectedLocation.setText(R.string.error_maps_key_missing);
        }

        buttonConfirmLocation.setOnClickListener(v -> confirmSelection());
    }

    @Override
    public void onMapReady(GoogleMap map) {
        MapsInitializer.initialize(this);
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);

        boolean hasInitialPoint = getIntent().hasExtra(IntentKeys.EXTRA_LATITUDE)
                && getIntent().hasExtra(IntentKeys.EXTRA_LONGITUDE);
        double defaultLat = getIntent().getDoubleExtra(IntentKeys.EXTRA_LATITUDE, -23.550520);
        double defaultLng = getIntent().getDoubleExtra(IntentKeys.EXTRA_LONGITUDE, -46.633308);
        LatLng initialPosition = new LatLng(defaultLat, defaultLng);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 12f));
        if (hasInitialPoint) {
            selectedLatLng = initialPosition;
            googleMap.addMarker(new MarkerOptions().position(initialPosition));
            textSelectedLocation.setText(getString(
                    R.string.map_location_selected,
                    String.format(Locale.US, "%.5f", initialPosition.latitude),
                    String.format(Locale.US, "%.5f", initialPosition.longitude)
            ));
        }

        googleMap.setOnMapLoadedCallback(() -> {
            if (selectedLatLng == null) {
                textSelectedLocation.setText(R.string.map_picker_hint);
            }
        });
        googleMap.setOnMapClickListener(latLng -> {
            selectedLatLng = latLng;
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(latLng));
            textSelectedLocation.setText(getString(
                    R.string.map_location_selected,
                    String.format(Locale.US, "%.5f", latLng.latitude),
                    String.format(Locale.US, "%.5f", latLng.longitude)
            ));
        });
    }

    private void confirmSelection() {
        if (selectedLatLng == null) {
            textSelectedLocation.setText(R.string.map_location_required);
            return;
        }

        String reference = LocationUtils.resolveLocationLabel(
                this,
                selectedLatLng.latitude,
                selectedLatLng.longitude
        );

        Intent resultIntent = new Intent();
        resultIntent.putExtra(IntentKeys.EXTRA_LATITUDE, selectedLatLng.latitude);
        resultIntent.putExtra(IntentKeys.EXTRA_LONGITUDE, selectedLatLng.longitude);
        resultIntent.putExtra(IntentKeys.EXTRA_LOCATION_REFERENCE, reference);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
