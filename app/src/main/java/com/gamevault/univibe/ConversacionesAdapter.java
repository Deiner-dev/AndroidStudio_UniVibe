package com.gamevault.univibe;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class ConversacionesAdapter extends RecyclerView.Adapter<ConversacionesAdapter.ConversacionViewHolder> {

    private final Context context;
    private final List<Conversacion> conversacionList;

    public ConversacionesAdapter(Context context, List<Conversacion> conversacionList) {
        this.context = context;
        this.conversacionList = conversacionList;
    }

    @NonNull
    @Override
    public ConversacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversacion, parent, false);
        return new ConversacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversacionViewHolder holder, int position) {
        Conversacion conv = conversacionList.get(position);

        holder.tvUserName.setText(!TextUtils.isEmpty(conv.getChatUserName()) ? conv.getChatUserName() : "Usuario");
        holder.tvLastMessage.setText(!TextUtils.isEmpty(conv.getLastMessage()) ? conv.getLastMessage() : "");

        if (conv.getLastMessageTimestamp() > 0) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    conv.getLastMessageTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
            holder.tvTime.setText(timeAgo);
        } else {
            holder.tvTime.setText("");
        }

        if (!TextUtils.isEmpty(conv.getChatUserPhotoUrl())) {
            Glide.with(context)
                    .load(conv.getChatUserPhotoUrl())
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Real-time online status check for chat contact
        if (!TextUtils.isEmpty(conv.getChatUserId()) && holder.tvOnlineStatusText != null) {
            DatabaseReference onlineRef = FirebaseDatabase.getInstance()
                    .getReference("registroUsuario")
                    .child(conv.getChatUserId())
                    .child("online");

            onlineRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean isOnline = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                    if (isOnline) {
                        holder.tvOnlineStatusText.setVisibility(View.VISIBLE);
                    } else {
                        holder.tvOnlineStatusText.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        } else if (holder.tvOnlineStatusText != null) {
            holder.tvOnlineStatusText.setVisibility(View.GONE);
        }

        if (conv.getUnreadCount() > 0) {
            if (holder.tvUnreadCount != null) {
                holder.tvUnreadCount.setVisibility(View.VISIBLE);
                holder.tvUnreadCount.setText(String.valueOf(conv.getUnreadCount()));
            }
        } else {
            if (holder.tvUnreadCount != null) holder.tvUnreadCount.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(conv.getChatUserId())) {
                Intent intent = new Intent(context, chat.class);
                intent.putExtra("targetUid", conv.getChatUserId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversacionList.size();
    }

    public static class ConversacionViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvUserName, tvLastMessage, tvTime, tvUnreadCount, tvOnlineStatusText;

        public ConversacionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_chat_user_avatar);
            tvUserName = itemView.findViewById(R.id.tv_chat_user_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_chat_time);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
            tvOnlineStatusText = itemView.findViewById(R.id.tv_online_status_text);
        }
    }
}
