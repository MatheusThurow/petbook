package com.example.petcompanyapp.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcompanyapp.R;
import com.example.petcompanyapp.repositories.AnimalRepository;
import com.example.petcompanyapp.utils.IntentKeys;
import com.example.petcompanyapp.utils.MaskUtils;
import com.example.petcompanyapp.utils.UserProfileStorage;
import com.example.petcompanyapp.utils.UserType;
import com.example.petcompanyapp.utils.ValidationUtils;

public class AnimalRegisterActivity extends AppCompatActivity {

    private EditText editAnimalName;
    private Spinner spinnerSpecies;
    private EditText editBreed;
    private EditText editAge;
    private EditText editWeight;
    private Long userId;
    private String userType;

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
        Button buttonRegister = findViewById(R.id.buttonAnimalRegister);

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
            Toast.makeText(this, R.string.error_animal_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.animal_register_success, Toast.LENGTH_LONG).show();
        finish();
    }
}
