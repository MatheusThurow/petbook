package com.petbook.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.petbook.app.R;
import com.petbook.app.models.ConversationSummary;
import com.petbook.app.utils.UserType;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClicked(ConversationSummary conversation);
    }

    private final List<ConversationSummary> items = new ArrayList<>();
    private final OnConversationClickListener listener;

    public ConversationAdapter(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ConversationSummary> conversations) {
        items.clear();
        items.addAll(conversations);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        ConversationSummary conversation = items.get(position);
        holder.textName.setText(conversation.getPartnerName());
        holder.textSubtitle.setText(holder.itemView.getContext().getString(
                R.string.chat_partner_type,
                getUserTypeLabel(conversation.getPartnerType(), holder.itemView)
        ));
        holder.textPreview.setText(
                conversation.getLastMessageText() == null || conversation.getLastMessageText().trim().isEmpty()
                        ? holder.itemView.getContext().getString(R.string.chat_no_messages_yet)
                        : conversation.getLastMessageText()
        );
        holder.textDate.setText(DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT,
                new Locale("pt", "BR")
        ).format(new Date(conversation.getLastMessageAtMillis())));

        if (conversation.getUnreadCount() > 0) {
            holder.textUnread.setVisibility(View.VISIBLE);
            holder.textUnread.setText(String.valueOf(conversation.getUnreadCount()));
        } else {
            holder.textUnread.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onConversationClicked(conversation));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String getUserTypeLabel(String userType, View itemView) {
        return UserType.isCompany(userType)
                ? itemView.getContext().getString(R.string.user_type_company)
                : itemView.getContext().getString(R.string.user_type_person);
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textSubtitle;
        private final TextView textPreview;
        private final TextView textDate;
        private final TextView textUnread;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textConversationName);
            textSubtitle = itemView.findViewById(R.id.textConversationSubtitle);
            textPreview = itemView.findViewById(R.id.textConversationPreview);
            textDate = itemView.findViewById(R.id.textConversationDate);
            textUnread = itemView.findViewById(R.id.textConversationUnread);
        }
    }
}

