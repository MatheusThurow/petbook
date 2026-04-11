package com.example.petcompanyapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcompanyapp.R;
import com.example.petcompanyapp.models.AnimalPost;
import com.example.petcompanyapp.utils.ImageUtils;
import com.example.petcompanyapp.utils.PostType;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnimalPostAdapter extends RecyclerView.Adapter<AnimalPostAdapter.PostViewHolder> {

    public interface OnPostActionListener {
        void onLikeClicked(AnimalPost post);
        void onShareClicked(AnimalPost post);
        void onMapClicked(AnimalPost post);
    }

    private final List<AnimalPost> items = new ArrayList<>();
    private final OnPostActionListener listener;

    public AnimalPostAdapter(OnPostActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AnimalPost> posts) {
        items.clear();
        items.addAll(posts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        AnimalPost post = items.get(position);
        boolean isLost = PostType.isLost(post.getPostType());

        holder.textBadge.setText(isLost
                ? holder.itemView.getContext().getString(R.string.post_type_lost)
                : holder.itemView.getContext().getString(R.string.post_type_adoption));
        holder.textBadge.setBackgroundResource(isLost ? R.drawable.bg_badge_lost : R.drawable.bg_badge_adoption);
        ImageUtils.loadInto(holder.imagePost, post.getImageUri());
        holder.textAnimalName.setText(post.getAnimalName());
        holder.textMeta.setText(holder.itemView.getContext().getString(
                R.string.feed_post_meta,
                post.getSpecies(),
                post.getBreed(),
                post.getAge()
        ));
        holder.textDescription.setText(post.getDescription());
        holder.textContact.setText(holder.itemView.getContext().getString(
                R.string.feed_post_contact,
                post.getContactPhone()
        ));
        holder.textLike.setText(holder.itemView.getContext().getString(
                post.isLiked() ? R.string.action_liked : R.string.action_like,
                post.getLikeCount()
        ));
        holder.textAuthor.setText(holder.itemView.getContext().getString(
                R.string.feed_post_author,
                post.getAuthorName()
        ));

        String formattedDate = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT,
                new Locale("pt", "BR")
        ).format(new Date(post.getCreatedAtMillis()));
        holder.textDate.setText(formattedDate);

        if (isLost) {
            holder.textLocation.setVisibility(View.VISIBLE);
            holder.buttonMap.setVisibility(View.VISIBLE);
            holder.textLocation.setText(holder.itemView.getContext().getString(
                    R.string.feed_post_location,
                    post.getLocationReference()
            ));
            holder.buttonMap.setText(R.string.action_view_more_location);
        } else {
            holder.textLocation.setVisibility(View.GONE);
            holder.buttonMap.setVisibility(View.GONE);
        }

        holder.textLike.setOnClickListener(v -> listener.onLikeClicked(post));
        holder.textShare.setOnClickListener(v -> listener.onShareClicked(post));
        holder.buttonMap.setOnClickListener(v -> listener.onMapClicked(post));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        private final TextView textBadge;
        private final ImageView imagePost;
        private final TextView textAnimalName;
        private final TextView textMeta;
        private final TextView textDescription;
        private final TextView textLocation;
        private final TextView textContact;
        private final TextView textLike;
        private final TextView textShare;
        private final TextView textAuthor;
        private final TextView textDate;
        private final TextView buttonMap;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            textBadge = itemView.findViewById(R.id.textPostBadge);
            imagePost = itemView.findViewById(R.id.imagePost);
            textAnimalName = itemView.findViewById(R.id.textPostAnimalName);
            textMeta = itemView.findViewById(R.id.textPostMeta);
            textDescription = itemView.findViewById(R.id.textPostDescription);
            textLocation = itemView.findViewById(R.id.textPostLocation);
            textContact = itemView.findViewById(R.id.textPostContact);
            textLike = itemView.findViewById(R.id.textPostLike);
            textShare = itemView.findViewById(R.id.textPostShare);
            textAuthor = itemView.findViewById(R.id.textPostAuthor);
            textDate = itemView.findViewById(R.id.textPostDate);
            buttonMap = itemView.findViewById(R.id.textPostMap);
        }
    }
}
