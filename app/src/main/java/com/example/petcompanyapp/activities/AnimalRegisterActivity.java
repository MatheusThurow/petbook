package com.petbook.app.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.petbook.app.R;
import com.petbook.app.repositories.FirebaseAnimalRepository;
import com.petbook.app.repositories.AnimalRepository;
import com.petbook.app.utils.ActionStateHelper;
import com.petbook.app.utils.IntentKeys;
import com.petbook.app.utils.MaskUtils;
import com.petbook.app.utils.FirebaseChatConfig;
import com.petbook.app.utils.UserProfileStorage;
import com.petbook.app.utils.UserType;
import com.petbook.app.utils.ValidationUtils;

public class AnimalRegisterActivity extends AppCompatActivity {

    private EditText editAnimalName;
    private Spinner spinnerSpecies;
    private EditText editBreed;
    private EditText editAge;
    private EditText editWeight;
    private Long userId;
    private String userType;
    private Button buttonRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animal_register);

        editAnimalName = findViewById(R.id.editAnimalName);
        spinnerSpecies = findViewById(R.id.spinnerSpecies);
        editBreed = findViewById(R.id.editAnimalBreed);
        editAge = findViewById(R.id.editAnimalAge);
        editWeight = findViewById(R.id.editAnimalWeight);
        TextView textBack = findViewById(R.id.textBackAnimalRegister);
        TextView textAnimalRegisterSubtitle = findViewById(R.id.textAnimalRegisterSubtitle);
        buttonRegister = findViewById(R.id.buttonAnimalRegister);

        userType = getIntent().getStringExtra(IntentKeys.EXTRA_USER_TYPE);
        if (userType == null) {
            userType = UserType.PERSON;
        }
        userId = UserProfileStorage.getUserId(this);

        textAnimalRegisterSubtitle.setText(UserType.isCompany(userType)
                ? R.string.animal_register_subtitle_company
                : R.string.animal_register_subtitle_person);

        MaskUtils.configureSpeciesSpinner(this, spinnerSpecies);

        textBack.setOnClickListener(v -> finish());
        buttonRegister.setOnClickListener(v -> registerAnimal());
    }

    private void registerAnimal() {
        String animalName = editAnimalName.getText().toString().trim();
        String species = spinnerSpecies.getSelectedItem().toString();
        String breed = editBreed.getText().toString().trim();
        String age = editAge.getText().toString().trim();
        String weight = editWeight.getText().toString().trim();

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

        if (ValidationUtils.isEmpty(weight)) {
            editWeight.setError(getString(R.string.error_required_field));
            editWeight.requestFocus();
            return;
        }

        setRegisterLoading(true);

        if (FirebaseChatConfig.isEnabled(this)) {
            FirebaseAnimalRepository.saveAnimal(
                    this,
                    userId,
                    null,
                    animalName,
                    species,
                    breed,
                    Integer.parseInt(age),
                    Double.parseDouble(weight.replace(",", ".")),
                    new FirebaseAnimalRepository.AnimalIdCallback() {
                        @Override
                        public void onSuccess(long animalId) {
                            runOnUiThread(() -> {
                                setRegisterLoading(false);
                                Toast.makeText(AnimalRegisterActivity.this, R.string.animal_register_success, Toast.LENGTH_LONG).show();
                                finish();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> {
                                setRegisterLoading(false);
                                Toast.makeText(
                                        AnimalRegisterActivity.this,
                                        message == null ? getString(R.string.error_animal_save_failed) : message,
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                        }
                    }
            );
            return;
        }

        long animalId = AnimalRepository.saveAnimal(
                this,
                userId,
                null,
                animalName,
                species,
                breed,
                Integer.parseInt(age),
                Double.parseDouble(weight.replace(",", "."))
        );
        if (animalId < 0) {
            setRegisterLoading(false);
            Toast.makeText(this, R.string.error_animal_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        setRegisterLoading(false);
        Toast.makeText(this, R.string.animal_register_success, Toast.LENGTH_LONG).show();
        finish();
    }

    private void setRegisterLoading(boolean isLoading) {
        ActionStateHelper.setEnabled(!isLoading,
                editAnimalName, spinnerSpecies, editBreed, editAge, editWeight);
        ActionStateHelper.setLoading(
                buttonRegister,
                isLoading,
                getString(R.string.button_register_animal),
                getString(R.string.action_processing)
        );
    }
}

