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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComentariosAdapter extends RecyclerView.Adapter<ComentariosAdapter.ComentarioViewHolder> {

    public interface OnCommentReplyClickListener {
        void onReplyClicked(Comentario parentComment);
    }

    private final Context context;
    private final List<Comentario> comentariosList;
    private final String currentUid;
    private final OnCommentReplyClickListener replyClickListener;

    public ComentariosAdapter(Context context, List<Comentario> comentariosList, OnCommentReplyClickListener replyClickListener) {
        this.context = context;
        this.comentariosList = comentariosList;
        this.replyClickListener = replyClickListener;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        this.currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    @NonNull
    @Override
    public ComentarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comentario, parent, false);
        return new ComentarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComentarioViewHolder holder, int position) {
        Comentario comentario = comentariosList.get(position);

        holder.tvUserName.setText(comentario.getUserName() != null ? comentario.getUserName() : "");
        holder.tvText.setText(comentario.getCommentText() != null ? comentario.getCommentText() : "");
        holder.tvLikesCount.setText(String.valueOf(comentario.getLikesCount()));

        // Relative time
        if (comentario.getTimestamp() > 0) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    comentario.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
            holder.tvTime.setText(timeAgo);
        } else {
            holder.tvTime.setText("");
        }

        // Avatar
        if (!TextUtils.isEmpty(comentario.getUserPhotoUrl())) {
            Glide.with(context)
                    .load(comentario.getUserPhotoUrl())
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivUserAvatar);
        } else {
            holder.ivUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Image in comment
        if (holder.ivCommentImage != null) {
            if (!TextUtils.isEmpty(comentario.getImageUrl())) {
                holder.ivCommentImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(comentario.getImageUrl())
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(holder.ivCommentImage);

                holder.ivCommentImage.setOnClickListener(v -> {
                    Intent intent = new Intent(context, VerImagenCompletaActivity.class);
                    intent.putExtra("imageUrl", comentario.getImageUrl());
                    context.startActivity(intent);
                });
            } else {
                holder.ivCommentImage.setVisibility(View.GONE);
                holder.ivCommentImage.setOnClickListener(null);
            }
        }

        // Check & handle comment like state
        if (holder.ivLikeStar != null) {
            verificarMeGustaComentario(comentario.getCommentId(), holder.ivLikeStar);
        }

        if (holder.llLike != null) {
            holder.llLike.setOnClickListener(v -> toggleMeGustaComentario(comentario, holder.ivLikeStar));
        }

        // Reply click
        if (holder.tvReply != null) {
            holder.tvReply.setOnClickListener(v -> {
                if (replyClickListener != null) {
                    replyClickListener.onReplyClicked(comentario);
                }
            });
        }

        // Load nested replies
        cargarRespuestas(comentario, holder.rvReplies);
    }

    private void verificarMeGustaComentario(String commentId, ImageView ivStar) {
        if (TextUtils.isEmpty(commentId) || TextUtils.isEmpty(currentUid)) return;

        DatabaseReference likeRef = FirebaseDatabase.getInstance().getReference("commentLikes").child(commentId).child(currentUid);
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

    private void toggleMeGustaComentario(Comentario comentario, ImageView ivStar) {
        if (comentario == null || TextUtils.isEmpty(comentario.getCommentId()) || TextUtils.isEmpty(currentUid)) return;

        String commentId = comentario.getCommentId();
        String postId = comentario.getPostId();
        DatabaseReference likeRef = FirebaseDatabase.getInstance().getReference("commentLikes").child(commentId).child(currentUid);

        likeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean yaTieneLike = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                int nuevoCount = yaTieneLike ? Math.max(0, comentario.getLikesCount() - 1) : comentario.getLikesCount() + 1;

                if (!yaTieneLike) {
                    reproducirSonidoEstrella();
                    if (ivStar != null) {
                        AnimacionUtils.animarEstrella(ivStar);
                    }
                    if (!comentario.getUid().equals(currentUid)) {
                        NotificacionesHelper.registrarNotificacion(comentario.getUid(), "LIKE_COMMENT", comentario.getPostId(), comentario.getCommentId(), "", "le dio me gusta a tu comentario");
                    }
                }

                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                Map<String, Object> updates = new HashMap<>();

                if (yaTieneLike) {
                    updates.put("/commentLikes/" + commentId + "/" + currentUid, null);
                } else {
                    updates.put("/commentLikes/" + commentId + "/" + currentUid, true);
                }

                updates.put("/comentarios/" + postId + "/" + commentId + "/likesCount", nuevoCount);

                rootRef.updateChildren(updates);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void cargarRespuestas(Comentario comentario, RecyclerView rvReplies) {
        if (comentario == null || TextUtils.isEmpty(comentario.getPostId()) || TextUtils.isEmpty(comentario.getCommentId()) || rvReplies == null) return;

        DatabaseReference repliesRef = FirebaseDatabase.getInstance().getReference("comentarios")
                .child(comentario.getPostId())
                .child(comentario.getCommentId())
                .child("respuestas");

        repliesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Respuesta> respuestasList = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        Respuesta reply = snap.getValue(Respuesta.class);
                        if (reply != null) {
                            respuestasList.add(reply);
                        }
                    } catch (Exception e) {}
                }

                Collections.sort(respuestasList, (r1, r2) -> Long.compare(r1.getTimestamp(), r2.getTimestamp()));

                if (!respuestasList.isEmpty()) {
                    rvReplies.setVisibility(View.VISIBLE);
                    rvReplies.setLayoutManager(new LinearLayoutManager(context));
                    RespuestasAdapter adapter = new RespuestasAdapter(context, comentario.getPostId(), comentario.getCommentId(), respuestasList);
                    rvReplies.setAdapter(adapter);
                } else {
                    rvReplies.setVisibility(View.GONE);
                }
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
        return comentariosList.size();
    }

    public static class ComentarioViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivUserAvatar, ivCommentImage;
        ImageView ivLikeStar;
        TextView tvUserName, tvText, tvTime, tvLikesCount, tvReply;
        LinearLayout llLike;
        RecyclerView rvReplies;

        public ComentarioViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.iv_comment_user_avatar);
            ivCommentImage = itemView.findViewById(R.id.iv_comment_image);
            ivLikeStar = itemView.findViewById(R.id.iv_comment_like_star);
            tvUserName = itemView.findViewById(R.id.tv_comment_user_name);
            tvText = itemView.findViewById(R.id.tv_comment_text);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvLikesCount = itemView.findViewById(R.id.tv_comment_likes_count);
            tvReply = itemView.findViewById(R.id.tv_comment_reply);
            llLike = itemView.findViewById(R.id.ll_comment_like);
            rvReplies = itemView.findViewById(R.id.rv_comment_replies);
        }
    }
}
