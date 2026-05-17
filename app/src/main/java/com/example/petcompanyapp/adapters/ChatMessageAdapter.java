package com.petbook.app.adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.petbook.app.R;
import com.petbook.app.models.ChatMessage;
import com.petbook.app.utils.ChatIdentityUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ChatMessageViewHolder> {

    private final List<ChatMessage> items = new ArrayList<>();
    private final long currentUserId;
    private final String currentUserKey;

    public ChatMessageAdapter(long currentUserId, String currentUserEmail) {
        this.currentUserId = currentUserId;
        this.currentUserKey = ChatIdentityUtils.userKeyFromEmail(currentUserEmail);
    }

    public void submitList(List<ChatMessage> messages) {
        items.clear();
        items.addAll(messages);
        notifyDataSetChanged();
    }

    public void append(ChatMessage message) {
        if (message == null) {
            return;
        }
        items.add(message);
        notifyItemInserted(items.size() - 1);
    }

    @NonNull
    @Override
    public ChatMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatMessageViewHolder holder, int position) {
        ChatMessage message = items.get(position);
        boolean isOwn = message.getSenderUserId() != null
                ? message.getSenderUserId() == currentUserId
                : currentUserKey.equals(message.getSenderUserKey());

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.container.getLayoutParams();
        params.gravity = isOwn ? Gravity.END : Gravity.START;
        holder.container.setLayoutParams(params);
        holder.container.setBackgroundResource(isOwn ? R.drawable.bg_chat_bubble_own : R.drawable.bg_chat_bubble_other);
        holder.textMessage.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                isOwn ? android.R.color.white : R.color.primary_text
        ));
        holder.textMeta.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                isOwn ? android.R.color.white : R.color.secondary_text
        ));

        holder.textMessage.setText(message.getMessageText());
        holder.textMeta.setText(holder.itemView.getContext().getString(
                R.string.chat_message_meta,
                isOwn ? holder.itemView.getContext().getString(R.string.chat_you_label) : message.getSenderName(),
                DateFormat.getTimeInstance(DateFormat.SHORT, new Locale("pt", "BR")).format(new Date(message.getSentAtMillis()))
        ));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ChatMessageViewHolder extends RecyclerView.ViewHolder {
        private final View container;
        private final TextView textMessage;
        private final TextView textMeta;

        ChatMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.layoutChatBubble);
            textMessage = itemView.findViewById(R.id.textChatMessage);
            textMeta = itemView.findViewById(R.id.textChatMeta);
        }
    }
}

