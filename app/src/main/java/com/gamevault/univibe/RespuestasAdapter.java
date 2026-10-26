package com.gamevault.univibe;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RespuestasAdapter extends RecyclerView.Adapter<RespuestasAdapter.RespuestaViewHolder> {

    private final Context context;
    private final String postId;
    private final String commentId;
    private final List<Respuesta> respuestasList;
    private final String currentUid;

    public RespuestasAdapter(Context context, String postId, String commentId, List<Respuesta> respuestasList) {
        this.context = context;
        this.postId = postId;
        this.commentId = commentId;
        this.respuestasList = respuestasList;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        this.currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    @NonNull
    @Override
    public RespuestaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_respuesta, parent, false);
        return new RespuestaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RespuestaViewHolder holder, int position) {
        Respuesta reply = respuestasList.get(position);

        holder.tvUserName.setText(reply.getUserName() != null ? reply.getUserName() : "");
        holder.tvText.setText(reply.getReplyText() != null ? reply.getReplyText() : "");
        holder.tvLikesCount.setText(String.valueOf(reply.getLikesCount()));

        // Relative time
        if (reply.getTimestamp() > 0) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    reply.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
            holder.tvTime.setText(timeAgo);
        } else {
            holder.tvTime.setText("");
        }

        // Avatar
        if (!TextUtils.isEmpty(reply.getUserPhotoUrl())) {
            Glide.with(context)
                    .load(reply.getUserPhotoUrl())
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivUserAvatar);
        } else {
            holder.ivUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Image in reply
        if (holder.ivReplyImage != null) {
            if (!TextUtils.isEmpty(reply.getImageUrl())) {
                holder.ivReplyImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(reply.getImageUrl())
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(holder.ivReplyImage);

                holder.ivReplyImage.setOnClickListener(v -> {
                    Intent intent = new Intent(context, VerImagenCompletaActivity.class);
                    intent.putExtra("imageUrl", reply.getImageUrl());
                    context.startActivity(intent);
                });
            } else {
                holder.ivReplyImage.setVisibility(View.GONE);
                holder.ivReplyImage.setOnClickListener(null);
            }
        }

        // Like status check & click listener
        verificarMeGustaRespuesta(reply.getReplyId(), holder.ivLikeStar);

        holder.llLike.setOnClickListener(v -> toggleMeGustaRespuesta(reply, holder.ivLikeStar));
    }

    private void verificarMeGustaRespuesta(String replyId, ImageView ivStar) {
        if (TextUtils.isEmpty(replyId) || TextUtils.isEmpty(currentUid)) return;

        DatabaseReference likeRef = FirebaseDatabase.getInstance().getReference("replyLikes").child(replyId).child(currentUid);
        likeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                    ivStar.setImageResource(android.R.drawable.btn_star_big_on);
                    ivStar.setColorFilter(Color.parseColor("#00A884"));
                } else {
                    ivStar.setImageResource(android.R.drawable.btn_star_big_off);
                    ivStar.setColorFilter(Color.parseColor("#7A8C85"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void toggleMeGustaRespuesta(Respuesta reply, ImageView ivStar) {
        if (reply == null || TextUtils.isEmpty(reply.getReplyId()) || TextUtils.isEmpty(currentUid)) return;

        String replyId = reply.getReplyId();
        DatabaseReference likeRef = FirebaseDatabase.getInstance().getReference("replyLikes").child(replyId).child(currentUid);

        likeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean yaTieneLike = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                int nuevoCount = yaTieneLike ? Math.max(0, reply.getLikesCount() - 1) : reply.getLikesCount() + 1;

                if (!yaTieneLike) {
                    reproducirSonidoEstrella();
                    if (ivStar != null) {
                        AnimacionUtils.animarEstrella(ivStar);
                    }
                    if (!reply.getUid().equals(currentUid)) {
                        NotificacionesHelper.registrarNotificacion(reply.getUid(), "LIKE_REPLY", postId, commentId, reply.getReplyId(), "le dio me gusta a tu respuesta");
                    }
                }

                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                Map<String, Object> updates = new HashMap<>();

                if (yaTieneLike) {
                    updates.put("/replyLikes/" + replyId + "/" + currentUid, null);
                } else {
                    updates.put("/replyLikes/" + replyId + "/" + currentUid, true);
                }

                updates.put("/comentarios/" + postId + "/" + commentId + "/respuestas/" + replyId + "/likesCount", nuevoCount);

                rootRef.updateChildren(updates);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void reproducirSonidoEstrella() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.estrella_sonido);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return respuestasList.size();
    }

    public static class RespuestaViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivUserAvatar, ivReplyImage;
        ImageView ivLikeStar;
        TextView tvUserName, tvText, tvTime, tvLikesCount;
        LinearLayout llLike;

        public RespuestaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.iv_reply_user_avatar);
            ivReplyImage = itemView.findViewById(R.id.iv_reply_image);
            ivLikeStar = itemView.findViewById(R.id.iv_reply_like_star);
            tvUserName = itemView.findViewById(R.id.tv_reply_user_name);
            tvText = itemView.findViewById(R.id.tv_reply_text);
            tvTime = itemView.findViewById(R.id.tv_reply_time);
            tvLikesCount = itemView.findViewById(R.id.tv_reply_likes_count);
            llLike = itemView.findViewById(R.id.ll_reply_like);
        }
    }
}
