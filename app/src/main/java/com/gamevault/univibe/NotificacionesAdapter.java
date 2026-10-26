package com.gamevault.univibe;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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

import java.util.List;

public class NotificacionesAdapter extends RecyclerView.Adapter<NotificacionesAdapter.NotificacionViewHolder> {

    private final Context context;
    private final List<Notificacion> notificacionesList;

    public NotificacionesAdapter(Context context, List<Notificacion> notificacionesList) {
        this.context = context;
        this.notificacionesList = notificacionesList;
    }

    @NonNull
    @Override
    public NotificacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notificacion, parent, false);
        return new NotificacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificacionViewHolder holder, int position) {
        Notificacion notif = notificacionesList.get(position);

        holder.tvUserName.setText(notif.getFromUserName() != null ? notif.getFromUserName() : "");
        holder.tvMessage.setText(notif.getMessage() != null ? notif.getMessage() : "");

        if (notif.getTimestamp() > 0) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    notif.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
            holder.tvTime.setText(timeAgo);
        } else {
            holder.tvTime.setText("");
        }

        if (!TextUtils.isEmpty(notif.getFromUserPhotoUrl())) {
            Glide.with(context)
                    .load(notif.getFromUserPhotoUrl())
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Highlight unread notifications
        if (!notif.isRead()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#F2FAF6"));
            if (holder.vUnreadDot != null) holder.vUnreadDot.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            if (holder.vUnreadDot != null) holder.vUnreadDot.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            String type = notif.getType();
            if ("FOLLOW".equals(type) && !TextUtils.isEmpty(notif.getFromUid())) {
                Intent intent = new Intent(context, PefilUser.class);
                intent.putExtra("targetUid", notif.getFromUid());
                context.startActivity(intent);
            } else if (!TextUtils.isEmpty(notif.getPostId())) {
                Intent intent = new Intent(context, listaComentarios.class);
                intent.putExtra("postId", notif.getPostId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificacionesList.size();
    }

    public static class NotificacionViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvUserName, tvMessage, tvTime;
        View vUnreadDot;

        public NotificacionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_notif_avatar);
            tvUserName = itemView.findViewById(R.id.tv_notif_user_name);
            tvMessage = itemView.findViewById(R.id.tv_notif_message);
            tvTime = itemView.findViewById(R.id.tv_notif_time);
            vUnreadDot = itemView.findViewById(R.id.v_unread_dot);
        }
    }
}
