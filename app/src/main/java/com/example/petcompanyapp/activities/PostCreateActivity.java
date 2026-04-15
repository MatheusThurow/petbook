package com.example.petcompanyapp.activities;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.example.petcompanyapp.repositories.AnimalPostRepository;
import com.example.petcompanyapp.repositories.ApiPostRepository;
import com.example.petcompanyapp.repositories.UserRepository;
import com.example.petcompanyapp.utils.IntentKeys;
import com.example.petcompanyapp.utils.AsyncRunner;
import com.example.petcompanyapp.utils.FeatureFlags;
import com.example.petcompanyapp.utils.ImageUtils;
import com.example.petcompanyapp.utils.LocationUtils;
import com.example.petcompanyapp.utils.MaskUtils;
import com.example.petcompanyapp.utils.PostType;
import com.example.petcompanyapp.utils.UserProfileStorage;
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
    private ImageView imagePostPreview;
    private Long authorUserId;
    private Long editingPostId;
    private String selectedPostType = PostType.ADOPTION;
    private String authorName;
    private Double selectedLatitude;
    private Double selectedLongitude;
    private Uri selectedImageUri;

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

    private final ActivityResultLauncher<String[]> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) {
                    return;
                }

                getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                selectedImageUri = uri;
                imagePostPreview.setImageURI(uri);
            });

    private final ActivityResultLauncher<Intent> mapPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }

                Intent data = result.getData();
                selectedLatitude = data.getDoubleExtra(IntentKeys.EXTRA_LATITUDE, 0d);
                selectedLongitude = data.getDoubleExtra(IntentKeys.EXTRA_LONGITUDE, 0d);
                String locationReference = data.getStringExtra(IntentKeys.EXTRA_LOCATION_REFERENCE);

                editLocationReference.setText(locationReference);
                textLocationStatus.setText(getString(
                        R.string.location_captured,
                        String.format(Locale.US, "%.5f", selectedLatitude),
                        String.format(Locale.US, "%.5f", selectedLongitude)
                ));
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
        imagePostPreview = findViewById(R.id.imagePostPreview);
        TextView textBack = findViewById(R.id.textBackPostCreate);
        Button buttonSelectImage = findViewById(R.id.buttonSelectImage);
        Button buttonCaptureLocation = findViewById(R.id.buttonCaptureLocation);
        Button buttonSelectLocationOnMap = findViewById(R.id.buttonSelectLocationOnMap);
        Button buttonCreatePost = findViewById(R.id.buttonCreatePost);

        MaskUtils.applyPhoneMask(editContactPhone);
        MaskUtils.configureSpeciesSpinner(this, spinnerSpecies);
        long extraUserId = getIntent().getLongExtra(IntentKeys.EXTRA_USER_ID, -1L);
        authorUserId = extraUserId >= 0 ? extraUserId : UserProfileStorage.getUserId(this);
        authorName = getIntent().getStringExtra(IntentKeys.EXTRA_USER_NAME);
        if (authorName == null || authorName.trim().isEmpty()) {
            authorName = getString(R.string.default_user_name);
        }
        authorName = UserProfileStorage.getName(this, authorName);
        editingPostId = getIntent().hasExtra(IntentKeys.EXTRA_POST_ID)
                ? getIntent().getLongExtra(IntentKeys.EXTRA_POST_ID, -1L)
                : null;
        if (editingPostId != null && editingPostId < 0) {
            editingPostId = null;
        }

        bindEditingData(buttonCreatePost);
        updatePostTypeUi(selectedPostType);

        radioGroupPostType.setOnCheckedChangeListener((group, checkedId) -> {
            selectedPostType = checkedId == R.id.radioPostLost ? PostType.LOST : PostType.ADOPTION;
            updatePostTypeUi(selectedPostType);
        });

        textBack.setOnClickListener(v -> finish());
        buttonCaptureLocation.setOnClickListener(v -> requestLocation());
        buttonSelectLocationOnMap.setOnClickListener(v -> openMapPicker());
        buttonSelectImage.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));
        buttonCreatePost.setOnClickListener(v -> createPost());
    }

    private void bindEditingData(Button buttonCreatePost) {
        if (editingPostId == null) {
            return;
        }

        selectedPostType = getIntent().getStringExtra(IntentKeys.EXTRA_POST_TYPE);
        if (selectedPostType == null || selectedPostType.trim().isEmpty()) {
            selectedPostType = PostType.ADOPTION;
        }

        radioGroupPostType.check(PostType.isLost(selectedPostType)
                ? R.id.radioPostLost
                : R.id.radioPostAdoption);
        editAnimalName.setText(getIntent().getStringExtra(IntentKeys.EXTRA_POST_ANIMAL_NAME));
        setSpinnerSelection(getIntent().getStringExtra(IntentKeys.EXTRA_POST_SPECIES));
        editBreed.setText(getIntent().getStringExtra(IntentKeys.EXTRA_POST_BREED));
        editAge.setText(getIntent().getStringExtra(IntentKeys.EXTRA_POST_AGE));
        editDescription.setText(getIntent().getStringExtra(IntentKeys.EXTRA_POST_DESCRIPTION));
        editContactPhone.setText(getIntent().getStringExtra(IntentKeys.EXTRA_POST_CONTACT_PHONE));
        editLocationReference.setText(getIntent().getStringExtra(IntentKeys.EXTRA_LOCATION_REFERENCE));

        if (getIntent().hasExtra(IntentKeys.EXTRA_LATITUDE)) {
            selectedLatitude = getIntent().getDoubleExtra(IntentKeys.EXTRA_LATITUDE, 0d);
        }
        if (getIntent().hasExtra(IntentKeys.EXTRA_LONGITUDE)) {
            selectedLongitude = getIntent().getDoubleExtra(IntentKeys.EXTRA_LONGITUDE, 0d);
        }

        String imageUri = getIntent().getStringExtra(IntentKeys.EXTRA_POST_IMAGE_URI);
        if (imageUri != null && !imageUri.trim().isEmpty()) {
            selectedImageUri = Uri.parse(imageUri);
            imagePostPreview.setImageURI(selectedImageUri);
        }

        if (selectedLatitude != null && selectedLongitude != null) {
            textLocationStatus.setText(getString(
                    R.string.location_captured,
                    String.format(Locale.US, "%.5f", selectedLatitude),
                    String.format(Locale.US, "%.5f", selectedLongitude)
            ));
        }

        buttonCreatePost.setText(R.string.button_save_post_changes);
    }

    private void setSpinnerSelection(String species) {
        if (species == null || spinnerSpecies.getAdapter() == null) {
            return;
        }

        for (int index = 0; index < spinnerSpecies.getAdapter().getCount(); index++) {
            Object item = spinnerSpecies.getAdapter().getItem(index);
            if (item != null && species.equalsIgnoreCase(item.toString())) {
                spinnerSpecies.setSelection(index);
                return;
            }
        }
    }

    private void updatePostTypeUi(String postType) {
        if (PostType.isLost(postType)) {
            if (selectedLatitude != null && selectedLongitude != null) {
                textLocationStatus.setText(getString(
                        R.string.location_captured,
                        String.format(Locale.US, "%.5f", selectedLatitude),
                        String.format(Locale.US, "%.5f", selectedLongitude)
                ));
            } else {
                textLocationStatus.setText(R.string.location_not_captured);
            }
        } else {
            textLocationStatus.setText(R.string.location_optional);
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
            editLocationReference.setText(LocationUtils.resolveLocationLabel(
                    this,
                    selectedLatitude,
                    selectedLongitude
            ));
            textLocationStatus.setText(getString(
                    R.string.location_captured,
                    String.format(Locale.US, "%.5f", selectedLatitude),
                    String.format(Locale.US, "%.5f", selectedLongitude)
            ));
        } catch (SecurityException exception) {
            Toast.makeText(this, R.string.error_location_permission, Toast.LENGTH_SHORT).show();
        }
    }

    private void openMapPicker() {
        Intent intent = new Intent(this, MapPickerActivity.class);
        if (selectedLatitude != null) {
            intent.putExtra(IntentKeys.EXTRA_LATITUDE, selectedLatitude);
        }
        if (selectedLongitude != null) {
            intent.putExtra(IntentKeys.EXTRA_LONGITUDE, selectedLongitude);
        }
        mapPickerLauncher.launch(intent);
    }

    private void createPost() {
        String animalName = editAnimalName.getText().toString().trim();
        String species = spinnerSpecies.getSelectedItem().toString();
        String breed = editBreed.getText().toString().trim();
        String age = editAge.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String contactPhone = editContactPhone.getText().toString().replaceAll("\\D", "");
        String locationReference = editLocationReference.getText().toString().trim();

        if (authorUserId == null) {
            Toast.makeText(this, R.string.error_post_user_invalid, Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, R.string.error_image_required, Toast.LENGTH_SHORT).show();
            return;
        }

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

        if (!FeatureFlags.useRemoteApi(this)) {
            if (!UserRepository.isValidActiveUser(this, authorUserId)) {
                Toast.makeText(this, R.string.error_post_user_invalid, Toast.LENGTH_LONG).show();
                return;
            }

            AnimalPost animalPost = new AnimalPost(
                    editingPostId,
                    authorUserId,
                    selectedPostType,
                    animalName,
                    species,
                    breed,
                    age,
                    description,
                    contactPhone,
                    selectedLatitude,
                    selectedLongitude,
                    locationReference,
                    selectedImageUri.toString(),
                    authorName,
                    System.currentTimeMillis(),
                    false,
                    0
            );

            boolean success;
            if (editingPostId == null) {
                AnimalPostRepository.addPost(this, animalPost);
                success = true;
            } else {
                success = AnimalPostRepository.updatePost(this, animalPost);
            }

            if (!success) {
                Toast.makeText(this, R.string.error_save_post_failed, Toast.LENGTH_LONG).show();
                return;
            }

            int successMessage = editingPostId == null
                    ? (PostType.isLost(animalPost.getPostType()) ? R.string.post_lost_success : R.string.post_adoption_success)
                    : R.string.post_edit_success;
            Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        AsyncRunner.run(
                () -> editingPostId == null
                        ? ApiPostRepository.createPost(
                                this,
                                authorUserId,
                                selectedPostType,
                                animalName,
                                species,
                                breed,
                                age,
                                description,
                                contactPhone,
                                selectedLatitude,
                                selectedLongitude,
                                locationReference,
                                ImageUtils.encodeImageAsDataUrl(this, selectedImageUri)
                        )
                        : ApiPostRepository.updatePost(
                                this,
                                editingPostId,
                                authorUserId,
                                selectedPostType,
                                animalName,
                                species,
                                breed,
                                age,
                                description,
                                contactPhone,
                                selectedLatitude,
                                selectedLongitude,
                                locationReference,
                                ImageUtils.encodeImageAsDataUrl(this, selectedImageUri)
                        ),
                savedPost -> {
                    int successMessage = editingPostId == null
                            ? (PostType.isLost(savedPost.getPostType()) ? R.string.post_lost_success : R.string.post_adoption_success)
                            : R.string.post_edit_success;
                    Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                    finish();
                },
                exception -> Toast.makeText(
                        this,
                        exception.getMessage() == null ? getString(R.string.error_server_unavailable) : exception.getMessage(),
                        Toast.LENGTH_LONG
                ).show()
        );
    }
}
