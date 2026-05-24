package com.petbook.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.petbook.app.R;
import com.petbook.app.models.AppNotification;
import com.petbook.app.utils.NotificationType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnNotificationActionListener {
        void onNotificationClicked(AppNotification notification);
        void onMarkAsReadClicked(AppNotification notification);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<Object> items = new ArrayList<>();
    private final OnNotificationActionListener listener;
    private final SimpleDateFormat headerFormat = new SimpleDateFormat("dd 'de' MMMM", new Locale("pt", "BR"));
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("pt", "BR"));

    public NotificationAdapter(OnNotificationActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AppNotification> notifications) {
        items.clear();
        String previousHeader = null;
        for (AppNotification notification : notifications) {
            String header = headerFormat.format(new Date(notification.getCreatedAtMillis()));
            if (!header.equals(previousHeader)) {
                items.add(header);
                previousHeader = header;
            }
            items.add(notification);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_notification_header, parent, false));
        }
        return new NotificationViewHolder(inflater.inflate(R.layout.item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).textHeader.setText(String.valueOf(item));
            return;
        }

        AppNotification notification = (AppNotification) item;
        NotificationViewHolder viewHolder = (NotificationViewHolder) holder;
        viewHolder.textTitle.setText(notification.getTitle());
        viewHolder.textMessage.setText(notification.getMessage());
        viewHolder.textTime.setText(timeFormat.format(new Date(notification.getCreatedAtMillis())));
        viewHolder.textType.setText(resolveTypeLabel(notification.getType(), holder.itemView));
        viewHolder.viewUnread.setVisibility(notification.isRead() ? View.INVISIBLE : View.VISIBLE);
        viewHolder.textMarkRead.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
        viewHolder.itemView.setOnClickListener(v -> listener.onNotificationClicked(notification));
        viewHolder.textMarkRead.setOnClickListener(v -> listener.onMarkAsReadClicked(notification));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView textHeader;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textHeader = itemView.findViewById(R.id.textNotificationHeader);
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final View viewUnread;
        private final TextView textType;
        private final TextView textTitle;
        private final TextView textMessage;
        private final TextView textTime;
        private final TextView textMarkRead;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            viewUnread = itemView.findViewById(R.id.viewUnreadIndicator);
            textType = itemView.findViewById(R.id.textNotificationType);
            textTitle = itemView.findViewById(R.id.textNotificationTitle);
            textMessage = itemView.findViewById(R.id.textNotificationMessage);
            textTime = itemView.findViewById(R.id.textNotificationTime);
            textMarkRead = itemView.findViewById(R.id.textNotificationMarkRead);
        }
    }

    private String resolveTypeLabel(String type, View itemView) {
        if (NotificationType.LIKE.equals(type)) {
            return itemView.getContext().getString(R.string.notification_type_like);
        }
        if (NotificationType.COMMENT.equals(type)) {
            return itemView.getContext().getString(R.string.notification_type_comment);
        }
        if (NotificationType.CHAT_MESSAGE.equals(type)) {
            return itemView.getContext().getString(R.string.notification_type_chat);
        }
        if (NotificationType.ADOPTION_INTEREST.equals(type)) {
            return itemView.getContext().getString(R.string.notification_type_interest);
        }
        if (NotificationType.POST_UPDATED.equals(type)) {
            return itemView.getContext().getString(R.string.notification_type_update);
        }
        return type;
    }
}
