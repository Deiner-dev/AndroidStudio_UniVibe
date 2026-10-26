package com.gamevault.univibe;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

public class VerImagenCompletaActivity extends AppCompatActivity {

    private ImageView ivFullImage;
    private ProgressBar pbLoading;
    private ImageButton ibClose, ibOptions;
    private PopupWindow optionsPopupWindow;
    private String imageUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ver_imagen_completa);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageUrl = getIntent().getStringExtra("imageUrl");
        if (TextUtils.isEmpty(imageUrl)) {
            Toast.makeText(this, "Imagen no disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        cargarImagen();
    }

    private void initViews() {
        ivFullImage = findViewById(R.id.iv_full_image);
        pbLoading = findViewById(R.id.pb_full_image_loading);
        ibClose = findViewById(R.id.ib_close_full_image);
        ibOptions = findViewById(R.id.ib_options_full_image);
    }

    private void setupListeners() {
        if (ibClose != null) {
            ibClose.setOnClickListener(v -> finish());
        }

        if (ibOptions != null) {
            ibOptions.setOnClickListener(this::mostrarMenuOpciones);
        }
    }

    private void cargarImagen() {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(imageUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                        Toast.makeText(VerImagenCompletaActivity.this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(ivFullImage);
    }

    private void mostrarMenuOpciones(View anchorView) {
        if (optionsPopupWindow != null && optionsPopupWindow.isShowing()) {
            optionsPopupWindow.dismiss();
            return;
        }

        View popupView = getLayoutInflater().inflate(R.layout.popup_image_options_menu, null);

        optionsPopupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        optionsPopupWindow.setElevation(20f);
        optionsPopupWindow.setOutsideTouchable(true);

        View itemDownload = popupView.findViewById(R.id.ll_option_download);
        if (itemDownload != null) {
            itemDownload.setOnClickListener(v -> {
                optionsPopupWindow.dismiss();
                descargarImagen(imageUrl);
            });
        }

        optionsPopupWindow.showAsDropDown(anchorView, -320, 10);
    }

    private void descargarImagen(String url) {
        if (TextUtils.isEmpty(url)) return;

        try {
            Toast.makeText(this, R.string.msg_downloading_image, Toast.LENGTH_SHORT).show();

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("UniVibe Imagen");
            request.setDescription("Guardando imagen en la galería...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            String fileName = "UniVibe_" + System.currentTimeMillis() + ".jpg";
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "UniVibe/" + fileName);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(this, R.string.msg_download_success, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.msg_download_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (optionsPopupWindow != null && optionsPopupWindow.isShowing()) {
            optionsPopupWindow.dismiss();
        }
    }
}
