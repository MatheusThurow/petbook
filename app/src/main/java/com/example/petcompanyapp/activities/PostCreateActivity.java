package com.example.petcompanyapp.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.petcompanyapp.R;
import com.example.petcompanyapp.models.AnimalPost;
import com.example.petcompanyapp.utils.MaskUtils;
import com.example.petcompanyapp.utils.PostType;
import com.example.petcompanyapp.utils.ValidationUtils;

import java.util.Locale;

public class PostCreateActivity extends AppCompatActivity {

    private RadioGroup radioGroupPostType;
    private EditText editAnimalName;
    private Spinner spinnerSpecies;
    private EditText editBreed;
    private EditText editAge;
    private EditText editDescription;
    private EditText editContactPhone;
    private EditText editLocationReference;
    private LinearLayout layoutLostLocation;
    private TextView textLocationStatus;
    private String selectedPostType = PostType.ADOPTION;
    private Double selectedLatitude;
    private Double selectedLongitude;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                    captureCurrentLocation();
                } else {
                    Toast.makeText(this, R.string.error_location_permission, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_create);

        radioGroupPostType = findViewById(R.id.radioGroupPostType);
        editAnimalName = findViewById(R.id.editPostAnimalName);
        spinnerSpecies = findViewById(R.id.spinnerPostSpecies);
        editBreed = findViewById(R.id.editPostBreed);
        editAge = findViewById(R.id.editPostAge);
        editDescription = findViewById(R.id.editPostDescription);
        editContactPhone = findViewById(R.id.editPostContactPhone);
        editLocationReference = findViewById(R.id.editLostLocationReference);
        layoutLostLocation = findViewById(R.id.layoutLostLocation);
        textLocationStatus = findViewById(R.id.textLocationStatus);
        Button buttonCaptureLocation = findViewById(R.id.buttonCaptureLocation);
        Button buttonCreatePost = findViewById(R.id.buttonCreatePost);

        MaskUtils.applyPhoneMask(editContactPhone);
        MaskUtils.configureSpeciesSpinner(this, spinnerSpecies);
        updatePostTypeUi(selectedPostType);

        radioGroupPostType.setOnCheckedChangeListener((group, checkedId) -> {
            selectedPostType = checkedId == R.id.radioPostLost ? PostType.LOST : PostType.ADOPTION;
            updatePostTypeUi(selectedPostType);
        });

        buttonCaptureLocation.setOnClickListener(v -> requestLocation());
        buttonCreatePost.setOnClickListener(v -> createPost());
    }

    private void updatePostTypeUi(String postType) {
        boolean isLost = PostType.isLost(postType);
        layoutLostLocation.setVisibility(isLost ? android.view.View.VISIBLE : android.view.View.GONE);

        if (!isLost) {
            selectedLatitude = null;
            selectedLongitude = null;
            textLocationStatus.setText(R.string.location_not_required);
            editLocationReference.setText("");
        } else {
            textLocationStatus.setText(R.string.location_not_captured);
        }
    }

    private void requestLocation() {
        boolean fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            captureCurrentLocation();
            return;
        }

        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void captureCurrentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            Toast.makeText(this, R.string.error_location_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Location bestLocation = null;

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }

            if (bestLocation == null) {
                textLocationStatus.setText(R.string.error_location_unavailable);
                Toast.makeText(this, R.string.error_location_unavailable, Toast.LENGTH_SHORT).show();
                return;
            }

            selectedLatitude = bestLocation.getLatitude();
            selectedLongitude = bestLocation.getLongitude();
            textLocationStatus.setText(getString(
                    R.string.location_captured,
                    String.format(Locale.US, "%.5f", selectedLatitude),
                    String.format(Locale.US, "%.5f", selectedLongitude)
            ));
        } catch (SecurityException exception) {
            Toast.makeText(this, R.string.error_location_permission, Toast.LENGTH_SHORT).show();
        }
    }

    private void createPost() {
        String animalName = editAnimalName.getText().toString().trim();
        String species = spinnerSpecies.getSelectedItem().toString();
        String breed = editBreed.getText().toString().trim();
        String age = editAge.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String contactPhone = editContactPhone.getText().toString().replaceAll("\\D", "");
        String locationReference = editLocationReference.getText().toString().trim();

        if (ValidationUtils.isEmpty(animalName)) {
            editAnimalName.setError(getString(R.string.error_required_field));
            editAnimalName.requestFocus();
            return;
        }

        if (species.equals(getString(R.string.species_hint))) {
            Toast.makeText(this, R.string.error_species_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (ValidationUtils.isEmpty(breed)) {
            editBreed.setError(getString(R.string.error_required_field));
            editBreed.requestFocus();
            return;
        }

        if (ValidationUtils.isEmpty(age)) {
            editAge.setError(getString(R.string.error_required_field));
            editAge.requestFocus();
            return;
        }

        if (ValidationUtils.isEmpty(description)) {
            editDescription.setError(getString(R.string.error_required_field));
            editDescription.requestFocus();
            return;
        }

        if (contactPhone.length() < 10) {
            editContactPhone.setError(getString(R.string.error_invalid_phone));
            editContactPhone.requestFocus();
            return;
        }

        if (PostType.isLost(selectedPostType)) {
            if (selectedLatitude == null || selectedLongitude == null) {
                Toast.makeText(this, R.string.error_location_required, Toast.LENGTH_SHORT).show();
                return;
            }

            if (ValidationUtils.isEmpty(locationReference)) {
                editLocationReference.setError(getString(R.string.error_required_field));
                editLocationReference.requestFocus();
                return;
            }
        }

        AnimalPost animalPost = new AnimalPost(
                selectedPostType,
                animalName,
                species,
                breed,
                age,
                description,
                contactPhone,
                selectedLatitude,
                selectedLongitude,
                locationReference
        );

        int successMessage = PostType.isLost(animalPost.getPostType())
                ? R.string.post_lost_success
                : R.string.post_adoption_success;
        Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
        finish();
    }
}
