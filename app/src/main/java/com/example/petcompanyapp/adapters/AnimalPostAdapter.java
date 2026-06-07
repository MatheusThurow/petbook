package com.petbook.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.petbook.app.R;
import com.petbook.app.models.AnimalPost;
import com.petbook.app.utils.ImageUtils;
import com.petbook.app.utils.PostType;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnimalPostAdapter extends RecyclerView.Adapter<AnimalPostAdapter.PostViewHolder> {

    public interface OnPostActionListener {
        void onShareClicked(AnimalPost post);
        void onMapClicked(AnimalPost post);
        void onCommentClicked(AnimalPost post);
        void onInterestClicked(AnimalPost post);
        void onEditClicked(AnimalPost post);
        void onDeleteClicked(AnimalPost post);
    }

    private final List<AnimalPost> items = new ArrayList<>();
    private final OnPostActionListener listener;
    private Long currentUserId;

    public AnimalPostAdapter(OnPostActionListener listener, Long currentUserId) {
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    public void submitList(List<AnimalPost> posts) {
        items.clear();
        items.addAll(posts);
        notifyDataSetChanged();
    }

    public void setCurrentUserId(Long currentUserId) {
        this.currentUserId = currentUserId;
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
        boolean isFair = PostType.isFair(post.getPostType());

        holder.textBadge.setText(isLost
                ? holder.itemView.getContext().getString(R.string.post_type_lost)
                : (isFair
                ? holder.itemView.getContext().getString(R.string.post_type_fair)
                : holder.itemView.getContext().getString(R.string.post_type_adoption)));
        holder.textBadge.setBackgroundResource(isLost
                ? R.drawable.bg_badge_lost
                : (isFair ? R.drawable.bg_badge_fair : R.drawable.bg_badge_adoption));
        ImageUtils.loadInto(holder.imagePost, post.getImageUri());
        holder.textAnimalName.setText(post.getAnimalName());
        if (isFair) {
            holder.textMeta.setText(holder.itemView.getContext().getString(
                    R.string.feed_post_fair_meta,
                    post.getFairAnimalCount()
            ));
        } else {
            holder.textMeta.setText(holder.itemView.getContext().getString(
                    R.string.feed_post_meta,
                    post.getSpecies(),
                    post.getBreed(),
                    post.getAge()
            ));
        }
        holder.textDescription.setText(post.getDescription());
        holder.textContact.setText(holder.itemView.getContext().getString(
                R.string.feed_post_contact,
                post.getContactPhone()
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

        boolean isOwner = currentUserId != null
                && post.getAuthorUserId() != null
                && currentUserId.longValue() == post.getAuthorUserId().longValue();
        holder.layoutOwnerActions.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        holder.textInterest.setVisibility(!isOwner && PostType.isAdoptionRelated(post.getPostType()) ? View.VISIBLE : View.GONE);
        holder.textComment.setText(isOwner
                ? holder.itemView.getContext().getString(R.string.action_comments)
                : (isLost
                ? holder.itemView.getContext().getString(R.string.action_comment)
                : holder.itemView.getContext().getString(R.string.action_contact_owner)));
        holder.textComment.setBackgroundResource(R.drawable.bg_post_primary_action);
        holder.textComment.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                R.color.primary_dark
        ));

        if (isLost) {
            holder.textLocation.setVisibility(View.VISIBLE);
            holder.buttonMap.setVisibility(View.VISIBLE);
            holder.textLocation.setText(holder.itemView.getContext().getString(
                    R.string.feed_post_location,
                    post.getLocationReference()
            ));
            holder.buttonMap.setText(R.string.action_view_more_location);
        } else if (isFair) {
            holder.textLocation.setVisibility(View.VISIBLE);
            holder.buttonMap.setVisibility(View.VISIBLE);
            holder.textLocation.setText(holder.itemView.getContext().getString(
                    R.string.feed_post_fair_animals,
                    post.getFairAnimalCount()
            ));
            holder.buttonMap.setText(R.string.action_view_fair_animals);
        } else {
            holder.textLocation.setVisibility(View.GONE);
            holder.buttonMap.setVisibility(View.GONE);
        }

        holder.textShare.setOnClickListener(v -> listener.onShareClicked(post));
        holder.buttonMap.setOnClickListener(v -> listener.onMapClicked(post));
        holder.textComment.setOnClickListener(v -> listener.onCommentClicked(post));
        holder.textInterest.setOnClickListener(v -> listener.onInterestClicked(post));
        holder.textEdit.setOnClickListener(v -> listener.onEditClicked(post));
        holder.textDelete.setOnClickListener(v -> listener.onDeleteClicked(post));
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
        private final TextView textShare;
        private final TextView textComment;
        private final TextView textInterest;
        private final TextView textAuthor;
        private final TextView textDate;
        private final TextView buttonMap;
        private final View layoutOwnerActions;
        private final TextView textEdit;
        private final TextView textDelete;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            textBadge = itemView.findViewById(R.id.textPostBadge);
            imagePost = itemView.findViewById(R.id.imagePost);
            textAnimalName = itemView.findViewById(R.id.textPostAnimalName);
            textMeta = itemView.findViewById(R.id.textPostMeta);
            textDescription = itemView.findViewById(R.id.textPostDescription);
            textLocation = itemView.findViewById(R.id.textPostLocation);
            textContact = itemView.findViewById(R.id.textPostContact);
            textShare = itemView.findViewById(R.id.textPostShare);
            textComment = itemView.findViewById(R.id.textPostComment);
            textInterest = itemView.findViewById(R.id.textPostInterest);
            textAuthor = itemView.findViewById(R.id.textPostAuthor);
            textDate = itemView.findViewById(R.id.textPostDate);
            buttonMap = itemView.findViewById(R.id.textPostMap);
            layoutOwnerActions = itemView.findViewById(R.id.layoutPostOwnerActions);
            textEdit = itemView.findViewById(R.id.textPostEdit);
            textDelete = itemView.findViewById(R.id.textPostDelete);
        }
    }
}
