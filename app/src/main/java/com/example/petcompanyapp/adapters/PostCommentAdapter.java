package com.petbook.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.petbook.app.R;
import com.petbook.app.models.PostComment;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostCommentAdapter extends RecyclerView.Adapter<PostCommentAdapter.CommentViewHolder> {

    public interface OnCommentReplyListener {
        void onReplyClicked(PostComment comment);
    }

    private final List<PostComment> items = new ArrayList<>();
    private final OnCommentReplyListener listener;

    public PostCommentAdapter(OnCommentReplyListener listener) {
        this.listener = listener;
    }

    public void submitList(List<PostComment> comments) {
        items.clear();
        items.addAll(comments);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        PostComment comment = items.get(position);
        holder.textAuthor.setText(comment.getAuthorName());
        holder.textAvatar.setText(buildInitials(comment.getAuthorName()));
        holder.textMessage.setText(comment.getMessage());
        holder.textLike.setText("♡ 0");
        holder.textLike.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_text));
        holder.textReply.setOnClickListener(v -> listener.onReplyClicked(comment));
        if (comment.isReply()) {
            String parentAuthorName = comment.getParentAuthorName();
            if (parentAuthorName == null || parentAuthorName.trim().isEmpty()) {
                holder.textReplyingLabel.setText(R.string.comments_reply_label);
            } else {
                holder.textReplyingLabel.setText(
                        holder.itemView.getContext().getString(R.string.comments_reply_to_label, parentAuthorName)
                );
            }
            holder.textReplyingLabel.setVisibility(View.VISIBLE);
        } else {
            holder.textReplyingLabel.setVisibility(View.GONE);
        }
        holder.itemView.setPadding(
                dp(holder.itemView, 12 + (comment.getDepth() * 20)),
                holder.itemView.getPaddingTop(),
                dp(holder.itemView, 12),
                holder.itemView.getPaddingBottom()
        );
        holder.textDate.setText(DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT,
                new Locale("pt", "BR")
        ).format(new Date(comment.getCreatedAtMillis())));

        int replyCount = countReplies(position, comment.getDepth());
        if (comment.getDepth() == 0 && replyCount > 0) {
            holder.textRepliesCount.setText(
                    holder.itemView.getContext().getResources().getQuantityString(
                            R.plurals.comments_replies_count,
                            replyCount,
                            replyCount
                    )
            );
            holder.textRepliesCount.setVisibility(View.VISIBLE);
        } else {
            holder.textRepliesCount.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final TextView textAvatar;
        private final TextView textAuthor;
        private final TextView textMessage;
        private final TextView textDate;
        private final TextView textLike;
        private final TextView textReply;
        private final TextView textReplyingLabel;
        private final TextView textRepliesCount;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            textAvatar = itemView.findViewById(R.id.textCommentAvatar);
            textAuthor = itemView.findViewById(R.id.textCommentAuthor);
            textMessage = itemView.findViewById(R.id.textCommentMessage);
            textDate = itemView.findViewById(R.id.textCommentDate);
            textLike = itemView.findViewById(R.id.textCommentLike);
            textReply = itemView.findViewById(R.id.textCommentReply);
            textReplyingLabel = itemView.findViewById(R.id.textCommentReplyingLabel);
            textRepliesCount = itemView.findViewById(R.id.textCommentRepliesCount);
        }
    }

    private int dp(View view, int value) {
        float density = view.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private int countReplies(int position, int depth) {
        int count = 0;
        for (int index = position + 1; index < items.size(); index++) {
            PostComment nextComment = items.get(index);
            if (nextComment.getDepth() <= depth) {
                break;
            }
            count++;
        }
        return count;
    }

    private String buildInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

}
