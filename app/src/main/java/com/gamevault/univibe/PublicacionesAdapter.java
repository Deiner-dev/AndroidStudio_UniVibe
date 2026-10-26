package com.gamevault.univibe;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import com.google.android.material.button.MaterialButton;
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

public class PublicacionesAdapter extends RecyclerView.Adapter<PublicacionesAdapter.PublicacionViewHolder> {

    private final Context context;
    private final List<Publicacion> publicacionesList;
    private final String currentUid;

    public PublicacionesAdapter(Context context, List<Publicacion> publicacionesList) {
        this.context = context;
        this.publicacionesList = publicacionesList;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        this.currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    @NonNull
    @Override
    public PublicacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_publicacion, parent, false);
        return new PublicacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PublicacionViewHolder holder, int position) {
        Publicacion post = publicacionesList.get(position);

        holder.tvUserName.setText(post.getUserName() != null ? post.getUserName() : "");

        String carrera = post.getCareer() != null ? post.getCareer() : "";
        String semestre = post.getSemester() != null ? post.getSemester() : "";
        if (!carrera.isEmpty() && !semestre.isEmpty()) {
            holder.tvAcademicInfo.setText(context.getString(R.string.user_academic_info_format, carrera, semestre));
            holder.tvAcademicInfo.setVisibility(View.VISIBLE);
        } else if (!carrera.isEmpty()) {
            holder.tvAcademicInfo.setText(carrera);
            holder.tvAcademicInfo.setVisibility(View.VISIBLE);
        } else {
            holder.tvAcademicInfo.setVisibility(View.GONE);
        }

        // Time ago
        if (post.getTimestamp() > 0) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    post.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
            holder.tvTime.setText(timeAgo);
        } else {
            holder.tvTime.setText("");
        }

        // Text content
        if (!TextUtils.isEmpty(post.getTextContent())) {
            holder.tvText.setText(post.getTextContent());
            holder.tvText.setVisibility(View.VISIBLE);
        } else {
            holder.tvText.setVisibility(View.GONE);
        }

        // Image content
        if (!TextUtils.isEmpty(post.getImageUrl())) {
            holder.ivImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getImageUrl())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivImage);

            holder.ivImage.setOnClickListener(v -> {
                Intent intent = new Intent(context, VerImagenCompletaActivity.class);
                intent.putExtra("imageUrl", post.getImageUrl());
                context.startActivity(intent);
            });
        } else {
            holder.ivImage.setVisibility(View.GONE);
            holder.ivImage.setOnClickListener(null);
        }

        // Location
        if (!TextUtils.isEmpty(post.getLocation())) {
            holder.llLocation.setVisibility(View.VISIBLE);
            holder.tvLocationText.setText(post.getLocation());
        } else {
            holder.llLocation.setVisibility(View.GONE);
        }

        // Likes & Comments Count
        if (holder.tvCommentCount != null) {
            holder.tvCommentCount.setText(String.valueOf(post.getCommentsCount()));
        }

        if (holder.tvLikeCount != null) {
            holder.tvLikeCount.setText(String.valueOf(post.getLikesCount()));
        }

        // Check Like State in Realtime Database
        if (holder.ivLikeStar != null) {
            verificarMeGusta(post.getPostId(), holder.ivLikeStar);
        }

        // Toggle Like Click Listener
        if (holder.llLike != null) {
            holder.llLike.setOnClickListener(v -> toggleMeGusta(post));
        }

        // Follow / Following Status Check & Click Listener
        if (holder.btnFollowUser != null) {
            String postAuthorUid = post.getUid();
            if (!TextUtils.isEmpty(postAuthorUid) && !postAuthorUid.equals(currentUid)) {
                holder.btnFollowUser.setVisibility(View.VISIBLE);
                verificarYConfigurarSeguir(postAuthorUid, holder.btnFollowUser);
                holder.btnFollowUser.setOnClickListener(v -> toggleSeguir(postAuthorUid));
            } else {
                holder.btnFollowUser.setVisibility(View.GONE);
            }
        }

        // Avatar
        if (!TextUtils.isEmpty(post.getUserPhotoUrl())) {
            Glide.with(context)
                    .load(post.getUserPhotoUrl())
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivUserAvatar);
        } else {
            holder.ivUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Open user profile when clicking avatar or user name
        View.OnClickListener abrirPerfilListener = v -> {
            if (!TextUtils.isEmpty(post.getUid())) {
                Intent intent = new Intent(context, PefilUser.class);
                intent.putExtra("targetUid", post.getUid());
                context.startActivity(intent);
            }
        };

        if (holder.ivUserAvatar != null) {
            holder.ivUserAvatar.setOnClickListener(abrirPerfilListener);
        }
        if (holder.tvUserName != null) {
            holder.tvUserName.setOnClickListener(abrirPerfilListener);
        }

        // Open comments & post detail screen when clicking item or comment action
        View.OnClickListener abrirDetalleListener = v -> {
            if (post.getPostId() != null && !post.getPostId().isEmpty()) {
                Intent intent = new Intent(context, listaComentarios.class);
                intent.putExtra("postId", post.getPostId());
                context.startActivity(intent);
            }
        };

        holder.itemView.setOnClickListener(abrirDetalleListener);
        if (holder.llComment != null) {
            holder.llComment.setOnClickListener(abrirDetalleListener);
        }
    }

    private void verificarMeGusta(String postId, ImageView ivStar) {
        if (TextUtils.isEmpty(postId) || TextUtils.isEmpty(currentUid)) return;

        DatabaseReference likeRef = FirebaseDatabase.getInstance().getReference("postLikes").child(postId).child(currentUid);
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

    private void toggleMeGusta(Publicacion post) {
        if (post == null || TextUtils.isEmpty(post.getPostId()) || TextUtils.isEmpty(currentUid)) return;

        String postId = post.getPostId();
        String authorUid = post.getUid();
        DatabaseReference likeRef = FirebaseDatabase.getInstance().getReference("postLikes").child(postId).child(currentUid);

        likeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean yaTieneLike = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                int nuevoCount = yaTieneLike ? Math.max(0, post.getLikesCount() - 1) : post.getLikesCount() + 1;

                if (!yaTieneLike) {
                    reproducirSonidoEstrella();
                    NotificacionesHelper.registrarNotificacion(authorUid, "LIKE_POST", postId, "", "", "le dio me gusta a tu publicación");
                }

                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                Map<String, Object> updates = new HashMap<>();

                if (yaTieneLike) {
                    updates.put("/postLikes/" + postId + "/" + currentUid, null);
                } else {
                    updates.put("/postLikes/" + postId + "/" + currentUid, true);
                }

                updates.put("/publicaciones/" + postId + "/likesCount", nuevoCount);
                if (!TextUtils.isEmpty(authorUid)) {
                    updates.put("/userPublicaciones/" + authorUid + "/" + postId + "/likesCount", nuevoCount);
                }

                rootRef.updateChildren(updates);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void verificarYConfigurarSeguir(String targetUid, MaterialButton btnFollow) {
        if (TextUtils.isEmpty(targetUid) || TextUtils.isEmpty(currentUid) || btnFollow == null) return;

        DatabaseReference followRef = FirebaseDatabase.getInstance().getReference("seguidos").child(currentUid).child(targetUid);
        followRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean loSigue = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                if (loSigue) {
                    btnFollow.setText(R.string.btn_following);
                    btnFollow.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                    btnFollow.setTextColor(Color.parseColor("#1A3E30"));
                } else {
                    btnFollow.setText(R.string.btn_follow);
                    btnFollow.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#36A477")));
                    btnFollow.setTextColor(Color.WHITE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void toggleSeguir(String targetUid) {
        if (TextUtils.isEmpty(targetUid) || TextUtils.isEmpty(currentUid)) return;

        DatabaseReference followRef = FirebaseDatabase.getInstance().getReference("seguidos").child(currentUid).child(targetUid);

        followRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean yaLoSigue = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                Map<String, Object> updates = new HashMap<>();

                if (yaLoSigue) {
                    updates.put("/seguidos/" + currentUid + "/" + targetUid, null);
                    updates.put("/seguidores/" + targetUid + "/" + currentUid, null);
                } else {
                    updates.put("/seguidos/" + currentUid + "/" + targetUid, true);
                    updates.put("/seguidores/" + targetUid + "/" + currentUid, true);
                    NotificacionesHelper.registrarNotificacion(targetUid, "FOLLOW", "", "", "", "te comenzó a seguir");
                }

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
        return publicacionesList.size();
    }

    public static class PublicacionViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivUserAvatar, ivImage;
        ImageView ivLikeStar;
        MaterialButton btnFollowUser;
        TextView tvUserName, tvAcademicInfo, tvTime, tvText, tvLocationText, tvCommentCount, tvLikeCount;
        LinearLayout llLocation, llComment, llLike;

        public PublicacionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.iv_post_user_avatar);
            ivImage = itemView.findViewById(R.id.iv_post_image);
            ivLikeStar = itemView.findViewById(R.id.iv_like_star);
            btnFollowUser = itemView.findViewById(R.id.btn_follow_user);
            tvUserName = itemView.findViewById(R.id.tv_post_user_name);
            tvAcademicInfo = itemView.findViewById(R.id.tv_post_academic_info);
            tvTime = itemView.findViewById(R.id.tv_post_time);
            tvText = itemView.findViewById(R.id.tv_post_text);
            tvLocationText = itemView.findViewById(R.id.tv_post_location_text);
            tvCommentCount = itemView.findViewById(R.id.tv_comment_count);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            llLocation = itemView.findViewById(R.id.ll_post_location);
            llComment = itemView.findViewById(R.id.ll_comment);
            llLike = itemView.findViewById(R.id.ll_like);
        }
    }
}
