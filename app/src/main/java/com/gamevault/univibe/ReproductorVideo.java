package com.gamevault.univibe;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReproductorVideo extends AppCompatActivity {

    private VideoView vvVideoPlayer;
    private ImageView ivVideoPoster;
    private ImageButton ibVideoBack, ibVideoOptions;
    private FrameLayout flCenterPlayPause;
    private ImageView ivCenterPlayPause;

    // Right Sidebar Actions
    private LinearLayout llActionLike, llActionComment, llActionBookmark;
    private ImageView ivActionLikeStar, ivActionComment, ivActionBookmark;
    private TextView tvActionLikeCount, tvActionCommentCount;

    // User Info
    private ShapeableImageView ivUserAvatar;
    private TextView tvUserName, tvUserHandle, tvCaption;

    // Controls
    private SeekBar sbVideoProgress;
    private ImageView ivControlPlayPause, ivControlFullscreen;
    private TextView tvVideoTime;

    // Bottom Navigation
    private LinearLayout llNavInicio, llNavBuscar, llNavAlertas, llNavPerfil;

    private boolean isPlaying = false;
    private boolean isLiked = false;
    private boolean isBookmarked = false;
    private int likeCount = 124;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable updateProgressRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_reproductor_video);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
        setupVideoPlayer();
        setupBottomNav();
        verificarMeGustaFirebase();
        verificarComentariosCountFirebase();
    }

    private void initViews() {
        vvVideoPlayer = findViewById(R.id.vv_video_player);
        ivVideoPoster = findViewById(R.id.iv_video_poster);

        ibVideoBack = findViewById(R.id.ib_video_back);
        ibVideoOptions = findViewById(R.id.ib_video_options);

        flCenterPlayPause = findViewById(R.id.fl_center_play_pause);
        ivCenterPlayPause = findViewById(R.id.iv_center_play_pause);

        llActionLike = findViewById(R.id.ll_action_like);
        llActionComment = findViewById(R.id.ll_action_comment);
        llActionBookmark = findViewById(R.id.ll_action_bookmark);

        ivActionLikeStar = findViewById(R.id.iv_action_like_star);
        ivActionComment = findViewById(R.id.iv_action_comment);
        ivActionBookmark = findViewById(R.id.iv_action_bookmark);

        tvActionLikeCount = findViewById(R.id.tv_action_like_count);
        tvActionCommentCount = findViewById(R.id.tv_action_comment_count);

        ivUserAvatar = findViewById(R.id.iv_video_user_avatar);
        tvUserName = findViewById(R.id.tv_video_user_name);
        tvUserHandle = findViewById(R.id.tv_video_user_handle);
        tvCaption = findViewById(R.id.tv_video_caption);

        sbVideoProgress = findViewById(R.id.sb_video_progress);
        ivControlPlayPause = findViewById(R.id.iv_control_play_pause);
        ivControlFullscreen = findViewById(R.id.iv_control_fullscreen);
        tvVideoTime = findViewById(R.id.tv_video_time);

        llNavInicio = findViewById(R.id.ll_nav_inicio);
        llNavBuscar = findViewById(R.id.ll_nav_buscar);
        llNavAlertas = findViewById(R.id.ll_nav_alertas);
        llNavPerfil = findViewById(R.id.ll_nav_perfil);
    }

    private void setupVideoPlayer() {
        String videoUrl = getIntent().getStringExtra("videoUrl");
        String userName = getIntent().getStringExtra("userName");
        String userPhotoUrl = getIntent().getStringExtra("userPhotoUrl");
        String caption = getIntent().getStringExtra("caption");
        int likes = getIntent().getIntExtra("likesCount", 124);

        if (!TextUtils.isEmpty(userName) && tvUserName != null) {
            tvUserName.setText(userName);
            if (tvUserHandle != null) {
                tvUserHandle.setText("@" + userName.replaceAll("\\s+", "").toLowerCase(Locale.ROOT));
            }
        }

        if (!TextUtils.isEmpty(caption) && tvCaption != null) {
            tvCaption.setText(caption);
        }

        if (tvActionLikeCount != null) {
            tvActionLikeCount.setText(String.valueOf(likes));
            likeCount = likes;
        }

        if (!TextUtils.isEmpty(userPhotoUrl) && ivUserAvatar != null) {
            Glide.with(this)
                    .load(userPhotoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.logo)
                    .into(ivUserAvatar);
        }

        if (videoUrl != null && !videoUrl.isEmpty()) {
            vvVideoPlayer.setVideoURI(Uri.parse(videoUrl));
        }

        vvVideoPlayer.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            int videoWidth = mp.getVideoWidth();
            int videoHeight = mp.getVideoHeight();

            if (videoWidth > 0 && videoHeight > 0 && vvVideoPlayer != null) {
                View parent = (View) vvVideoPlayer.getParent();
                int parentWidth = parent.getWidth();
                int parentHeight = parent.getHeight();

                if (parentWidth > 0 && parentHeight > 0) {
                    android.view.ViewGroup.LayoutParams lp = vvVideoPlayer.getLayoutParams();
                    boolean isVertical = videoHeight >= videoWidth;
                    if (isVertical) {
                        lp.width = parentWidth;
                        lp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                    } else {
                        lp.width = parentWidth;
                        lp.height = (int) (parentWidth * ((float) videoHeight / videoWidth));
                    }
                    vvVideoPlayer.setLayoutParams(lp);
                }
            }

            int duration = vvVideoPlayer.getDuration();
            if (sbVideoProgress != null) {
                sbVideoProgress.setMax(duration > 0 ? duration : 100);
            }
            actualizarTiempoTexto(vvVideoPlayer.getCurrentPosition(), duration);
            togglePlayPause();
        });

        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (vvVideoPlayer != null && vvVideoPlayer.isPlaying()) {
                    int current = vvVideoPlayer.getCurrentPosition();
                    int duration = vvVideoPlayer.getDuration();
                    if (sbVideoProgress != null) {
                        sbVideoProgress.setProgress(current);
                    }
                    actualizarTiempoTexto(current, duration);
                    progressHandler.postDelayed(this, 300);
                }
            }
        };
    }

    private void setupListeners() {
        if (ibVideoBack != null) {
            ibVideoBack.setOnClickListener(v -> finish());
        }

        if (ibVideoOptions != null) {
            ibVideoOptions.setOnClickListener(v ->
                Toast.makeText(this, "Opciones del video", Toast.LENGTH_SHORT).show()
            );
        }

        if (flCenterPlayPause != null) {
            flCenterPlayPause.setOnClickListener(v -> togglePlayPause());
        }

        if (ivControlPlayPause != null) {
            ivControlPlayPause.setOnClickListener(v -> togglePlayPause());
        }

        if (ivControlFullscreen != null) {
            ivControlFullscreen.setOnClickListener(v ->
                Toast.makeText(this, "Pantalla completa", Toast.LENGTH_SHORT).show()
            );
        }

        if (llActionLike != null) {
            llActionLike.setOnClickListener(v -> toggleLike());
        }

        if (llActionComment != null) {
            llActionComment.setOnClickListener(v -> mostrarComentariosBottomSheet());
        }

        if (llActionBookmark != null) {
            llActionBookmark.setOnClickListener(v -> toggleBookmark());
        }

        if (sbVideoProgress != null) {
            sbVideoProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && vvVideoPlayer != null) {
                        vvVideoPlayer.seekTo(progress);
                        actualizarTiempoTexto(progress, vvVideoPlayer.getDuration());
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    private void verificarMeGustaFirebase() {
        String postId = getIntent().getStringExtra("postId");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (TextUtils.isEmpty(postId) || currentUser == null) return;
        String currentUid = currentUser.getUid();

        DatabaseReference likeRef = FirebaseDatabase.getInstance().getReference("postLikes").child(postId).child(currentUid);
        likeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isLiked = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                if (isLiked) {
                    if (ivActionLikeStar != null) {
                        ivActionLikeStar.setImageResource(android.R.drawable.btn_star_big_on);
                        ivActionLikeStar.setColorFilter(0xFFFFC107);
                    }
                } else {
                    if (ivActionLikeStar != null) {
                        ivActionLikeStar.setImageResource(android.R.drawable.btn_star_big_off);
                        ivActionLikeStar.setColorFilter(0xFFFFFFFF);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void verificarComentariosCountFirebase() {
        String postId = getIntent().getStringExtra("postId");
        if (TextUtils.isEmpty(postId)) {
            if (tvActionCommentCount != null) tvActionCommentCount.setText("0");
            return;
        }

        DatabaseReference commentsRef = FirebaseDatabase.getInstance().getReference("comentarios").child(postId);
        commentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int total = 0;
                for (DataSnapshot commentSnap : snapshot.getChildren()) {
                    total++;
                    total += (int) commentSnap.child("respuestas").getChildrenCount();
                }
                if (tvActionCommentCount != null) {
                    tvActionCommentCount.setText(String.valueOf(total));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void toggleLike() {
        String postId = getIntent().getStringExtra("postId");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (TextUtils.isEmpty(postId) || currentUser == null) return;
        String currentUid = currentUser.getUid();

        DatabaseReference likeRef = FirebaseDatabase.getInstance().getReference("postLikes").child(postId).child(currentUid);
        likeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean yaTieneLike = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                int nuevoCount = yaTieneLike ? Math.max(0, likeCount - 1) : likeCount + 1;

                if (!yaTieneLike) {
                    reproducirSonidoEstrella();
                    String authorUid = getIntent().getStringExtra("postAuthorUid");
                    if (!TextUtils.isEmpty(authorUid)) {
                        NotificacionesHelper.registrarNotificacion(authorUid, "LIKE_POST", postId, "", "", "le dio me gusta a tu publicación");
                    }
                }

                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                Map<String, Object> updates = new HashMap<>();

                if (yaTieneLike) {
                    updates.put("/postLikes/" + postId + "/" + currentUid, null);
                } else {
                    updates.put("/postLikes/" + postId + "/" + currentUid, true);
                }

                updates.put("/publicaciones/" + postId + "/likesCount", nuevoCount);
                String authorUid = getIntent().getStringExtra("postAuthorUid");
                if (!TextUtils.isEmpty(authorUid)) {
                    updates.put("/userPublicaciones/" + authorUid + "/" + postId + "/likesCount", nuevoCount);
                }

                rootRef.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        likeCount = nuevoCount;
                        if (tvActionLikeCount != null) {
                            tvActionLikeCount.setText(String.valueOf(likeCount));
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void mostrarComentariosBottomSheet() {
        String postId = getIntent().getStringExtra("postId");
        if (TextUtils.isEmpty(postId)) {
            Toast.makeText(this, "Publicación no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_bottom_sheet_comments, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvTitle = sheetView.findViewById(R.id.tv_sheet_comments_title);
        ImageButton ibClose = sheetView.findViewById(R.id.ib_close_sheet);
        RecyclerView rvComments = sheetView.findViewById(R.id.rv_sheet_comments);
        EditText etInput = sheetView.findViewById(R.id.et_sheet_comment_input);
        ImageButton ibSend = sheetView.findViewById(R.id.ib_sheet_send_comment);
        ShapeableImageView ivAvatar = sheetView.findViewById(R.id.iv_sheet_user_avatar);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("registroUsuario").child(currentUser.getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String photo = snapshot.child("imgPerfil").getValue(String.class);
                        if (!TextUtils.isEmpty(photo) && ivAvatar != null) {
                            Glide.with(ReproductorVideo.this).load(photo).circleCrop().into(ivAvatar);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        List<Comentario> comentariosList = new ArrayList<>();
        ComentariosAdapter commentsAdapter = new ComentariosAdapter(this, comentariosList, parentComment -> {});
        rvComments.setAdapter(commentsAdapter);

        DatabaseReference commentsRef = FirebaseDatabase.getInstance().getReference("comentarios").child(postId);
        commentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                comentariosList.clear();
                int total = 0;
                for (DataSnapshot commentSnap : snapshot.getChildren()) {
                    Comentario com = commentSnap.getValue(Comentario.class);
                    if (com != null) {
                        com.setCommentId(commentSnap.getKey());
                        comentariosList.add(com);
                        total++;
                        total += (int) commentSnap.child("respuestas").getChildrenCount();
                    }
                }
                commentsAdapter.notifyDataSetChanged();
                if (tvTitle != null) {
                    tvTitle.setText("Comentarios (" + total + ")");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        if (ibClose != null) {
            ibClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        if (ibSend != null) {
            ibSend.setOnClickListener(v -> {
                String text = etInput.getText().toString().trim();
                if (TextUtils.isEmpty(text)) {
                    Toast.makeText(this, "Escribe un comentario", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentUser == null) return;

                String commentId = commentsRef.push().getKey();
                if (commentId == null) return;

                DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("registroUsuario").child(currentUser.getUid());
                myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("fullName").getValue(String.class);
                        String photo = snapshot.child("imgPerfil").getValue(String.class);

                        Comentario nuevoComentario = new Comentario(
                                commentId,
                                postId,
                                currentUser.getUid(),
                                name != null ? name : "Usuario",
                                photo != null ? photo : "",
                                text,
                                "",
                                System.currentTimeMillis(),
                                0
                        );

                        commentsRef.child(commentId).setValue(nuevoComentario).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                etInput.setText("");
                                Toast.makeText(ReproductorVideo.this, "Comentario publicado", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            });
        }

        bottomSheetDialog.show();
    }

    private void togglePlayPause() {
        if (isPlaying) {
            if (vvVideoPlayer != null && vvVideoPlayer.isPlaying()) {
                vvVideoPlayer.pause();
            }
            isPlaying = false;
            if (ivCenterPlayPause != null) {
                ivCenterPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
            if (ivControlPlayPause != null) {
                ivControlPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
            if (flCenterPlayPause != null) {
                flCenterPlayPause.setVisibility(View.VISIBLE);
            }
            if (ivVideoPoster != null) {
                ivVideoPoster.setVisibility(View.VISIBLE);
            }
            progressHandler.removeCallbacks(updateProgressRunnable);
        } else {
            if (vvVideoPlayer != null) {
                vvVideoPlayer.start();
            }
            isPlaying = true;
            if (ivCenterPlayPause != null) {
                ivCenterPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            }
            if (ivControlPlayPause != null) {
                ivControlPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            }
            if (flCenterPlayPause != null) {
                flCenterPlayPause.setVisibility(View.GONE);
            }
            if (ivVideoPoster != null) {
                ivVideoPoster.setVisibility(View.GONE);
            }
            progressHandler.post(updateProgressRunnable);
        }
    }

    private void toggleBookmark() {
        isBookmarked = !isBookmarked;
        if (isBookmarked) {
            if (ivActionBookmark != null) {
                ivActionBookmark.setColorFilter(0xFF30D5C8);
            }
            Toast.makeText(this, "Video guardado en tu perfil", Toast.LENGTH_SHORT).show();
        } else {
            if (ivActionBookmark != null) {
                ivActionBookmark.setColorFilter(0xFFFFFFFF);
            }
            Toast.makeText(this, "Video removido de guardados", Toast.LENGTH_SHORT).show();
        }
    }

    private void reproducirSonidoEstrella() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.estrella_sonido);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void actualizarTiempoTexto(int currentMs, int durationMs) {
        if (tvVideoTime == null) return;

        int currentSec = Math.max(0, currentMs / 1000);
        int durationSec = Math.max(15, durationMs / 1000);

        String currentFormatted = String.format(Locale.US, "%02d:%02d", currentSec / 60, currentSec % 60);
        String durationFormatted = String.format(Locale.US, "%02d:%02d", durationSec / 60, durationSec % 60);

        tvVideoTime.setText(String.format("%s / %s", currentFormatted, durationFormatted));
    }

    private void setupBottomNav() {
        if (llNavInicio != null) {
            llNavInicio.setOnClickListener(v -> {
                Intent intent = new Intent(this, panelPrincipal.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            });
        }

        if (llNavBuscar != null) {
            llNavBuscar.setOnClickListener(v -> {
                Intent intent = new Intent(this, Buscador.class);
                startActivity(intent);
                finish();
            });
        }

        View flCenterNav = findViewById(R.id.fl_nav_center);
        if (flCenterNav != null) {
            flCenterNav.setOnClickListener(v -> {
                Intent intent = new Intent(this, PublicacionContenido.class);
                startActivity(intent);
            });
        }

        if (llNavAlertas != null) {
            llNavAlertas.setOnClickListener(v -> {
                Intent intent = new Intent(this, alertas.class);
                startActivity(intent);
                finish();
            });
        }

        if (llNavPerfil != null) {
            llNavPerfil.setOnClickListener(v -> {
                Intent intent = new Intent(this, PefilUser.class);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (progressHandler != null) {
            progressHandler.removeCallbacks(updateProgressRunnable);
        }
        if (isPlaying && vvVideoPlayer != null) {
            vvVideoPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressHandler != null) {
            progressHandler.removeCallbacks(updateProgressRunnable);
        }
        if (vvVideoPlayer != null) {
            vvVideoPlayer.stopPlayback();
        }
    }
}
