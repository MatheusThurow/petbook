package com.petbook.app.activities;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.petbook.app.R;
import com.petbook.app.models.AnimalPost;
import com.petbook.app.models.FairAnimal;
import com.petbook.app.repositories.AnimalPostRepository;
import com.petbook.app.repositories.ApiPostRepository;
import com.petbook.app.repositories.FirebaseNotificationRepository;
import com.petbook.app.repositories.FirebasePostRepository;
import com.petbook.app.repositories.NotificationRepository;
import com.petbook.app.repositories.UserRepository;
import com.petbook.app.utils.AgeFormatUtils;
import com.petbook.app.utils.AsyncRunner;
import com.petbook.app.utils.BackNavigationUtils;
import com.petbook.app.utils.FeatureFlags;
import com.petbook.app.utils.ImageUtils;
import com.petbook.app.utils.IntentKeys;
import com.petbook.app.utils.LocationUtils;
import com.petbook.app.utils.MaskUtils;
import com.petbook.app.utils.NotificationType;
import com.petbook.app.utils.PostType;
import com.petbook.app.utils.SessionUtils;
import com.petbook.app.utils.UserProfileStorage;
import com.petbook.app.utils.UserType;
import com.petbook.app.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostCreateActivity extends AppCompatActivity {

    private RadioGroup radioGroupPostType;
    private RadioButton radioPostFair;
    private TextInputLayout inputAnimalName;
    private EditText editAnimalName;
    private LinearLayout layoutSingleAnimalFields;
    private TextInputLayout inputBreed;
    private TextInputLayout inputAgeYears;
    private TextInputLayout inputAgeMonths;
    private Spinner spinnerSpecies;
    private EditText editBreed;
    private EditText editAgeYears;
    private EditText editAgeMonths;
    private EditText editDescription;
    private EditText editContactPhone;
    private EditText editLocationReference;
    private LinearLayout layoutLostLocation;
    private MaterialCardView layoutFairAnimalsSection;
    private LinearLayout containerFairAnimals;
    private TextView textFairAnimalsHint;
    private TextView textLocationStatus;
    private ImageView imagePostPreview;
    private Long authorUserId;
    private Long editingPostId;
    private String selectedPostType = PostType.ADOPTION;
    private String authorName;
    private String authorUserType;
    private Double selectedLatitude;
    private Double selectedLongitude;
    private Uri selectedImageUri;
    private String selectedImageValue;
    private Uri selectedFairAnimalImageUri;
    private String selectedFairAnimalImageValue;
    private ImageView currentFairAnimalPreview;
    private final List<FairAnimal> fairAnimals = new ArrayList<>();

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

                try {
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (SecurityException ignored) {
                    // Segue com a URI normalmente.
                }

                if (currentFairAnimalPreview != null) {
                    selectedFairAnimalImageUri = uri;
                    selectedFairAnimalImageValue = uri.toString();
                    ImageUtils.loadInto(this, currentFairAnimalPreview, uri);
                    return;
                }

                selectedImageUri = uri;
                selectedImageValue = uri.toString();
                ImageUtils.loadInto(this, imagePostPreview, uri);
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
        if (!SessionUtils.requireAuthenticated(this)) {
            return;
        }
        setContentView(R.layout.activity_post_create);

        radioGroupPostType = findViewById(R.id.radioGroupPostType);
        radioPostFair = findViewById(R.id.radioPostFair);
        inputAnimalName = findViewById(R.id.inputPostAnimalName);
        editAnimalName = findViewById(R.id.editPostAnimalName);
        layoutSingleAnimalFields = findViewById(R.id.layoutSingleAnimalFields);
        inputBreed = findViewById(R.id.inputPostBreed);
        inputAgeYears = findViewById(R.id.inputPostAgeYears);
        inputAgeMonths = findViewById(R.id.inputPostAgeMonths);
        spinnerSpecies = findViewById(R.id.spinnerPostSpecies);
        editBreed = findViewById(R.id.editPostBreed);
        editAgeYears = findViewById(R.id.editPostAgeYears);
        editAgeMonths = findViewById(R.id.editPostAgeMonths);
        editDescription = findViewById(R.id.editPostDescription);
        editContactPhone = findViewById(R.id.editPostContactPhone);
        editLocationReference = findViewById(R.id.editLostLocationReference);
        layoutLostLocation = findViewById(R.id.layoutLostLocation);
        layoutFairAnimalsSection = findViewById(R.id.layoutFairAnimalsSection);
        containerFairAnimals = findViewById(R.id.containerFairAnimals);
        textFairAnimalsHint = findViewById(R.id.textFairAnimalsHint);
        textLocationStatus = findViewById(R.id.textLocationStatus);
        imagePostPreview = findViewById(R.id.imagePostPreview);
        TextView textBack = findViewById(R.id.textBackPostCreate);
        Button buttonSelectImage = findViewById(R.id.buttonSelectImage);
        Button buttonCaptureLocation = findViewById(R.id.buttonCaptureLocation);
        Button buttonSelectLocationOnMap = findViewById(R.id.buttonSelectLocationOnMap);
        Button buttonAddFairAnimal = findViewById(R.id.buttonAddFairAnimal);
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
        authorUserType = getIntent().getStringExtra(IntentKeys.EXTRA_USER_TYPE);
        if (authorUserType == null || authorUserType.trim().isEmpty()) {
            authorUserType = UserProfileStorage.getUserType(this, UserType.PERSON);
        }
        editingPostId = getIntent().hasExtra(IntentKeys.EXTRA_POST_ID)
                ? getIntent().getLongExtra(IntentKeys.EXTRA_POST_ID, -1L)
                : null;
        if (editingPostId != null && editingPostId < 0) {
            editingPostId = null;
        }

        if (UserType.isCompany(authorUserType)) {
            radioPostFair.setVisibility(View.VISIBLE);
        } else {
            radioPostFair.setVisibility(View.GONE);
        }

        bindEditingData(buttonCreatePost);
        updatePostTypeUi(selectedPostType);

        radioGroupPostType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioPostLost) {
                selectedPostType = PostType.LOST;
            } else if (checkedId == R.id.radioPostFair) {
                selectedPostType = PostType.FAIR;
            } else {
                selectedPostType = PostType.ADOPTION;
            }
            updatePostTypeUi(selectedPostType);
        });

        BackNavigationUtils.bind(this, textBack);
        buttonCaptureLocation.setOnClickListener(v -> requestLocation());
        buttonSelectLocationOnMap.setOnClickListener(v -> openMapPicker());
        buttonSelectImage.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));
        buttonAddFairAnimal.setOnClickListener(v -> showFairAnimalDialog(null, -1));
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

        if (PostType.isLost(selectedPostType)) {
            radioGroupPostType.check(R.id.radioPostLost);
        } else if (PostType.isFair(selectedPostType) && UserType.isCompany(authorUserType)) {
            radioGroupPostType.check(R.id.radioPostFair);
        } else {
            radioGroupPostType.check(R.id.radioPostAdoption);
        }

        editAnimalName.setText(getIntent().getStringExtra(IntentKeys.EXTRA_POST_ANIMAL_NAME));
        setSpinnerSelection(getIntent().getStringExtra(IntentKeys.EXTRA_POST_SPECIES));
        editBreed.setText(getIntent().getStringExtra(IntentKeys.EXTRA_POST_BREED));
        populateAgeFields(getIntent().getStringExtra(IntentKeys.EXTRA_POST_AGE), editAgeYears, editAgeMonths);
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
            selectedImageValue = imageUri;
            if (!imageUri.startsWith("data:image")) {
                selectedImageUri = Uri.parse(imageUri);
                ImageUtils.loadInto(this, imagePostPreview, selectedImageUri);
            } else {
                ImageUtils.loadInto(imagePostPreview, imageUri);
            }
        }

        if (PostType.isFair(selectedPostType)) {
            if (FirebasePostRepository.isEnabled(this)) {
                FirebasePostRepository.getFairAnimals(this, editingPostId, new FirebasePostRepository.FairAnimalsCallback() {
                    @Override
                    public void onSuccess(List<FairAnimal> loadedFairAnimals) {
                        fairAnimals.clear();
                        fairAnimals.addAll(loadedFairAnimals);
                        renderFairAnimals();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(PostCreateActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (!FeatureFlags.useRemoteApi(this)) {
                fairAnimals.clear();
                fairAnimals.addAll(AnimalPostRepository.getFairAnimalsForPost(this, editingPostId));
                renderFairAnimals();
            }
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
        boolean isLost = PostType.isLost(postType);
        boolean isFair = PostType.isFair(postType);

        layoutLostLocation.setVisibility(isLost ? View.VISIBLE : View.GONE);
        layoutFairAnimalsSection.setVisibility(isFair ? View.VISIBLE : View.GONE);
        layoutSingleAnimalFields.setVisibility(isFair ? View.GONE : View.VISIBLE);
        inputAnimalName.setHint(isFair
                ? getString(R.string.hint_fair_title)
                : getString(R.string.hint_animal_name));

        if (isLost) {
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

        if (isFair) {
            renderFairAnimals();
        }
    }

    private void renderFairAnimals() {
        containerFairAnimals.removeAllViews();

        if (fairAnimals.isEmpty()) {
            textFairAnimalsHint.setText(R.string.fair_animals_hint);
            return;
        }

        textFairAnimalsHint.setText(getString(R.string.fair_animals_count, fairAnimals.size()));
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int index = 0; index < fairAnimals.size(); index++) {
            FairAnimal fairAnimal = fairAnimals.get(index);
            View itemView = inflater.inflate(R.layout.item_fair_animal_editor, containerFairAnimals, false);

            TextView textName = itemView.findViewById(R.id.textFairAnimalEditorName);
            TextView textMeta = itemView.findViewById(R.id.textFairAnimalEditorMeta);
            TextView textEdit = itemView.findViewById(R.id.textFairAnimalEditorEdit);
            TextView textRemove = itemView.findViewById(R.id.textFairAnimalEditorRemove);
            ImageView imageAnimal = itemView.findViewById(R.id.imageFairAnimalEditor);

            textName.setText(fairAnimal.getName());
            textMeta.setText(getString(
                    R.string.feed_post_meta,
                    fairAnimal.getSpecies(),
                    fairAnimal.getBreed(),
                    fairAnimal.getAgeDescription()
            ));
            ImageUtils.loadInto(imageAnimal, fairAnimal.getImageUri());

            final int itemIndex = index;
            textEdit.setOnClickListener(v -> showFairAnimalDialog(fairAnimal, itemIndex));
            textRemove.setOnClickListener(v -> {
                fairAnimals.remove(itemIndex);
                renderFairAnimals();
            });

            containerFairAnimals.addView(itemView);
        }
    }

    private void showFairAnimalDialog(FairAnimal existingAnimal, int editIndex) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_fair_animal, null, false);
        TextView textDialogTitle = dialogView.findViewById(R.id.textFairAnimalDialogTitle);
        TextInputEditText editName = dialogView.findViewById(R.id.editFairAnimalName);
        Spinner spinnerDialogSpecies = dialogView.findViewById(R.id.spinnerFairAnimalSpecies);
        TextInputEditText editBreed = dialogView.findViewById(R.id.editFairAnimalBreed);
        TextInputEditText editAgeYears = dialogView.findViewById(R.id.editFairAnimalAgeYears);
        TextInputEditText editAgeMonths = dialogView.findViewById(R.id.editFairAnimalAgeMonths);
        ImageView imagePreview = dialogView.findViewById(R.id.imageFairAnimalPreview);
        Button buttonSelectImage = dialogView.findViewById(R.id.buttonSelectFairAnimalImage);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancelFairAnimal);
        Button buttonSave = dialogView.findViewById(R.id.buttonSaveFairAnimal);

        MaskUtils.configureSpeciesSpinner(this, spinnerDialogSpecies);
        selectedFairAnimalImageUri = null;
        selectedFairAnimalImageValue = existingAnimal == null ? "" : existingAnimal.getImageUri();
        currentFairAnimalPreview = imagePreview;
        ImageUtils.loadInto(imagePreview, selectedFairAnimalImageValue);
        textDialogTitle.setText(existingAnimal == null ? R.string.dialog_add_fair_animal : R.string.dialog_edit_fair_animal);

        if (existingAnimal != null) {
            editName.setText(existingAnimal.getName());
            editBreed.setText(existingAnimal.getBreed());
            populateAgeFields(existingAnimal.getAgeDescription(), editAgeYears, editAgeMonths);

            for (int index = 0; index < spinnerDialogSpecies.getAdapter().getCount(); index++) {
                Object item = spinnerDialogSpecies.getAdapter().getItem(index);
                if (item != null && existingAnimal.getSpecies().equalsIgnoreCase(item.toString())) {
                    spinnerDialogSpecies.setSelection(index);
                    break;
                }
            }
        }

        buttonSelectImage.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonSave.setOnClickListener(v -> {
            String animalName = String.valueOf(editName.getText()).trim();
            String species = String.valueOf(spinnerDialogSpecies.getSelectedItem());
            String breed = String.valueOf(editBreed.getText()).trim();
            String yearsValue = String.valueOf(editAgeYears.getText()).trim();
            String monthsValue = String.valueOf(editAgeMonths.getText()).trim();

            if (ValidationUtils.isEmpty(animalName)) {
                editName.setError(getString(R.string.error_required_field));
                return;
            }
            if (species.equals(getString(R.string.species_hint))) {
                Toast.makeText(this, R.string.error_species_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (ValidationUtils.isEmpty(breed)) {
                editBreed.setError(getString(R.string.error_required_field));
                return;
            }
            if (!AgeFormatUtils.hasAnyValue(yearsValue, monthsValue)) {
                editAgeYears.setError(getString(R.string.error_fair_animal_age_required));
                editAgeYears.requestFocus();
                return;
            }

            int years = AgeFormatUtils.parseYears(yearsValue);
            int months = AgeFormatUtils.parseMonths(monthsValue);
            if (!AgeFormatUtils.isValidMonths(months)) {
                editAgeMonths.setError(getString(R.string.error_fair_animal_age_months_invalid));
                editAgeMonths.requestFocus();
                return;
            }

            String age = AgeFormatUtils.formatAge(years, months);
            if ((selectedFairAnimalImageValue == null || selectedFairAnimalImageValue.trim().isEmpty()) && selectedFairAnimalImageUri == null) {
                Toast.makeText(this, R.string.error_image_required, Toast.LENGTH_SHORT).show();
                return;
            }

            String imageValue = selectedFairAnimalImageValue;
            if (selectedFairAnimalImageUri != null) {
                try {
                    imageValue = ImageUtils.encodeImageAsDataUrl(this, selectedFairAnimalImageUri);
                } catch (Exception exception) {
                    Toast.makeText(this, R.string.error_save_post_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            FairAnimal fairAnimal = new FairAnimal(
                    existingAnimal == null ? null : existingAnimal.getId(),
                    existingAnimal == null ? null : existingAnimal.getPostId(),
                    animalName,
                    species,
                    breed,
                    age,
                    imageValue
            );

            if (editIndex >= 0) {
                fairAnimals.set(editIndex, fairAnimal);
            } else {
                fairAnimals.add(fairAnimal);
            }
            renderFairAnimals();
            clearFairAnimalSelectionState();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(dialogInterface -> clearFairAnimalSelectionState());
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void populateAgeFields(
            String ageDescription,
            EditText editAgeYears,
            EditText editAgeMonths
    ) {
        AgeFormatUtils.AgeValue ageValue = AgeFormatUtils.parseAgeDescription(ageDescription);
        if (ageValue.getYears() > 0) {
            editAgeYears.setText(String.valueOf(ageValue.getYears()));
        }
        if (ageValue.getMonths() > 0) {
            editAgeMonths.setText(String.valueOf(ageValue.getMonths()));
        }
    }

    private void clearFairAnimalSelectionState() {
        selectedFairAnimalImageUri = null;
        selectedFairAnimalImageValue = null;
        currentFairAnimalPreview = null;
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
        String postTitle = editAnimalName.getText().toString().trim();
        String species = spinnerSpecies.getSelectedItem().toString();
        String breed = editBreed.getText().toString().trim();
        String ageYearsValue = editAgeYears.getText().toString().trim();
        String ageMonthsValue = editAgeMonths.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String contactPhone = editContactPhone.getText().toString().replaceAll("\\D", "");
        String locationReference = editLocationReference.getText().toString().trim();
        boolean isLost = PostType.isLost(selectedPostType);
        boolean isFair = PostType.isFair(selectedPostType);

        if (authorUserId == null) {
            Toast.makeText(this, R.string.error_post_user_invalid, Toast.LENGTH_LONG).show();
            return;
        }

        if (isFair && !UserType.isCompany(authorUserType)) {
            Toast.makeText(this, R.string.error_fair_company_only, Toast.LENGTH_LONG).show();
            return;
        }

        if (FeatureFlags.useRemoteApi(this) && isFair) {
            Toast.makeText(this, R.string.error_fair_remote_unavailable, Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, R.string.error_image_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (ValidationUtils.isEmpty(postTitle)) {
            editAnimalName.setError(getString(R.string.error_required_field));
            editAnimalName.requestFocus();
            return;
        }

        if (!isFair && species.equals(getString(R.string.species_hint))) {
            Toast.makeText(this, R.string.error_species_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isFair && ValidationUtils.isEmpty(breed)) {
            editBreed.setError(getString(R.string.error_required_field));
            editBreed.requestFocus();
            return;
        }

        if (!isFair && !AgeFormatUtils.hasAnyValue(ageYearsValue, ageMonthsValue)) {
            editAgeYears.setError(getString(R.string.error_age_required));
            editAgeYears.requestFocus();
            return;
        }

        int ageYears = AgeFormatUtils.parseYears(ageYearsValue);
        int ageMonths = AgeFormatUtils.parseMonths(ageMonthsValue);
        if (!isFair && !AgeFormatUtils.isValidMonths(ageMonths)) {
            editAgeMonths.setError(getString(R.string.error_age_months_invalid));
            editAgeMonths.requestFocus();
            return;
        }

        String age = isFair ? "" : AgeFormatUtils.formatAge(ageYears, ageMonths);

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

        if (isLost) {
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

        if (isFair && fairAnimals.isEmpty()) {
            Toast.makeText(this, R.string.error_fair_animals_required, Toast.LENGTH_LONG).show();
            return;
        }

        if (!FeatureFlags.useRemoteApi(this)) {
            if (FirebasePostRepository.isEnabled(this)) {
                savePostWithFirebase(postTitle, species, breed, age, description, contactPhone, locationReference, isLost, isFair);
                return;
            }

            if (!UserRepository.isValidActiveUser(this, authorUserId)) {
                Toast.makeText(this, R.string.error_post_user_invalid, Toast.LENGTH_LONG).show();
                return;
            }

            AnimalPost animalPost = new AnimalPost(
                    editingPostId,
                    authorUserId,
                    selectedPostType,
                    postTitle,
                    isFair ? getString(R.string.fair_species_label) : species,
                    isFair ? getString(R.string.fair_breed_label) : breed,
                    isFair ? getString(R.string.fair_animals_count_short, fairAnimals.size()) : age,
                    description,
                    contactPhone,
                    isLost ? selectedLatitude : null,
                    isLost ? selectedLongitude : null,
                    isLost ? locationReference : "",
                    selectedImageUri.toString(),
                    authorName,
                    UserProfileStorage.getEmail(this, ""),
                    System.currentTimeMillis(),
                    fairAnimals.size()
            );

            boolean success = editingPostId == null
                    ? AnimalPostRepository.addPost(this, animalPost, fairAnimals)
                    : AnimalPostRepository.updatePost(this, animalPost, fairAnimals);

            if (!success) {
                Toast.makeText(this, R.string.error_save_post_failed, Toast.LENGTH_LONG).show();
                return;
            }

            if (editingPostId != null) {
                NotificationRepository.addNotification(
                        this,
                        authorUserId,
                        NotificationType.POST_UPDATED,
                        getString(R.string.notification_post_updated_title),
                        getString(R.string.notification_post_updated_message, postTitle),
                        editingPostId,
                        selectedPostType,
                        authorUserId,
                        authorName,
                        UserProfileStorage.getEmail(this, ""),
                        null
                );
            }

            int successMessage = editingPostId == null
                    ? resolveSuccessMessage(selectedPostType)
                    : R.string.post_edit_success;
            Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
            BackNavigationUtils.navigateBack(this);
            return;
        }

        AsyncRunner.run(
                () -> editingPostId == null
                        ? ApiPostRepository.createPost(
                                this,
                                authorUserId,
                                selectedPostType,
                                postTitle,
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
                                postTitle,
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
                            ? resolveSuccessMessage(savedPost.getPostType())
                            : R.string.post_edit_success;
                    Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                    BackNavigationUtils.navigateBack(this);
                },
                exception -> Toast.makeText(
                        this,
                        exception.getMessage() == null ? getString(R.string.error_server_unavailable) : exception.getMessage(),
                        Toast.LENGTH_LONG
                ).show()
        );
    }

    private int resolveSuccessMessage(String postType) {
        if (PostType.isLost(postType)) {
            return R.string.post_lost_success;
        }
        if (PostType.isFair(postType)) {
            return R.string.post_fair_success;
        }
        return R.string.post_adoption_success;
    }

    private void savePostWithFirebase(
            String postTitle,
            String species,
            String breed,
            String age,
            String description,
            String contactPhone,
            String locationReference,
            boolean isLost,
            boolean isFair
    ) {
        long postId = editingPostId == null ? System.currentTimeMillis() : editingPostId;
        String imageValue = selectedImageValue;
        if (imageValue == null || imageValue.trim().isEmpty()) {
            if (selectedImageUri == null) {
                Toast.makeText(this, R.string.error_image_required, Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                imageValue = ImageUtils.encodeImageAsDataUrl(this, selectedImageUri);
            } catch (Exception exception) {
                Toast.makeText(this, R.string.error_save_post_failed, Toast.LENGTH_LONG).show();
                return;
            }
        } else if (!imageValue.startsWith("data:image")) {
            try {
                imageValue = ImageUtils.encodeImageAsDataUrl(this, selectedImageUri);
            } catch (Exception exception) {
                Toast.makeText(this, R.string.error_save_post_failed, Toast.LENGTH_LONG).show();
                return;
            }
        }

        AnimalPost animalPost = new AnimalPost(
                postId,
                authorUserId,
                selectedPostType,
                postTitle,
                isFair ? getString(R.string.fair_species_label) : species,
                isFair ? getString(R.string.fair_breed_label) : breed,
                isFair ? getString(R.string.fair_animals_count_short, fairAnimals.size()) : age,
                description,
                contactPhone,
                isLost ? selectedLatitude : null,
                isLost ? selectedLongitude : null,
                isLost ? locationReference : "",
                imageValue,
                authorName,
                UserProfileStorage.getEmail(this, ""),
                editingPostId == null ? System.currentTimeMillis() : System.currentTimeMillis(),
                fairAnimals.size()
        );

        FirebasePostRepository.savePost(
                this,
                animalPost,
                fairAnimals,
                new FirebasePostRepository.PostCallback() {
                    @Override
                    public void onSuccess(AnimalPost post) {
                        if (editingPostId != null) {
                            FirebaseNotificationRepository.addNotification(
                                    PostCreateActivity.this,
                                    UserProfileStorage.getEmail(PostCreateActivity.this, ""),
                                    NotificationType.POST_UPDATED,
                                    getString(R.string.notification_post_updated_title),
                                    getString(R.string.notification_post_updated_message, postTitle),
                                    post.getId(),
                                    post.getPostType(),
                                    authorUserId,
                                    authorName,
                                    UserProfileStorage.getEmail(PostCreateActivity.this, ""),
                                    null
                            );
                        }
                        Toast.makeText(
                                PostCreateActivity.this,
                                editingPostId == null ? resolveSuccessMessage(post.getPostType()) : R.string.post_edit_success,
                                Toast.LENGTH_LONG
                        ).show();
                        BackNavigationUtils.navigateBack(PostCreateActivity.this);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(
                                PostCreateActivity.this,
                                message == null ? getString(R.string.error_save_post_failed) : message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }
}
