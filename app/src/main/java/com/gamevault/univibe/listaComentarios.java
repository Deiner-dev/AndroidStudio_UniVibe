package com.gamevault.univibe;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class listaComentarios extends AppCompatActivity {

    private ImageButton ibBack, ibRemoveCommentImage;
    private ShapeableImageView ivPostUserAvatar, ivPostImage, ivMyAvatarComment, ivCommentImagePreview;
    private ImageView ivLikeStarDetail, ivCommentGalleryAttach;
    private FrameLayout flCommentImagePreview;
    private TextView tvPostUserName, tvPostAcademicInfo, tvPostTime, tvPostText, tvPostLocationText;
    private TextView tvLikeCountDetail, tvCommentCountStatDetail, tvCommentsHeaderCount;
    private LinearLayout llPostLocationDetail, llStatLike;
    private EditText etCommentInput;
    private FloatingActionButton btnSendComment;
    private RecyclerView rvComments;

    private FirebaseAuth mAuth;
    private DatabaseReference postRef, commentsRef, currentUserRef, likeRef;
    private StorageReference storageCommentsRef;
    private ValueEventListener postListener, commentsListener, userListener, likeListener;

    private String postId = "";
    private String postAuthorUid = "";
    private String currentUserId = "";
    private String currentUserName = "";
    private String currentUserPhotoUrl = "";
    private int currentLikesCount = 0;

    private Comentario replyingParentComment = null;
    private Uri selectedCommentImageUri = null;

    private List<Comentario> comentariosList;
    private ComentariosAdapter comentariosAdapter;

    private final ActivityResultLauncher<String> pickCommentImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedCommentImageUri = uri;
                    mostrarVistaPreviaImagenComentario();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_lista_comentarios);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomPadding = Math.max(systemBars.bottom, ime.bottom);
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        postId = getIntent().getStringExtra("postId");
        if (TextUtils.isEmpty(postId)) {
            Toast.makeText(this, "Publicación no encontrada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initFirebase();
        setupListeners();
        cargarDatosUsuarioActual();
        cargarDetallePublicacion();
        cargarComentarios();
        verificarEstadoMeGusta();
    }

    private void initViews() {
        ibBack = findViewById(R.id.ib_back_comments);
        ivPostUserAvatar = findViewById(R.id.iv_post_user_avatar_detail);
        ivPostImage = findViewById(R.id.iv_post_image_detail);
        ivMyAvatarComment = findViewById(R.id.iv_my_avatar_comment);
        ivLikeStarDetail = findViewById(R.id.iv_like_star_detail);

        flCommentImagePreview = findViewById(R.id.fl_comment_image_preview);
        ivCommentImagePreview = findViewById(R.id.iv_comment_image_preview);
        ibRemoveCommentImage = findViewById(R.id.ib_remove_comment_image);
        ivCommentGalleryAttach = findViewById(R.id.iv_comment_gallery_attach);

        tvPostUserName = findViewById(R.id.tv_post_user_name_detail);
        tvPostAcademicInfo = findViewById(R.id.tv_post_academic_detail);
        tvPostTime = findViewById(R.id.tv_post_time_detail);
        tvPostText = findViewById(R.id.tv_post_text_detail);
        tvPostLocationText = findViewById(R.id.tv_post_location_text_detail);
        llPostLocationDetail = findViewById(R.id.ll_post_location_detail);
        llStatLike = findViewById(R.id.ll_stat_like);

        tvLikeCountDetail = findViewById(R.id.tv_like_count_detail);
        tvCommentCountStatDetail = findViewById(R.id.tv_comment_count_stat_detail);
        tvCommentsHeaderCount = findViewById(R.id.tv_comments_header_count);

        etCommentInput = findViewById(R.id.et_comment_input);
        btnSendComment = findViewById(R.id.btn_send_comment);
        rvComments = findViewById(R.id.rv_comments);

        comentariosList = new ArrayList<>();
        comentariosAdapter = new ComentariosAdapter(this, comentariosList, parentComment -> {
            replyingParentComment = parentComment;
            if (etCommentInput != null) {
                etCommentInput.setHint("Respondiendo a " + parentComment.getUserName() + "...");
                etCommentInput.requestFocus();
            }
        });

        if (rvComments != null) {
            rvComments.setLayoutManager(new LinearLayoutManager(this));
            rvComments.setAdapter(comentariosAdapter);
            rvComments.setNestedScrollingEnabled(false);
        }
    }

    private void initFirebase() {
        postRef = FirebaseDatabase.getInstance().getReference("publicaciones").child(postId);
        commentsRef = FirebaseDatabase.getInstance().getReference("comentarios").child(postId);
        storageCommentsRef = FirebaseStorage.getInstance().getReference("comentarios").child(postId);
        if (!TextUtils.isEmpty(currentUserId)) {
            currentUserRef = FirebaseDatabase.getInstance().getReference("registroUsuario").child(currentUserId);
            likeRef = FirebaseDatabase.getInstance().getReference("postLikes").child(postId).child(currentUserId);
        }
    }

    private void setupListeners() {
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        if (btnSendComment != null) {
            btnSendComment.setOnClickListener(v -> enviarComentario());
        }

        if (llStatLike != null) {
            llStatLike.setOnClickListener(v -> toggleMeGusta());
        }

        if (ivCommentGalleryAttach != null) {
            ivCommentGalleryAttach.setOnClickListener(v -> pickCommentImageLauncher.launch("image/*"));
        }

        if (ibRemoveCommentImage != null) {
            ibRemoveCommentImage.setOnClickListener(v -> quitarImagenComentario());
        }
    }

    private void mostrarVistaPreviaImagenComentario() {
        if (selectedCommentImageUri != null && flCommentImagePreview != null && ivCommentImagePreview != null) {
            flCommentImagePreview.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(selectedCommentImageUri)
                    .centerCrop()
                    .into(ivCommentImagePreview);
        }
    }

    private void quitarImagenComentario() {
        selectedCommentImageUri = null;
        if (flCommentImagePreview != null) {
            flCommentImagePreview.setVisibility(View.GONE);
        }
    }

    private void verificarEstadoMeGusta() {
        if (likeRef == null) return;

        likeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                    if (ivLikeStarDetail != null) {
                        ivLikeStarDetail.setImageResource(android.R.drawable.btn_star_big_on);
                        ivLikeStarDetail.setColorFilter(Color.parseColor("#00A884"));
                    }
                } else {
                    if (ivLikeStarDetail != null) {
                        ivLikeStarDetail.setImageResource(android.R.drawable.btn_star_big_off);
                        ivLikeStarDetail.setColorFilter(Color.parseColor("#7A8C85"));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        likeRef.addValueEventListener(likeListener);
    }

    private void toggleMeGusta() {
        if (likeRef == null || TextUtils.isEmpty(currentUserId)) return;

        likeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean yaTieneLike = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                int nuevoCount = yaTieneLike ? Math.max(0, currentLikesCount - 1) : currentLikesCount + 1;

                if (!yaTieneLike) {
                    reproducirSonidoEstrella();
                    if (ivLikeStarDetail != null) {
                        AnimacionUtils.animarEstrella(ivLikeStarDetail);
                    }
                    if (!postAuthorUid.equals(currentUserId)) {
                        NotificacionesHelper.registrarNotificacion(postAuthorUid, "LIKE_POST", postId, "", "", "le dio me gusta a tu publicación");
                    }
                }

                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                Map<String, Object> updates = new HashMap<>();

                if (yaTieneLike) {
                    updates.put("/postLikes/" + postId + "/" + currentUserId, null);
                } else {
                    updates.put("/postLikes/" + postId + "/" + currentUserId, true);
                }

                updates.put("/publicaciones/" + postId + "/likesCount", nuevoCount);
                if (!TextUtils.isEmpty(postAuthorUid)) {
                    updates.put("/userPublicaciones/" + postAuthorUid + "/" + postId + "/likesCount", nuevoCount);
                }

                rootRef.updateChildren(updates);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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

    private void cargarDatosUsuarioActual() {
        if (currentUserRef == null) return;

        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                if (snapshot.exists()) {
                    currentUserName = snapshot.child("fullName").getValue(String.class);
                    currentUserPhotoUrl = snapshot.child("imgPerfil").getValue(String.class);

                    if (currentUserPhotoUrl != null && !currentUserPhotoUrl.isEmpty() && ivMyAvatarComment != null) {
                        Glide.with(listaComentarios.this)
                                .load(currentUserPhotoUrl)
                                .circleCrop()
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .into(ivMyAvatarComment);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        currentUserRef.addValueEventListener(userListener);
    }

    private void cargarDetallePublicacion() {
        postListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                if (snapshot.exists()) {
                    Publicacion post = snapshot.getValue(Publicacion.class);
                    if (post != null) {
                        postAuthorUid = post.getUid();
                        currentLikesCount = post.getLikesCount();

                        if (tvLikeCountDetail != null) {
                            tvLikeCountDetail.setText(String.valueOf(currentLikesCount));
                        }

                        tvPostUserName.setText(post.getUserName());

                        String carrera = post.getCareer();
                        String universidad = post.getUniversity();
                        if (!TextUtils.isEmpty(carrera) && !TextUtils.isEmpty(universidad)) {
                            tvPostAcademicInfo.setText(getString(R.string.user_academic_university_format, carrera, universidad));
                            tvPostAcademicInfo.setVisibility(View.VISIBLE);
                        } else if (!TextUtils.isEmpty(carrera)) {
                            tvPostAcademicInfo.setText(carrera);
                            tvPostAcademicInfo.setVisibility(View.VISIBLE);
                        } else {
                            tvPostAcademicInfo.setVisibility(View.GONE);
                        }

                        // Time ago
                        if (post.getTimestamp() > 0) {
                            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                                    post.getTimestamp(),
                                    System.currentTimeMillis(),
                                    DateUtils.MINUTE_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_RELATIVE
                            );
                            tvPostTime.setText(getString(R.string.time_audience_format, timeAgo, getString(R.string.public_audience)));
                        }

                        // Text content
                        if (!TextUtils.isEmpty(post.getTextContent())) {
                            tvPostText.setText(post.getTextContent());
                            tvPostText.setVisibility(View.VISIBLE);
                        } else {
                            tvPostText.setVisibility(View.GONE);
                        }

                        // Image content
                        if (!TextUtils.isEmpty(post.getImageUrl())) {
                            ivPostImage.setVisibility(View.VISIBLE);
                            Glide.with(listaComentarios.this)
                                    .load(post.getImageUrl())
                                    .centerCrop()
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .into(ivPostImage);

                            ivPostImage.setOnClickListener(v -> {
                                Intent intent = new Intent(listaComentarios.this, VerImagenCompletaActivity.class);
                                intent.putExtra("imageUrl", post.getImageUrl());
                                startActivity(intent);
                            });
                        } else {
                            ivPostImage.setVisibility(View.GONE);
                            ivPostImage.setOnClickListener(null);
                        }

                        // Location
                        if (!TextUtils.isEmpty(post.getLocation())) {
                            llPostLocationDetail.setVisibility(View.VISIBLE);
                            tvPostLocationText.setText(post.getLocation());
                        } else {
                            llPostLocationDetail.setVisibility(View.GONE);
                        }

                        // Author Avatar
                        if (!TextUtils.isEmpty(post.getUserPhotoUrl())) {
                            Glide.with(listaComentarios.this)
                                    .load(post.getUserPhotoUrl())
                                    .circleCrop()
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .into(ivPostUserAvatar);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        postRef.addValueEventListener(postListener);
    }

    private void cargarComentarios() {
        commentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                comentariosList.clear();
                int totalComentariosYRespuestas = 0;

                for (DataSnapshot commentSnap : snapshot.getChildren()) {
                    try {
                        Comentario comentario = commentSnap.getValue(Comentario.class);
                        if (comentario != null) {
                            comentariosList.add(comentario);
                            totalComentariosYRespuestas++;

                            if (commentSnap.hasChild("respuestas")) {
                                totalComentariosYRespuestas += (int) commentSnap.child("respuestas").getChildrenCount();
                            }
                        }
                    } catch (Exception e) {}
                }

                // Order comments by timestamp ascending
                Collections.sort(comentariosList, (c1, c2) -> Long.compare(c1.getTimestamp(), c2.getTimestamp()));

                if (tvCommentCountStatDetail != null) {
                    tvCommentCountStatDetail.setText(String.valueOf(totalComentariosYRespuestas));
                }
                if (tvCommentsHeaderCount != null) {
                    tvCommentsHeaderCount.setText(getString(R.string.title_comments_count, totalComentariosYRespuestas));
                }

                comentariosAdapter.notifyDataSetChanged();

                // Persist total comments + replies count in Database
                actualizarContadorComentariosEnDatabase(totalComentariosYRespuestas);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        commentsRef.addValueEventListener(commentsListener);
    }

    private void actualizarContadorComentariosEnDatabase(int count) {
        if (TextUtils.isEmpty(postId)) return;

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> updates = new HashMap<>();
        updates.put("/publicaciones/" + postId + "/commentsCount", count);
        if (!TextUtils.isEmpty(postAuthorUid)) {
            updates.put("/userPublicaciones/" + postAuthorUid + "/" + postId + "/commentsCount", count);
        }
        rootRef.updateChildren(updates);
    }

    private void enviarComentario() {
        String textoInput = etCommentInput.getText().toString().trim();
        boolean tieneTexto = !TextUtils.isEmpty(textoInput);
        boolean tieneImagen = (selectedCommentImageUri != null);

        if (!tieneTexto && !tieneImagen) {
            Toast.makeText(this, R.string.msg_comment_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();

        if (replyingParentComment != null) {
            // Publishing a REPLY under the parent comment
            String parentCommentId = replyingParentComment.getCommentId();
            DatabaseReference repliesRef = commentsRef.child(parentCommentId).child("respuestas");
            String replyId = repliesRef.push().getKey();

            if (replyId == null) {
                Toast.makeText(this, R.string.msg_comment_error, Toast.LENGTH_SHORT).show();
                return;
            }

            if (tieneImagen) {
                subirImagenYGuardarRespuesta(repliesRef, replyId, parentCommentId, textoInput, timestamp);
            } else {
                guardarRespuestaEnDatabase(repliesRef, replyId, parentCommentId, textoInput, "", timestamp);
            }

        } else {
            // Publishing a TOP-LEVEL COMMENT
            String commentId = commentsRef.push().getKey();
            if (commentId == null) {
                Toast.makeText(this, R.string.msg_comment_error, Toast.LENGTH_SHORT).show();
                return;
            }

            if (tieneImagen) {
                subirImagenYGuardarComentario(commentId, textoInput, timestamp);
            } else {
                guardarComentarioEnDatabase(commentId, textoInput, "", timestamp);
            }
        }
    }

    private void subirImagenYGuardarComentario(String commentId, String texto, long timestamp) {
        Toast.makeText(this, R.string.msg_publishing, Toast.LENGTH_SHORT).show();
        StorageReference imageFileRef = storageCommentsRef.child(commentId + ".jpg");

        imageFileRef.putFile(selectedCommentImageUri).addOnSuccessListener(taskSnapshot ->
                imageFileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    String imageUrl = downloadUri.toString();
                    guardarComentarioEnDatabase(commentId, texto, imageUrl, timestamp);
                }).addOnFailureListener(e -> Toast.makeText(listaComentarios.this, R.string.msg_comment_error, Toast.LENGTH_SHORT).show())
        ).addOnFailureListener(e -> Toast.makeText(listaComentarios.this, R.string.msg_comment_error, Toast.LENGTH_SHORT).show());
    }

    private void guardarComentarioEnDatabase(String commentId, String texto, String imageUrl, long timestamp) {
        Comentario nuevoComentario = new Comentario(
                commentId,
                postId,
                currentUserId,
                currentUserName,
                currentUserPhotoUrl,
                texto,
                imageUrl,
                timestamp,
                0
        );

        commentsRef.child(commentId).setValue(nuevoComentario).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                etCommentInput.setText("");
                quitarImagenComentario();
                Toast.makeText(listaComentarios.this, R.string.msg_comment_success, Toast.LENGTH_SHORT).show();

                if (!postAuthorUid.equals(currentUserId)) {
                    String msg = "comentó en tu publicación" + (!TextUtils.isEmpty(texto) ? ": \"" + texto + "\"" : "");
                    NotificacionesHelper.registrarNotificacion(postAuthorUid, "COMMENT_POST", postId, commentId, "", msg);
                }
            } else {
                Toast.makeText(listaComentarios.this, R.string.msg_comment_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void subirImagenYGuardarRespuesta(DatabaseReference repliesRef, String replyId, String parentCommentId, String texto, long timestamp) {
        Toast.makeText(this, R.string.msg_publishing, Toast.LENGTH_SHORT).show();
        StorageReference imageFileRef = storageCommentsRef.child(replyId + ".jpg");

        imageFileRef.putFile(selectedCommentImageUri).addOnSuccessListener(taskSnapshot ->
                imageFileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    String imageUrl = downloadUri.toString();
                    guardarRespuestaEnDatabase(repliesRef, replyId, parentCommentId, texto, imageUrl, timestamp);
                }).addOnFailureListener(e -> Toast.makeText(listaComentarios.this, R.string.msg_comment_error, Toast.LENGTH_SHORT).show())
        ).addOnFailureListener(e -> Toast.makeText(listaComentarios.this, R.string.msg_comment_error, Toast.LENGTH_SHORT).show());
    }

    private void guardarRespuestaEnDatabase(DatabaseReference repliesRef, String replyId, String parentCommentId, String texto, String imageUrl, long timestamp) {
        Respuesta nuevaRespuesta = new Respuesta(
                replyId,
                parentCommentId,
                currentUserId,
                currentUserName,
                currentUserPhotoUrl,
                texto,
                imageUrl,
                timestamp,
                0
        );

        repliesRef.child(replyId).setValue(nuevaRespuesta).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                etCommentInput.setText("");
                quitarImagenComentario();
                String targetReplyUid = replyingParentComment != null ? replyingParentComment.getUid() : "";
                replyingParentComment = null;
                if (etCommentInput != null) {
                    etCommentInput.setHint(R.string.hint_write_comment);
                }
                Toast.makeText(listaComentarios.this, "Respuesta publicada", Toast.LENGTH_SHORT).show();

                if (!TextUtils.isEmpty(targetReplyUid) && !targetReplyUid.equals(currentUserId)) {
                    String msg = "respondió a tu comentario" + (!TextUtils.isEmpty(texto) ? ": \"" + texto + "\"" : "");
                    NotificacionesHelper.registrarNotificacion(targetReplyUid, "REPLY_COMMENT", postId, parentCommentId, replyId, msg);
                }
            } else {
                Toast.makeText(listaComentarios.this, R.string.msg_comment_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (postRef != null && postListener != null) {
            postRef.removeEventListener(postListener);
        }
        if (commentsRef != null && commentsListener != null) {
            commentsRef.removeEventListener(commentsListener);
        }
        if (currentUserRef != null && userListener != null) {
            currentUserRef.removeEventListener(userListener);
        }
        if (likeRef != null && likeListener != null) {
            likeRef.removeEventListener(likeListener);
        }
    }
}
