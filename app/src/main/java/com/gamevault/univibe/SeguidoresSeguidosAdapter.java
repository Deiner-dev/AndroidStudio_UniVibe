package com.gamevault.univibe;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class SeguidoresSeguidosAdapter extends RecyclerView.Adapter<SeguidoresSeguidosAdapter.UserViewHolder> {

    private final Context context;
    private final List<User> userList;
    private final String currentUid;

    public SeguidoresSeguidosAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        this.currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_usuario_seguimiento, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.tvUserName.setText(!TextUtils.isEmpty(user.getFullName()) ? user.getFullName() : "Usuario");

        if (holder.tvUserHandle != null) {
            if (!TextUtils.isEmpty(user.getInstitutionalEmail())) {
                holder.tvUserHandle.setText(user.getInstitutionalEmail());
                holder.tvUserHandle.setVisibility(View.VISIBLE);
            } else {
                holder.tvUserHandle.setVisibility(View.GONE);
            }
        }

        if (!TextUtils.isEmpty(user.getCareer())) {
            holder.tvUserCareer.setText(user.getCareer());
            holder.tvUserCareer.setVisibility(View.VISIBLE);
        } else {
            holder.tvUserCareer.setText(R.string.placeholder_career);
            holder.tvUserCareer.setVisibility(View.VISIBLE);
        }

        if (!TextUtils.isEmpty(user.getUniversity())) {
            holder.tvUserUniversity.setText(user.getUniversity());
            holder.tvUserUniversity.setVisibility(View.VISIBLE);
        } else {
            holder.tvUserUniversity.setText(R.string.placeholder_university);
            holder.tvUserUniversity.setVisibility(View.VISIBLE);
        }

        if (!TextUtils.isEmpty(user.getImgPerfil())) {
            Glide.with(context)
                    .load(user.getImgPerfil())
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Follow Button
        String targetUid = user.getUid();
        if (!TextUtils.isEmpty(targetUid) && !targetUid.equals(currentUid)) {
            holder.btnFollowUser.setVisibility(View.VISIBLE);
            verificarYConfigurarSeguir(targetUid, holder.btnFollowUser);
            holder.btnFollowUser.setOnClickListener(v -> toggleSeguir(targetUid));
        } else {
            holder.btnFollowUser.setVisibility(View.GONE);
        }

        // Click item opens profile
        holder.itemView.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(targetUid)) {
                Intent intent = new Intent(context, PefilUser.class);
                intent.putExtra("targetUid", targetUid);
                context.startActivity(intent);
            }
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
                    btnFollow.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#36A477")));
                    btnFollow.setTextColor(Color.WHITE);
                } else {
                    btnFollow.setText(R.string.btn_follow);
                    btnFollow.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                    btnFollow.setTextColor(Color.parseColor("#1A3E30"));
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

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvUserName, tvUserHandle, tvUserCareer, tvUserUniversity;
        MaterialButton btnFollowUser;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvUserHandle = itemView.findViewById(R.id.tv_user_handle);
            tvUserCareer = itemView.findViewById(R.id.tv_user_career);
            tvUserUniversity = itemView.findViewById(R.id.tv_user_university);
            btnFollowUser = itemView.findViewById(R.id.btn_follow_user);
        }
    }
}
