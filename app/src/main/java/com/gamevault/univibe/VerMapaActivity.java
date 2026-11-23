package com.gamevault.univibe;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class VerMapaActivity extends AppCompatActivity {

    private WebView wvMap;
    private ProgressBar pbLoading;
    private TextView tvLocationName;
    private MaterialButton btnGetDirections, btnOpenExternalMap;
    private ImageButton btnBack;

    private locationHelper locationHelper;
    private String locationText = "";
    private double destLatitude = 8.3090; // Default Aguachica / Cesar fallback
    private double destLongitude = -73.6190;

    private double currentUserLat = 0.0;
    private double currentUserLon = 0.0;
    private boolean tieneUbicacionUsuario = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_ver_mapa);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        locationHelper = new locationHelper(this);

        locationText = getIntent().getStringExtra("locationText");
        if (TextUtils.isEmpty(locationText)) {
            locationText = "Ubicación compartida";
        }

        initViews();
        obtenerCoordenadasDestino();
        configurarWebView();
        cargarMapaOpenStreetMap();
    }

    private void initViews() {
        wvMap = findViewById(R.id.wv_open_street_map);
        pbLoading = findViewById(R.id.pb_map_loading);
        tvLocationName = findViewById(R.id.tv_location_name_detail);
        btnGetDirections = findViewById(R.id.btn_get_directions);
        btnOpenExternalMap = findViewById(R.id.btn_open_external_map);
        btnBack = findViewById(R.id.btn_back_map);

        if (tvLocationName != null) {
            tvLocationName.setText(locationText);
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnGetDirections != null) {
            btnGetDirections.setOnClickListener(v -> calcularYMostrarRutaInterna());
        }

        if (btnOpenExternalMap != null) {
            btnOpenExternalMap.setOnClickListener(v -> abrirNavegacionExterna());
        }
    }

    private void obtenerCoordenadasDestino() {
        if (TextUtils.isEmpty(locationText)) return;

        // Si el texto ya contiene coordenadas numéricas "lat, lon"
        if (locationText.contains(",")) {
            String[] partes = locationText.split(",");
            if (partes.length >= 2) {
                try {
                    double latParsed = Double.parseDouble(partes[0].trim());
                    double lonParsed = Double.parseDouble(partes[1].trim());
                    destLatitude = latParsed;
                    destLongitude = lonParsed;
                    return;
                } catch (Exception ignored) {}
            }
        }

        // De lo contrario, usar Geocoder para geocodificar el nombre de la ubicación
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(locationText, 1);
            if (addresses != null && !addresses.isEmpty()) {
                destLatitude = addresses.get(0).getLatitude();
                destLongitude = addresses.get(0).getLongitude();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configurarWebView() {
        if (wvMap == null) return;

        WebSettings webSettings = wvMap.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        // User-Agent estable que identifica la app según las políticas de OSM
        String customUserAgent = "UniVibeApp/1.0 (Android; com.gamevault.univibe) " + WebSettings.getDefaultUserAgent(this);
        webSettings.setUserAgentString(customUserAgent);

        wvMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (pbLoading != null) {
                    pbLoading.setVisibility(View.GONE);
                }
            }
        });

        wvMap.setWebChromeClient(new WebChromeClient());
    }

    private void cargarMapaOpenStreetMap() {
        if (wvMap == null) return;

        String htmlContent = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
                "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet-routing-machine@latest/dist/leaflet-routing-machine.css\" />\n" +
                "    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
                "    <script src=\"https://unpkg.com/leaflet-routing-machine@latest/dist/leaflet-routing-machine.js\"></script>\n" +
                "    <style>\n" +
                "        body, html, #map { height: 100%; width: 100%; margin: 0; padding: 0; background-color: #f4f9f7; }\n" +
                "        .leaflet-popup-content-wrapper { border-radius: 12px; font-family: sans-serif; text-align: center; }\n" +
                "        .leaflet-routing-container { display: none !important; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"map\"></div>\n" +
                "    <script>\n" +
                "        var map = L.map('map').setView([" + destLatitude + ", " + destLongitude + "], 16);\n" +
                "        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                "            maxZoom: 19,\n" +
                "            attribution: '© <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors'\n" +
                "        }).addTo(map);\n" +
                "\n" +
                "        var destMarker = L.marker([" + destLatitude + ", " + destLongitude + "]).addTo(map);\n" +
                "        destMarker.bindPopup(\"<b>" + escapeHtml(locationText) + "</b>\").openPopup();\n" +
                "\n" +
                "        function trazarRutaInterna(userLat, userLon) {\n" +
                "            if (window.routingControl) {\n" +
                "                map.removeControl(window.routingControl);\n" +
                "            }\n" +
                "            window.routingControl = L.Routing.control({\n" +
                "                waypoints: [\n" +
                "                    L.latLng(userLat, userLon),\n" +
                "                    L.latLng(" + destLatitude + ", " + destLongitude + ")\n" +
                "                ],\n" +
                "                routeWhileDragging: false,\n" +
                "                addWaypoints: false,\n" +
                "                draggableWaypoints: false,\n" +
                "                fitSelectedRoutes: true,\n" +
                "                show: false,\n" +
                "                lineOptions: {\n" +
                "                    styles: [{color: '#00A884', opacity: 0.85, weight: 6}]\n" +
                "                }\n" +
                "            }).addTo(map);\n" +
                "\n" +
                "            L.marker([userLat, userLon]).addTo(map).bindPopup(\"<b>Tu ubicación actual</b>\");\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";

        wvMap.loadDataWithBaseURL("https://www.openstreetmap.org", htmlContent, "text/html", "UTF-8", null);
    }

    private void calcularYMostrarRutaInterna() {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);

        locationHelper.obtenerUbicacion(new locationHelper.LocationCallback() {
            @Override
            public void onLocationResult(double latitud, double longitud) {
                if (isFinishing() || isDestroyed()) return;

                currentUserLat = latitud;
                currentUserLon = longitud;
                tieneUbicacionUsuario = true;

                if (pbLoading != null) pbLoading.setVisibility(View.GONE);

                // Trazar ruta dentro de la app en la WebView
                if (wvMap != null) {
                    String script = String.format(Locale.US, "trazarRutaInterna(%f, %f);", latitud, longitud);
                    wvMap.evaluateJavascript(script, null);
                }

                Toast.makeText(VerMapaActivity.this, "Ruta generada en el mapa", Toast.LENGTH_SHORT).show();

                // Mostrar botón para redirección externa opcional
                if (btnOpenExternalMap != null) {
                    btnOpenExternalMap.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onLocationError(String mensaje) {
                if (isFinishing() || isDestroyed()) return;

                if (pbLoading != null) pbLoading.setVisibility(View.GONE);

                Toast.makeText(
                        VerMapaActivity.this,
                        "No se pudo obtener tu ubicación actual. Permite los permisos de ubicación para calcular la ruta.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void abrirNavegacionExterna() {
        try {
            String url;
            if (tieneUbicacionUsuario) {
                url = String.format(Locale.US, "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%s",
                        currentUserLat, currentUserLon, Uri.encode(locationText));
            } else {
                url = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(locationText);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir la navegación externa", Toast.LENGTH_SHORT).show();
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("'", "\\'").replace("\"", "&quot;");
    }
}
