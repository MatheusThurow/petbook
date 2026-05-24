package com.petbook.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.petbook.app.R;
import com.petbook.app.models.FairAnimal;

import java.util.ArrayList;
import java.util.List;

public class FairAnimalAdapter extends RecyclerView.Adapter<FairAnimalAdapter.FairAnimalViewHolder> {

    private final List<FairAnimal> items = new ArrayList<>();

    public void submitList(List<FairAnimal> fairAnimals) {
        items.clear();
        items.addAll(fairAnimals);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FairAnimalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fair_animal, parent, false);
        return new FairAnimalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FairAnimalViewHolder holder, int position) {
        FairAnimal animal = items.get(position);
        holder.textName.setText(animal.getName());
        holder.textMeta.setText(holder.itemView.getContext().getString(
                R.string.feed_post_meta,
                animal.getSpecies(),
                animal.getBreed(),
                animal.getAgeDescription()
        ));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FairAnimalViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textMeta;

        FairAnimalViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textFairAnimalName);
            textMeta = itemView.findViewById(R.id.textFairAnimalMeta);
        }
    }
}
