package com.example.petcompanyapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcompanyapp.R;
import com.example.petcompanyapp.utils.IntentKeys;
import com.example.petcompanyapp.utils.LocationUtils;
import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;

import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity {

    private static final String MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private TextView textSelectedLocation;
    private LatLng selectedLatLng;
    private Marker selectedMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_map_picker);

        textSelectedLocation = findViewById(R.id.textSelectedLocation);
        mapView = findViewById(R.id.mapPickerView);
        Button buttonConfirmLocation = findViewById(R.id.buttonConfirmLocation);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            mapLibreMap = map;
            configureMap();
        });

        buttonConfirmLocation.setOnClickListener(v -> confirmSelection());
    }

    private void configureMap() {
        boolean hasInitialPoint = getIntent().hasExtra(IntentKeys.EXTRA_LATITUDE)
                && getIntent().hasExtra(IntentKeys.EXTRA_LONGITUDE);
        double defaultLat = getIntent().getDoubleExtra(IntentKeys.EXTRA_LATITUDE, -23.550520);
        double defaultLng = getIntent().getDoubleExtra(IntentKeys.EXTRA_LONGITUDE, -46.633308);
        LatLng initialPosition = new LatLng(defaultLat, defaultLng);

        mapLibreMap.setStyle(new Style.Builder().fromUri(MAP_STYLE_URL), style -> {
            mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder()
                            .target(initialPosition)
                            .zoom(12.0)
                            .build()
            ));

            if (hasInitialPoint) {
                updateSelectedPoint(initialPosition);
            } else {
                textSelectedLocation.setText(R.string.map_picker_hint);
            }

            mapLibreMap.addOnMapClickListener(latLng -> {
                updateSelectedPoint(latLng);
                return true;
            });
        });
    }

    private void updateSelectedPoint(LatLng latLng) {
        selectedLatLng = latLng;
        if (selectedMarker != null) {
            mapLibreMap.removeMarker(selectedMarker);
        }
        selectedMarker = mapLibreMap.addMarker(new MarkerOptions().position(latLng));
        textSelectedLocation.setText(getString(
                R.string.map_location_selected,
                String.format(Locale.US, "%.5f", latLng.getLatitude()),
                String.format(Locale.US, "%.5f", latLng.getLongitude())
        ));
        mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0));
    }

    private void confirmSelection() {
        if (selectedLatLng == null) {
            textSelectedLocation.setText(R.string.map_location_required);
            return;
        }

        String reference = LocationUtils.resolveLocationLabel(
                this,
                selectedLatLng.getLatitude(),
                selectedLatLng.getLongitude()
        );

        Intent resultIntent = new Intent();
        resultIntent.putExtra(IntentKeys.EXTRA_LATITUDE, selectedLatLng.getLatitude());
        resultIntent.putExtra(IntentKeys.EXTRA_LONGITUDE, selectedLatLng.getLongitude());
        resultIntent.putExtra(IntentKeys.EXTRA_LOCATION_REFERENCE, reference);
        setResult(RESULT_OK, resultIntent);
        finish();
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
