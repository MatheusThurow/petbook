package com.petbook.app.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.petbook.app.R;
import com.petbook.app.adapters.FairAnimalAdapter;
import com.petbook.app.models.AnimalPost;
import com.petbook.app.models.FairAnimal;
import com.petbook.app.repositories.AnimalPostRepository;
import com.petbook.app.repositories.FirebasePostRepository;
import com.petbook.app.utils.IntentKeys;

import java.util.List;

public class FairPostDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fair_post_detail);

        TextView textBack = findViewById(R.id.textBackFairDetail);
        TextView textTitle = findViewById(R.id.textFairDetailTitle);
        TextView textSubtitle = findViewById(R.id.textFairDetailSubtitle);
        TextView textDescription = findViewById(R.id.textFairDetailDescription);
        TextView textContact = findViewById(R.id.textFairDetailContact);
        RecyclerView recyclerAnimals = findViewById(R.id.recyclerFairAnimals);
        TextView textEmpty = findViewById(R.id.textFairAnimalsEmpty);

        textBack.setOnClickListener(v -> finish());

        long postId = getIntent().getLongExtra(IntentKeys.EXTRA_POST_ID, -1L);
        if (postId < 0) {
            finish();
            return;
        }

        List<FairAnimal> fairAnimals = AnimalPostRepository.getFairAnimalsForPost(this, postId);
        FairAnimalAdapter adapter = new FairAnimalAdapter();
        recyclerAnimals.setLayoutManager(new LinearLayoutManager(this));
        recyclerAnimals.setAdapter(adapter);

        if (FirebasePostRepository.isEnabled(this)) {
            FirebasePostRepository.getPostById(this, postId, new FirebasePostRepository.PostCallback() {
                @Override
                public void onSuccess(AnimalPost post) {
                    textTitle.setText(post.getAnimalName());
                    textSubtitle.setText(getString(
                            R.string.fair_detail_subtitle,
                            post.getAuthorName(),
                            post.getFairAnimalCount()
                    ));
                    textDescription.setText(post.getDescription());
                    textContact.setText(getString(R.string.feed_post_contact, post.getContactPhone()));
                    FirebasePostRepository.getFairAnimals(FairPostDetailActivity.this, postId, new FirebasePostRepository.FairAnimalsCallback() {
                        @Override
                        public void onSuccess(List<FairAnimal> fairAnimals) {
                            adapter.submitList(fairAnimals);
                            textEmpty.setVisibility(fairAnimals.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                        }

                        @Override
                        public void onError(String message) {
                            finish();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    finish();
                }
            });
            return;
        }

        AnimalPost post = AnimalPostRepository.findById(this, postId);
        if (post == null) {
            finish();
            return;
        }

        textTitle.setText(post.getAnimalName());
        textSubtitle.setText(getString(
                R.string.fair_detail_subtitle,
                post.getAuthorName(),
                post.getFairAnimalCount()
        ));
        textDescription.setText(post.getDescription());
        textContact.setText(getString(R.string.feed_post_contact, post.getContactPhone()));
        adapter.submitList(fairAnimals);
        textEmpty.setVisibility(fairAnimals.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}
