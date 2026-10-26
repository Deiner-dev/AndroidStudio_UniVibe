package com.gamevault.univibe;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UniversidadApi {

    private static final String URL_PRIMARY =
            "https://universities.hipolabs.com/search?country=";

    private static final String URL_FALLBACK_GITHUB =
            "https://raw.githubusercontent.com/Hipo/university-domains-list/master/world_universities_and_domains.json";

    private static final ExecutorService EJECUTOR =
            Executors.newSingleThreadExecutor();

    private static final Handler HANDLER =
            new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(List<Universidad> universidades);
        void onError(String mensaje);
    }

    public static void consultarColombia(Callback callback) {
        buscarPorPais("Colombia", callback);
        //buscarPorPais("España", callback);
    }

    public static void buscarPorPais(String paisBusqueda, Callback callback) {
        EJECUTOR.execute(() -> {
            //String paisParam = (paisBusqueda != null && !paisBusqueda.isEmpty()) ? paisBusqueda : "España";
            String paisParam = (paisBusqueda != null && !paisBusqueda.isEmpty()) ? paisBusqueda : "Colombia";
            List<Universidad> universidades = new ArrayList<>();

            try {
                // Intento 1: Servidor Principal Hipolabs
                try {
                    String paisCodificado = URLEncoder.encode(paisParam, StandardCharsets.UTF_8.name());
                    String jsonString = obtenerJsonHttp(URL_PRIMARY + paisCodificado, 6000);
                    universidades = procesarJsonUniversidades(jsonString, paisParam);
                } catch (Exception ePrimary) {
                    // Intento 2: Servidor Secundario GitHub CDN (Repository oficial de Hipo)
                    String jsonString = obtenerJsonHttp(URL_FALLBACK_GITHUB, 10000);
                    universidades = procesarJsonUniversidades(jsonString, paisParam);
                }

                if (universidades.isEmpty()) {
                    throw new Exception("No se encontraron universidades disponibles para " + paisParam + ".");
                }

                List<Universidad> resultado = Collections.unmodifiableList(universidades);
                HANDLER.post(() -> callback.onSuccess(resultado));

            } catch (Exception e) {
                String errorDetalle = (e.getLocalizedMessage() != null && !e.getLocalizedMessage().isEmpty())
                        ? e.getLocalizedMessage()
                        : "Error de conexión al obtener la lista de universidades.";

                HANDLER.post(() -> callback.onError(errorDetalle));
            }
        });
    }

    private static String obtenerJsonHttp(String urlString, int timeoutMillis) throws Exception {
        HttpURLConnection conexion = null;
        try {
            URL url = new URL(urlString);
            conexion = (HttpURLConnection) url.openConnection();
            conexion.setRequestMethod("GET");
            conexion.setConnectTimeout(timeoutMillis);
            conexion.setReadTimeout(timeoutMillis);
            conexion.setInstanceFollowRedirects(true);
            conexion.setRequestProperty("Accept", "application/json");
            conexion.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            int codigo = conexion.getResponseCode();

            if (codigo == HttpURLConnection.HTTP_MOVED_PERM || codigo == HttpURLConnection.HTTP_MOVED_TEMP || codigo == 307 || codigo == 308) {
                String nuevaUrl = conexion.getHeaderField("Location");
                if (nuevaUrl != null && !nuevaUrl.isEmpty()) {
                    conexion.disconnect();
                    url = new URL(nuevaUrl);
                    conexion = (HttpURLConnection) url.openConnection();
                    conexion.setRequestMethod("GET");
                    conexion.setConnectTimeout(timeoutMillis);
                    conexion.setReadTimeout(timeoutMillis);
                    conexion.setRequestProperty("Accept", "application/json");
                    conexion.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                    codigo = conexion.getResponseCode();
                }
            }

            if (codigo < 200 || codigo >= 300) {
                throw new Exception("HTTP Error " + codigo);
            }

            StringBuilder respuesta = new StringBuilder();
            try (BufferedReader lector = new BufferedReader(
                    new InputStreamReader(conexion.getInputStream(), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    respuesta.append(linea);
                }
            }
            return respuesta.toString();

        } finally {
            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    private static List<Universidad> procesarJsonUniversidades(String jsonString, String paisFiltro) throws Exception {
        JSONArray arreglo = new JSONArray(jsonString);
        List<Universidad> universidades = new ArrayList<>();
        Set<String> nombresRegistrados = new HashSet<>();

        for (int i = 0; i < arreglo.length(); i++) {
            JSONObject objeto = arreglo.getJSONObject(i);

            String nombre = objeto.optString("name", "").trim();
            String paisUniversidad = objeto.optString("country", "").trim();

            if (nombre.isEmpty()) {
                continue;
            }

            // Filtrar por país si se especifica un filtro
            if (paisFiltro != null && !paisFiltro.isEmpty()) {
                if (!paisUniversidad.equalsIgnoreCase(paisFiltro)) {
                    continue;
                }
            }

            // Evitar duplicados
            String clave = nombre.toLowerCase(Locale.ROOT);
            if (!nombresRegistrados.add(clave)) {
                continue;
            }

            String dominio = "";
            String paginaWeb = "";

            JSONArray dominios = objeto.optJSONArray("domains");
            if (dominios != null && dominios.length() > 0) {
                dominio = dominios.optString(0, "");
            }

            JSONArray paginas = objeto.optJSONArray("web_pages");
            if (paginas != null && paginas.length() > 0) {
                paginaWeb = paginas.optString(0, "");
            }

            universidades.add(new Universidad(
                    nombre,
                    paisUniversidad,
                    dominio,
                    paginaWeb
            ));
        }

        // Orden alfabético respetando acentos
        Collator collator = Collator.getInstance(new Locale("es", "CO"));
        Collections.sort(universidades, (a, b) -> collator.compare(a.getNombre(), b.getNombre()));

        return universidades;
    }
}
