package com.gamevault.univibe;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final Context context;
    private final List<Mensaje> mensajeList;
    private final String currentUid;
    private final String targetPhotoUrl;

    public ChatAdapter(Context context, List<Mensaje> mensajeList, String targetPhotoUrl) {
        this.context = context;
        this.mensajeList = mensajeList;
        this.targetPhotoUrl = targetPhotoUrl;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        this.currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_mensaje, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Mensaje mensaje = mensajeList.get(position);
        boolean isSentByMe = !TextUtils.isEmpty(mensaje.getSenderUid()) && mensaje.getSenderUid().equals(currentUid);

        String timeFormatted = "";
        if (mensaje.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            timeFormatted = sdf.format(new Date(mensaje.getTimestamp()));
        }

        if (isSentByMe) {
            holder.llSentContainer.setVisibility(View.VISIBLE);
            holder.llReceivedContainer.setVisibility(View.GONE);

            holder.tvSentTime.setText(timeFormatted);

            if (!TextUtils.isEmpty(mensaje.getText())) {
                holder.tvSentText.setText(mensaje.getText());
                holder.tvSentText.setVisibility(View.VISIBLE);
            } else {
                holder.tvSentText.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(mensaje.getImageUrl())) {
                holder.ivSentImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(mensaje.getImageUrl())
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(holder.ivSentImage);

                holder.ivSentImage.setOnClickListener(v -> {
                    Intent intent = new Intent(context, VerImagenCompletaActivity.class);
                    intent.putExtra("imageUrl", mensaje.getImageUrl());
                    context.startActivity(intent);
                });
            } else {
                holder.ivSentImage.setVisibility(View.GONE);
            }

        } else {
            holder.llSentContainer.setVisibility(View.GONE);
            holder.llReceivedContainer.setVisibility(View.VISIBLE);

            holder.tvReceivedTime.setText(timeFormatted);

            if (!TextUtils.isEmpty(mensaje.getText())) {
                holder.tvReceivedText.setText(mensaje.getText());
                holder.tvReceivedText.setVisibility(View.VISIBLE);
            } else {
                holder.tvReceivedText.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(targetPhotoUrl)) {
                Glide.with(context)
                        .load(targetPhotoUrl)
                        .circleCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(holder.ivReceivedUserAvatar);
            } else {
                holder.ivReceivedUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            if (!TextUtils.isEmpty(mensaje.getImageUrl())) {
                holder.ivReceivedImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(mensaje.getImageUrl())
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(holder.ivReceivedImage);

                holder.ivReceivedImage.setOnClickListener(v -> {
                    Intent intent = new Intent(context, VerImagenCompletaActivity.class);
                    intent.putExtra("imageUrl", mensaje.getImageUrl());
                    context.startActivity(intent);
                });
            } else {
                holder.ivReceivedImage.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mensajeList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llSentContainer, llReceivedContainer;
        ShapeableImageView ivSentImage, ivReceivedImage, ivReceivedUserAvatar;
        TextView tvSentText, tvSentTime, tvReceivedText, tvReceivedTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            llSentContainer = itemView.findViewById(R.id.ll_sent_container);
            llReceivedContainer = itemView.findViewById(R.id.ll_received_container);

            ivSentImage = itemView.findViewById(R.id.iv_sent_image);
            tvSentText = itemView.findViewById(R.id.tv_sent_text);
            tvSentTime = itemView.findViewById(R.id.tv_sent_time);

            ivReceivedUserAvatar = itemView.findViewById(R.id.iv_received_user_avatar);
            ivReceivedImage = itemView.findViewById(R.id.iv_received_image);
            tvReceivedText = itemView.findViewById(R.id.tv_received_text);
            tvReceivedTime = itemView.findViewById(R.id.tv_received_time);
        }
    }
}
